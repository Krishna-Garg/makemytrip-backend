package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RecurringFlightService {

    @Autowired private FlightRepository flightRepository;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY,
        "THU", DayOfWeek.THURSDAY, "FRI", DayOfWeek.FRIDAY,
        "SAT", DayOfWeek.SATURDAY, "SUN", DayOfWeek.SUNDAY
    );

    @Scheduled(cron = "0 0 0 * * *")
    public void generateFromTemplates() {
        doGenerate(flightRepository.findAll());
    }

    public List<Flight> generateFromTemplate(Flight template) {
        List<Flight> allFlights = flightRepository.findAll();
        return doGenerateForTemplate(template, LocalDate.now(), allFlights);
    }

    private void doGenerate(List<Flight> allFlights) {
        List<Flight> templates = allFlights.stream().filter(Flight::isTemplate).toList();
        for (Flight template : templates) {
            doGenerateForTemplate(template, LocalDate.now(), allFlights);
        }
    }

    private List<Flight> doGenerateForTemplate(Flight template, LocalDate startDate,
                                                List<Flight> allFlights) {
        List<Flight> generated = new ArrayList<>();
        if (template.getRecurringDays() == null || template.getRecurringDays().isEmpty()) return generated;
        if (template.getBaseTime() == null || template.getBaseTime().isEmpty()) return generated;

        int depHour, depMin;
        try {
            String[] tp = template.getBaseTime().split(":");
            depHour = Integer.parseInt(tp[0]);
            depMin  = Integer.parseInt(tp[1]);
        } catch (Exception e) { return generated; }

        long durationMinutes = 120;
        try {
            LocalDateTime d1 = LocalDateTime.parse(template.getDepartureTime());
            LocalDateTime d2 = LocalDateTime.parse(template.getArrivalTime());
            long d = ChronoUnit.MINUTES.between(d1, d2);
            if (d > 0) durationMinutes = d;
        } catch (Exception ignored) {}

        // FIX #14: build existing set ONCE outside nested loops
        Set<String> existingDeps = new HashSet<>();
        for (Flight f : allFlights) {
            if (template.getId().equals(f.getTemplateId())) {
                existingDeps.add(f.getDepartureTime());
            }
        }

        for (String dayCode : template.getRecurringDays()) {
            DayOfWeek dow = DAY_MAP.get(dayCode);
            if (dow == null) continue;
            for (int offset = 0; offset < 28; offset++) {
                LocalDate targetDate = startDate.plusDays(offset);
                if (targetDate.getDayOfWeek() != dow) continue;
                LocalDateTime dep = LocalDateTime.of(targetDate, LocalTime.of(depHour, depMin));
                if (dep.isBefore(LocalDateTime.now())) continue;
                String depStr = dep.toString();
                if (existingDeps.contains(depStr)) continue;

                Flight gen = new Flight();
                gen.setFlightName(template.getFlightName());
                gen.setFrom(template.getFrom());
                gen.setTo(template.getTo());
                gen.setDepartureTime(depStr);
                gen.setArrivalTime(dep.plusMinutes(durationMinutes).toString());
                gen.setPrice(template.getPrice());
                gen.setAvailableSeats(template.getAvailableSeats());
                gen.setBoardingMinutes(template.getBoardingMinutes() > 0 ? template.getBoardingMinutes() : 45);
                gen.setTemplateId(template.getId());
                gen.setStatus("ON_TIME");
                if (template.getAircraftModel() != null && !template.getAircraftModel().isEmpty())
                    gen.setAircraftModel(template.getAircraftModel());

                generated.add(flightRepository.save(gen));
                existingDeps.add(depStr);
            }
        }
        return generated;
    }

    public List<Flight> getGeneratedFlights(String templateId) {
        return flightRepository.findAll().stream()
            .filter(f -> templateId.equals(f.getTemplateId()))
            .sorted(Comparator.comparing(Flight::getDepartureTime))
            .toList();
    }

    public List<Flight> getAllTemplates() {
        return flightRepository.findAll().stream().filter(Flight::isTemplate).toList();
    }
}

package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class RecurringFlightService {

    @Autowired private FlightRepository flightRepository;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY,
        "THU", DayOfWeek.THURSDAY, "FRI", DayOfWeek.FRIDAY,
        "SAT", DayOfWeek.SATURDAY, "SUN", DayOfWeek.SUNDAY
    );

    // ── Runs every day at midnight ────────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")
    public void generateFromTemplates() {
        doGenerate();
    }

    // ── Called immediately when admin saves a new template ───────────────────
    public List<Flight> generateFromTemplate(Flight template) {
        return doGenerateForTemplate(template, LocalDate.now());
    }

    // ── Core generation logic ─────────────────────────────────────────────────
    private void doGenerate() {
        List<Flight> templates = flightRepository.findAll().stream()
            .filter(Flight::isTemplate).toList();
        LocalDate today = LocalDate.now();
        for (Flight template : templates) {
            doGenerateForTemplate(template, today);
        }
    }

    private List<Flight> doGenerateForTemplate(Flight template, LocalDate startDate) {
        List<Flight> generated = new ArrayList<>();

        if (template.getBaseTime() == null || template.getBaseTime().isEmpty()) return generated;

        // Parse baseTime e.g. "06:00"
        String[] timeParts = template.getBaseTime().split(":");
        if (timeParts.length < 2) return generated;
        int depHour = Integer.parseInt(timeParts[0]);
        int depMin  = Integer.parseInt(timeParts[1]);

        // Calculate duration from template departure/arrival (in minutes)
        long durationMinutes = 120; // default 2h fallback
        try {
            LocalDateTime templateDep = LocalDateTime.parse(template.getDepartureTime());
            LocalDateTime templateArr = LocalDateTime.parse(template.getArrivalTime());
            durationMinutes = ChronoUnit.MINUTES.between(templateDep, templateArr);
            if (durationMinutes <= 0) durationMinutes = 120;
        } catch (Exception ignored) {}

        for (String dayCode : template.getRecurringDays()) {
            DayOfWeek dow = DAY_MAP.get(dayCode);
            if (dow == null) continue;

            // Generate for today + next 4 weeks (28 days)
            for (int offset = 0; offset < 28; offset++) {
                LocalDate targetDate = startDate.plusDays(offset);

                // Only generate for matching day-of-week
                if (targetDate.getDayOfWeek() != dow) continue;

                LocalDateTime dep = LocalDateTime.of(targetDate, LocalTime.of(depHour, depMin));

                // Skip if departure already passed
                if (dep.isBefore(LocalDateTime.now())) continue;

                LocalDateTime arr = dep.plusMinutes(durationMinutes);
                String depStr = dep.toString();

                // Skip if already exists
                boolean exists = flightRepository.findAll().stream()
                    .anyMatch(f -> template.getId().equals(f.getTemplateId())
                        && depStr.equals(f.getDepartureTime()));
                if (exists) continue;

                Flight gen = new Flight();
                gen.setFlightName(template.getFlightName());
                gen.setFrom(template.getFrom());
                gen.setTo(template.getTo());
                gen.setDepartureTime(depStr);
                gen.setArrivalTime(arr.toString());
                gen.setPrice(template.getPrice());
                gen.setAvailableSeats(template.getAvailableSeats());
                gen.setBoardingMinutes(template.getBoardingMinutes() > 0 ? template.getBoardingMinutes() : 45);
                gen.setTemplateId(template.getId());
                gen.setStatus("ON_TIME");

                // FIX: copy aircraftModel so seat maps work on generated flights
                if (template.getAircraftModel() != null && !template.getAircraftModel().isEmpty()) {
                    gen.setAircraftModel(template.getAircraftModel());
                }

                generated.add(flightRepository.save(gen));
            }
        }
        return generated;
    }

    // ── Admin: get all flights generated from a specific template ─────────────
    public List<Flight> getGeneratedFlights(String templateId) {
        return flightRepository.findAll().stream()
            .filter(f -> templateId.equals(f.getTemplateId()))
            .sorted(Comparator.comparing(Flight::getDepartureTime))
            .toList();
    }

    // ── Admin: get all templates ──────────────────────────────────────────────
    public List<Flight> getAllTemplates() {
        return flightRepository.findAll().stream()
            .filter(Flight::isTemplate)
            .toList();
    }
}

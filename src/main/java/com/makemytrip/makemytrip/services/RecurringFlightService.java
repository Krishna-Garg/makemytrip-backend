package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class RecurringFlightService {

    @Autowired private FlightRepository flightRepository;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
            "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY, "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY, "SUN", DayOfWeek.SUNDAY
    );

    // Runs every day at midnight — generates next 4 weeks of flights from templates
    @Scheduled(cron = "0 0 0 * * *")
    public void generateFromTemplates() {
        List<Flight> templates = flightRepository.findAll().stream()
                .filter(Flight::isTemplate).toList();

        LocalDate today = LocalDate.now();

        for (Flight template : templates) {
            for (String dayCode : template.getRecurringDays()) {
                DayOfWeek dow = DAY_MAP.get(dayCode);
                if (dow == null) continue;

                // Generate for next 4 weeks
                for (int week = 0; week < 4; week++) {
                    LocalDate targetDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow))
                            .plusWeeks(week);

                    // Parse baseTime e.g. "06:00"
                    String[] timeParts = template.getBaseTime().split(":");
                    LocalDateTime dep = LocalDateTime.of(targetDate,
                            LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1])));

                    // Duration from template
                    LocalDateTime templateDep = LocalDateTime.parse(template.getDepartureTime());
                    LocalDateTime templateArr = LocalDateTime.parse(template.getArrivalTime());
                    long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(templateDep, templateArr);
                    LocalDateTime arr = dep.plusMinutes(durationMinutes);

                    // Skip if already exists for this date+template
                    String depStr = dep.toString();
                    boolean exists = flightRepository.findAll().stream()
                            .anyMatch(f -> template.getId().equals(f.getTemplateId())
                                    && depStr.equals(f.getDepartureTime()));
                    if (exists) continue;

                    Flight generated = new Flight();
                    generated.setFlightName(template.getFlightName());
                    generated.setFrom(template.getFrom());
                    generated.setTo(template.getTo());
                    generated.setDepartureTime(depStr);
                    generated.setArrivalTime(arr.toString());
                    generated.setPrice(template.getPrice());
                    generated.setAvailableSeats(template.getAvailableSeats());
                    generated.setTemplateId(template.getId());
                    generated.setStatus("ON_TIME");
                    flightRepository.save(generated);
                }
            }
        }
    }
}

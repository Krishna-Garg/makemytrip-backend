package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Flight.StatusUpdate;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import jdk.jshell.Snippet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FlightStatusService {
    @Autowired
    private FlightRepository flightRepository;

    public Flight createTemplate(Flight template) {
        template.setTemplate(true);
        template.setStatus("ON_TIME");
        return flightRepository.save(template);
    }

    public Flight updateStatus(String flightId, String status, String reason, String estimatedDeparture, int delayMinutes) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found!"));
        flight.setStatus(status);
        flight.setStatusReason(reason);
        flight.setDelayMinutes(delayMinutes);
        if (estimatedDeparture != null) flight.setEstimatedDeparture(estimatedDeparture);

        StatusUpdate update = new StatusUpdate();
        update.setStatus(status);
        update.setReason(reason);
        update.setUpdatedAt(LocalDateTime.now().toString());
        update.setUpdatedBy("ADMIN");
        flight.getStatusHistory().add(update);

        return flightRepository.save(flight);
    }

    @Scheduled(fixedRate = 120000)
    public void autoSimulation() {
        List<Flight> flights = flightRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Flight flight : flights) {
            if (flight.isTemplate()) continue;
            if ("DEPARTED".equals(flight.getStatus())  || "EXPIRED".equals(flight.getStatus())) continue;

            try {
                LocalDateTime dep = LocalDateTime.parse(flight.getDepartureTime());
                long minutesUntil = ChronoUnit.MINUTES.between(now, dep);

                String newStatus = flight.getStatus();

                if (minutesUntil < 0) {
                    flightRepository.deleteById(flight.getId());
                    continue;
                } else if (minutesUntil <= flight.getBoardingMinutes()) {
                    newStatus = "BOARDING";
                } else if ("ON_TIME".equals(flight.getStatus())) {
                    newStatus = "ON_TIME";
                }

                if (!newStatus.equals(flight.getStatus())) {
                    StatusUpdate update = new StatusUpdate();
                    update.setStatus(newStatus);
                    update.setReason("Auto updated by system");
                    update.setUpdatedBy("SYSTEM");
                    flight.getStatusHistory().add(update);
                    flight.setStatus(newStatus);
                    flightRepository.save(flight);
                }
            } catch (Exception ignored) {}
        }
    }

    public Flight getFlightStatus(String flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found!"));
    }

    public List<Flight> searchByDate(String from, String to, String date) {
        List<Flight> all = flightRepository.findAll();
        return all.stream()
                .filter(f -> !f.isTemplate())
                .filter(f -> !List.of("EXPIRED", "DEPARTED").contains(f.getStatus()))
                .filter(f -> f.getFrom().equalsIgnoreCase(from) && f.getTo().equalsIgnoreCase(to))
                .filter(f -> {
                    try {
                        String depDate = f.getDepartureTime().substring(0, 10);
                        return depDate.equals(date);
                    } catch (Exception e) { return false; }
                })
                .toList();
    }
}

package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Flight.StatusUpdate;
import com.makemytrip.makemytrip.models.FlightNotification;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.FlightNotificationRepository;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FlightStatusService {

    @Autowired private FlightRepository flightRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FlightNotificationRepository notificationRepository;

    // ── Admin manual override ────────────────────────────────────────────────

    public Flight updateStatus(String flightId, String status, String reason,
                               String estimatedDeparture, int delayMinutes) {
        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new RuntimeException("Flight not found"));

        String previousStatus = flight.getStatus();
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

        flightRepository.save(flight);

        // Fire notifications if status actually changed
        if (!status.equals(previousStatus)) {
            notifyUsers(flight, status, reason, delayMinutes);
        }

        return flight;
    }

    // ── Auto-simulation every 2 minutes ──────────────────────────────────────

    @Scheduled(fixedRate = 120000)
    public void autoSimulate() {
        List<Flight> flights = flightRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Flight flight : flights) {
            if (flight.isTemplate()) continue;
            if ("DEPARTED".equals(flight.getStatus()) || "EXPIRED".equals(flight.getStatus())) continue;

            try {
                LocalDateTime dep = LocalDateTime.parse(flight.getDepartureTime());
                long minutesUntil = ChronoUnit.MINUTES.between(now, dep);

                if (minutesUntil < 0) {
                    flightRepository.deleteById(flight.getId());
                    continue;
                }

                String newStatus = flight.getStatus();
                if (minutesUntil <= flight.getBoardingMinutes() && !"BOARDING".equals(flight.getStatus())
                        && !"DELAYED".equals(flight.getStatus())) {
                    newStatus = "BOARDING";
                }

                if (!newStatus.equals(flight.getStatus())) {
                    StatusUpdate update = new StatusUpdate();
                    update.setStatus(newStatus);
                    update.setReason("Auto-updated by system");
                    update.setUpdatedAt(now.toString());
                    update.setUpdatedBy("SYSTEM");
                    flight.getStatusHistory().add(update);
                    flight.setStatus(newStatus);
                    flightRepository.save(flight);
                    notifyUsers(flight, newStatus, null, 0);
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Notify all users who booked this flight ───────────────────────────────

    private void notifyUsers(Flight flight, String status, String reason, int delayMinutes) {
        List<Users> allUsers = userRepository.findAll();
        String message = buildMessage(flight.getFlightName(), status, reason, delayMinutes);
        String type = switch (status) {
            case "DELAYED"  -> "DELAY";
            case "BOARDING" -> "BOARDING";
            case "DEPARTED" -> "DEPARTED";
            default         -> "STATUS_CHANGE";
        };

        for (Users user : allUsers) {
            boolean hasBooking = user.getBookings().stream()
                .anyMatch(b -> (flight.getId() == null ? false : flight.getId().equals(b.getBookingId()))
                    && "Flight".equals(b.getType())
                    && "CONFIRMED".equals(b.getBookingStatus()));
            if (!hasBooking) continue;

            FlightNotification notif = new FlightNotification();
            notif.setUserId(user.getId());
            notif.setFlightId(flight.getId());
            notif.setFlightName(flight.getFlightName());
            notif.setMessage(message);
            notif.setType(type);
            notif.setCreatedAt(LocalDateTime.now().toString());
            notificationRepository.save(notif);
        }
    }

    private String buildMessage(String flightName, String status, String reason, int delayMinutes) {
        return switch (status) {
            case "DELAYED"  -> flightName + " is delayed by " + delayMinutes + " min"
                + (reason != null && !reason.isEmpty() ? " — " + reason : "");
            case "BOARDING" -> flightName + " is now boarding. Please proceed to gate.";
            case "DEPARTED" -> flightName + " has departed.";
            case "ON_TIME"  -> flightName + " is running on time.";
            default         -> flightName + " status updated to " + status;
        };
    }

    public Flight getFlightStatus(String flightId) {
        return flightRepository.findById(flightId)
            .orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    public List<Flight> searchByDate(String from, String to, String date) {
        return flightRepository.findAll().stream()
            .filter(f -> !f.isTemplate())
            .filter(f -> !List.of("EXPIRED", "DEPARTED").contains(f.getStatus()))
            .filter(f -> f.getFrom().equalsIgnoreCase(from) && f.getTo().equalsIgnoreCase(to))
            .filter(f -> {
                try { return f.getDepartureTime().substring(0, 10).equals(date); }
                catch (Exception e) { return false; }
            })
            .toList();
    }
    public Flight createTemplate(Flight template) {
        template.setTemplate(true);
        template.setStatus("ON_TIME");
        if (template.getStatusHistory() == null) {
            template.setStatusHistory(new java.util.ArrayList<>());
    }
        return flightRepository.save(template);
    }
}

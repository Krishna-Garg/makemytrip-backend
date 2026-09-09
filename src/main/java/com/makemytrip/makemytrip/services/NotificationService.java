// ═══════════════════════════════════════════════════════════
// services/NotificationService.java
// ═══════════════════════════════════════════════════════════

package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.FlightNotification;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.FlightNotificationRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired private FlightNotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    // Called by FlightStatusService whenever status changes
    public void notifyUsersOfStatusChange(String flightId, String flightName,
                                           String newStatus, String reason, int delayMinutes) {
        // Find all users who have booked this flight
        List<Users> allUsers = userRepository.findAll();
        for (Users user : allUsers) {
            boolean hasBooking = user.getBookings().stream()
                .anyMatch(b -> flightId.equals(b.getBookingId()) && "Flight".equals(b.getType())
                    && "CONFIRMED".equals(b.getBookingStatus()));
            if (!hasBooking) continue;

            String message = buildMessage(flightName, newStatus, reason, delayMinutes);
            String type = switch (newStatus) {
                case "DELAYED"  -> "DELAY";
                case "BOARDING" -> "BOARDING";
                case "DEPARTED" -> "DEPARTED";
                default         -> "STATUS_CHANGE";
            };

            FlightNotification notif = new FlightNotification();
            notif.setUserId(user.getId());
            notif.setFlightId(flightId);
            notif.setFlightName(flightName);
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

    public List<FlightNotification> getNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAllRead(String userId) {
        List<FlightNotification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void markOneRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}

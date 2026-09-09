// ═══════════════════════════════════════════════════════════
// controllers/NotificationController.java
// ═══════════════════════════════════════════════════════════

package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.FlightNotification;
import com.makemytrip.makemytrip.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    // GET /notifications?userId=xxx
    @GetMapping
    public ResponseEntity<List<FlightNotification>> getNotifications(@RequestParam String userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    // GET /notifications/unread-count?userId=xxx
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam String userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    // PUT /notifications/mark-all-read?userId=xxx
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@RequestParam String userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    // PUT /notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOneRead(@PathVariable String id) {
        notificationService.markOneRead(id);
        return ResponseEntity.ok().build();
    }
}

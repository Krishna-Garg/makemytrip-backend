// ═══════════════════════════════════════════════════════════
// controllers/BookingController.java
// FIX #8: (Integer) → ((Number) x).intValue()
// ═══════════════════════════════════════════════════════════
package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.models.CancellationReason;
import com.makemytrip.makemytrip.services.BookingService;
import com.makemytrip.makemytrip.services.CancellationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired private BookingService bookingService;
    @Autowired private CancellationService cancellationService;

    @PostMapping("/flight")
    public ResponseEntity<Users.Booking> bookFlight(@RequestBody Map<String, Object> body) {
        String userId  = (String) body.get("userId");
        String flightId = (String) body.get("flightId");
        // FIX #8: safe cast from Number
        int seats      = ((Number) body.get("seats")).intValue();
        double price   = ((Number) body.get("price")).doubleValue();
        @SuppressWarnings("unchecked")
        List<String> selectedSeats = (List<String>) body.getOrDefault("selectedSeats", null);
        return ResponseEntity.ok(bookingService.bookFlight(userId, flightId, seats, price, selectedSeats));
    }

    @PostMapping("/hotel")
    public ResponseEntity<Users.Booking> bookhotel(@RequestBody Map<String, Object> body) {
        String userId  = (String) body.get("userId");
        String hotelId = (String) body.get("hotelId");
        // FIX #8: safe cast from Number
        int rooms      = ((Number) body.get("rooms")).intValue();
        double price   = ((Number) body.get("price")).doubleValue();
        String selectedRoomType = (String) body.getOrDefault("selectedRoomType", null);
        return ResponseEntity.ok(bookingService.bookhotel(userId, hotelId, rooms, price, selectedRoomType));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Users.Booking> cancelBooking(
            @RequestParam String userId,
            @RequestParam String bookingId,
            @RequestParam String reason) {
        return ResponseEntity.ok(cancellationService.cancelBooking(userId, bookingId, reason));
    }

    @GetMapping("/my-booking")
    public ResponseEntity<List<Users.Booking>> getMyBooking(@RequestParam String userId) {
        return ResponseEntity.ok(cancellationService.getBookingsForUser(userId));
    }

    @GetMapping("/cancellation-reasons")
    public ResponseEntity<List<Map<String, String>>> getCancellationReasons() {
        List<Map<String, String>> reasons = Arrays.stream(CancellationReason.values())
            .map(r -> Map.of("value", r.name(), "label", r.getLabel()))
            .toList();
        return ResponseEntity.ok(reasons);
    }
}

package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.services.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seats")
@CrossOrigin(origins = "*")
public class SeatController {

    @Autowired private SeatService seatService;

    // ── Flight seat map ──────────────────────────────────────

    // GET /seats/flight/{flightId}
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<Flight> getSeatMap(@PathVariable String flightId) {
        return ResponseEntity.ok(seatService.getSeatMap(flightId));
    }

    // POST /seats/flight/{flightId}/generate?model=AIR_ASIA  (admin)
    @PostMapping("/flight/{flightId}/generate")
    public ResponseEntity<Flight> generateSeatMap(
            @PathVariable String flightId,
            @RequestParam String model) {
        return ResponseEntity.ok(seatService.generateSeatMap(flightId, model));
    }

    // POST /seats/flight/{flightId}/lock  body: { userId, seatNumbers: [] }
    @PostMapping("/flight/{flightId}/lock")
    public ResponseEntity<Map<String, Object>> lockSeats(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        @SuppressWarnings("unchecked")
        List<String> seatNumbers = (List<String>) body.get("seatNumbers");
        seatService.lockSeats(flightId, seatNumbers, userId);
        return ResponseEntity.ok(Map.of("success", true, "locked", seatNumbers));
    }

    // POST /seats/flight/{flightId}/confirm  body: { seatNumbers: [] }
    @PostMapping("/flight/{flightId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmSeats(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> seatNumbers = (List<String>) body.get("seatNumbers");
        seatService.confirmSeats(flightId, seatNumbers);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /seats/flight/{flightId}/unlock  body: { userId, seatNumbers: [] }
    @PostMapping("/flight/{flightId}/unlock")
    public ResponseEntity<Map<String, Object>> unlockSeats(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        @SuppressWarnings("unchecked")
        List<String> seatNumbers = (List<String>) body.get("seatNumbers");
        seatService.unlockSeats(flightId, seatNumbers, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Hotel room types ─────────────────────────────────────

    // GET /seats/hotel/{hotelId}
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<Hotel> getRoomTypes(@PathVariable String hotelId) {
        return ResponseEntity.ok(seatService.getRoomTypes(hotelId));
    }

    // POST /seats/hotel/{hotelId}/book  body: { roomType, quantity }
    @PostMapping("/hotel/{hotelId}/book")
    public ResponseEntity<Hotel> bookRoom(
            @PathVariable String hotelId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(seatService.bookRoom(
            hotelId,
            (String) body.get("roomType"),
            (Integer) body.get("quantity")
        ));
    }

    // ── User preferences ─────────────────────────────────────

    // POST /seats/preferences  body: { userId, seatPreference, roomPreference }
    @PostMapping("/preferences")
    public ResponseEntity<Users> savePreferences(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(seatService.savePreferences(
            body.get("userId"),
            body.get("seatPreference"),
            body.get("roomPreference")
        ));
    }
}

// ═══════════════════════════════════════════════════════════
// controllers/SeatController.java
// FIX #9: (Integer) body.get("quantity") → safe Number cast
// ═══════════════════════════════════════════════════════════

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
public class SeatController {

    @Autowired private SeatService seatService;

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<Flight> getSeatMap(@PathVariable String flightId) {
        return ResponseEntity.ok(seatService.getSeatMap(flightId));
    }

    @PostMapping("/flight/{flightId}/generate")
    public ResponseEntity<Flight> generateSeatMap(
            @PathVariable String flightId,
            @RequestParam String model) {
        return ResponseEntity.ok(seatService.generateSeatMap(flightId, model));
    }

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

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<Hotel> getRoomTypes(@PathVariable String hotelId) {
        return ResponseEntity.ok(seatService.getRoomTypes(hotelId));
    }

    @PostMapping("/hotel/{hotelId}/book")
    public ResponseEntity<Hotel> bookRoom(
            @PathVariable String hotelId,
            @RequestBody Map<String, Object> body) {
        // FIX #9: safe Number cast
        int quantity = ((Number) body.get("quantity")).intValue();
        return ResponseEntity.ok(seatService.bookRoom(
            hotelId, (String) body.get("roomType"), quantity
        ));
    }

    @PostMapping("/preferences")
    public ResponseEntity<Users> savePreferences(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(seatService.savePreferences(
            body.get("userId"), body.get("seatPreference"), body.get("roomPreference")
        ));
    }
}


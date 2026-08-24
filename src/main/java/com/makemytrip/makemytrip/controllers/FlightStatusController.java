package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.services.FlightStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flight-status")
@CrossOrigin(origins = "*")
public class FlightStatusController {
    @Autowired
    private FlightStatusService flightStatusService;

    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getStatus(@PathVariable String flightId) {
        return ResponseEntity.ok(flightStatusService.getFlightStatus(flightId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchByDate(@RequestParam String from, @RequestParam String to, @RequestParam String date) {
        return ResponseEntity.ok(flightStatusService.searchByDate(from, to, date));
    }

    @PutMapping("/admin/{flightId}")
    public ResponseEntity<Flight> adminUpdateStatus( @PathVariable String flightId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(flightStatusService.updateStatus(
                flightId,
                (String) body.get("status"),
                (String) body.getOrDefault("reason", ""),
                (String) body.getOrDefault("estimatedDeparture", null),
                body.get("delayMinutes") != null ? (Integer) body.get("delayMinutes") : 0
        ));
    }

    @PostMapping("/admin/template")
    public ResponseEntity<Flight> createTemplate(@RequestBody Flight template) {
        return ResponseEntity.ok(flightStatusService.createTemplate(template));
    }
}

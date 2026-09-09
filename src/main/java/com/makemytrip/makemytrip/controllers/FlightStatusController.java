package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.services.FlightStatusService;
import com.makemytrip.makemytrip.services.RecurringFlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flight-status")
@CrossOrigin(origins = "*")
public class FlightStatusController {

    @Autowired private FlightStatusService flightStatusService;
    @Autowired private RecurringFlightService recurringFlightService;

    // GET /flight-status/{flightId}
    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getStatus(@PathVariable String flightId) {
        return ResponseEntity.ok(flightStatusService.getFlightStatus(flightId));
    }

    // GET /flight-status/search?from=BLR&to=DEL&date=2026-07-20
    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchByDate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String date) {
        return ResponseEntity.ok(flightStatusService.searchByDate(from, to, date));
    }

    // PUT /flight-status/admin/{flightId}
    @PutMapping("/admin/{flightId}")
    public ResponseEntity<Flight> adminUpdateStatus(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(flightStatusService.updateStatus(
            flightId,
            (String) body.get("status"),
            (String) body.getOrDefault("reason", ""),
            (String) body.getOrDefault("estimatedDeparture", null),
            body.get("delayMinutes") != null ? ((Number) body.get("delayMinutes")).intValue() : 0
        ));
    }

    // POST /flight-status/admin/template
    // Creates the template AND immediately generates flights for today + 4 weeks
    @PostMapping("/admin/template")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Flight template) {
        Flight saved = flightStatusService.createTemplate(template);
        List<Flight> generated = recurringFlightService.generateFromTemplate(saved);
        return ResponseEntity.ok(Map.of(
            "template", saved,
            "generatedCount", generated.size(),
            "generatedFlights", generated
        ));
    }

    // GET /flight-status/admin/templates — list all templates
    @GetMapping("/admin/templates")
    public ResponseEntity<List<Flight>> getAllTemplates() {
        return ResponseEntity.ok(recurringFlightService.getAllTemplates());
    }

    // GET /flight-status/admin/generated/{templateId} — flights from a template
    @GetMapping("/admin/generated/{templateId}")
    public ResponseEntity<List<Flight>> getGeneratedFlights(@PathVariable String templateId) {
        return ResponseEntity.ok(recurringFlightService.getGeneratedFlights(templateId));
    }

    // POST /flight-status/admin/regenerate/{templateId} — force regenerate now
    @PostMapping("/admin/regenerate/{templateId}")
    public ResponseEntity<Map<String, Object>> regenerate(@PathVariable String templateId) {
        return recurringFlightService.getAllTemplates().stream()
            .filter(t -> templateId.equals(t.getId()))
            .findFirst()
            .map(template -> {
                List<Flight> generated = recurringFlightService.generateFromTemplate(template);
                return ResponseEntity.ok(Map.of(
                    "generatedCount", generated.size(),
                    "generatedFlights", generated
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}

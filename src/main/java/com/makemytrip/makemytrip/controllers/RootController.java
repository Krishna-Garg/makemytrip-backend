package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class RootController {

    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;

    // Lightweight flight DTO — no seats[] array
    private Map<String, Object> flightDto(com.makemytrip.makemytrip.models.Flight f) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("_id",            f.getId() != null ? f.getId() : "");
        dto.put("flightName",     f.getFlightName() != null ? f.getFlightName() : "");
        dto.put("from",           f.getFrom() != null ? f.getFrom() : "");
        dto.put("to",             f.getTo() != null ? f.getTo() : "");
        dto.put("departureTime",  f.getDepartureTime() != null ? f.getDepartureTime() : "");
        dto.put("arrivalTime",    f.getArrivalTime() != null ? f.getArrivalTime() : "");
        dto.put("price",          f.getPrice());
        dto.put("availableSeats", f.getAvailableSeats());
        dto.put("status",         f.getStatus() != null ? f.getStatus() : "ON_TIME");
        dto.put("aircraftModel",  f.getAircraftModel() != null ? f.getAircraftModel() : "");
        dto.put("boardingMinutes",f.getBoardingMinutes());
        dto.put("delayMinutes",   f.getDelayMinutes());
        dto.put("statusReason",   f.getStatusReason() != null ? f.getStatusReason() : "");
        return dto;
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to the backend";
    }
    // GET /flight — excludes templates and expired, strips seats[]
    @GetMapping("/flight")
    public ResponseEntity<List<Map<String, Object>>> getallflights() {
        List<Map<String, Object>> flights = flightRepository.findAll().stream()
            .filter(f -> !f.isTemplate())
            .filter(f -> {
                String s = f.getStatus();
                return s == null || (!s.equals("EXPIRED") && !s.equals("DEPARTED"));
            })
            .map(this::flightDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/hotel")
    public ResponseEntity<List<Hotel>> getallhotels() {
        return ResponseEntity.ok(hotelRepository.findAll());
    }

    // GET /home-data?date=2026-07-20 — single round trip for homepage
    @GetMapping("/home-data")
    public ResponseEntity<Map<String, Object>> getHomeData(
            @RequestParam(required = false) String date) {

        List<Map<String, Object>> flights = flightRepository.findAll().stream()
            .filter(f -> !f.isTemplate())
            .filter(f -> {
                String s = f.getStatus();
                return s == null || (!s.equals("EXPIRED") && !s.equals("DEPARTED"));
            })
            .filter(f -> {
                if (date == null || date.isEmpty()) return true;
                try { return f.getDepartureTime().substring(0, 10).equals(date); }
                catch (Exception e) { return true; }
            })
            .map(this::flightDto)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("flights", flights);
        result.put("hotels", hotelRepository.findAll());
        return ResponseEntity.ok(result);
    }
}

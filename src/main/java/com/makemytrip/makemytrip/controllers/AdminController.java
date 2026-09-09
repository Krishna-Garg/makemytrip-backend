package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import com.makemytrip.makemytrip.services.CancellationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CancellationService cancellationService;

    // ── Flights ──────────────────────────────────────────────────────────────

    @PostMapping("/flight")
    public ResponseEntity<Flight> addFlight(@RequestBody Flight flight) {
        return ResponseEntity.ok(flightRepository.save(flight));
    }

    @PutMapping("/flight/{id}")
    public ResponseEntity<Flight> editFlight(@PathVariable String id, @RequestBody Flight updated) {
        return flightRepository.findById(id).map(flight -> {
            flight.setFlightName(updated.getFlightName());
            flight.setFrom(updated.getFrom());
            flight.setTo(updated.getTo());
            flight.setDepartureTime(updated.getDepartureTime());
            flight.setArrivalTime(updated.getArrivalTime());
            flight.setPrice(updated.getPrice());
            flight.setAvailableSeats(updated.getAvailableSeats());
            if (updated.getBoardingMinutes() > 0) flight.setBoardingMinutes(updated.getBoardingMinutes());
            return ResponseEntity.ok(flightRepository.save(flight));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/flight/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable String id) {
        flightRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Hotels ───────────────────────────────────────────────────────────────

    @PostMapping("/hotel")
    public ResponseEntity<Hotel> addHotel(@RequestBody Hotel hotel) {
        return ResponseEntity.ok(hotelRepository.save(hotel));
    }

    @PutMapping("/hotel/{id}")
    public ResponseEntity<Hotel> editHotel(@PathVariable String id, @RequestBody Hotel updated) {
        return hotelRepository.findById(id).map(hotel -> {
            hotel.setHotelName(updated.getHotelName());
            hotel.setLocation(updated.getLocation());
            hotel.setPricePerNight(updated.getPricePerNight());
            hotel.setAvailableRooms(updated.getAvailableRooms());
            hotel.setAmenities(updated.getAmenities());
            if (updated.getImageUrls() != null) hotel.setImageUrls(updated.getImageUrls());
            return ResponseEntity.ok(hotelRepository.save(hotel));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/hotel/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable String id) {
        hotelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Users — password stripped from all responses ──────────────────────────

    private Map<String, Object> safeUser(Users u) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", u.getId());
        safe.put("firstName", u.getFirstName());
        safe.put("lastName", u.getLastName());
        safe.put("email", u.getEmail());
        safe.put("role", u.getRole());
        safe.put("phoneNumber", u.getPhoneNumber());
        safe.put("bookings", u.getBookings());
        return safe;
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(
            userRepository.findAll().stream().map(this::safeUser).collect(Collectors.toList())
        );
    }

    // ── Refunds ───────────────────────────────────────────────────────────────

    @GetMapping("/refunds")
    public ResponseEntity<List<Map<String, Object>>> getAllRefunds() {
        return ResponseEntity.ok(cancellationService.getAllCancelledBookings());
    }

    @PutMapping("/refund/status")
    public ResponseEntity<Users.Booking> updateRefundStatus(
            @RequestParam String userId,
            @RequestParam String bookingId,
            @RequestParam String status) {
        return ResponseEntity.ok(cancellationService.updateRefundStatus(userId, bookingId, status));
    }
}

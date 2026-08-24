package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.models.Users.Booking;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CancellationService {

    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RefundPolicyService refundPolicyService;

    public Booking cancelBooking(String userId, String bookingId, String reason) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = findActiveBooking(user, bookingId);

        if ("Flight".equals(booking.getType())) {
            return cancelFlightBooking(user, booking, reason);
        } else if ("Hotel".equals(booking.getType())) {
            return cancelHotelBooking(user, booking, reason);
        }
        throw new RuntimeException("Unknown booking type: " + booking.getType());
    }

    // ── Flight cancellation ──────────────────────────────────────────────────

    private Booking cancelFlightBooking(Users user, Booking booking, String reason) {
        LocalDateTime departureTime = parseDepartureTime(booking);
        RefundPolicyService.RefundResult refund =
            refundPolicyService.calculateRefund(booking.getTotalPrice(), departureTime, LocalDateTime.now());

        applyCancellation(booking, reason, refund);

        // Release seats back to inventory
        flightRepository.findById(booking.getBookingId()).ifPresent(flight -> {
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getQuantity());
            // Unlock specific seats if selected
            if (booking.getSelectedSeats() != null) {
                flight.getSeats().stream()
                    .filter(s -> booking.getSelectedSeats().contains(s.getSeatNumber()))
                    .forEach(s -> {
                        s.setStatus("AVAILABLE");
                        s.setLockedByUserId(null);
                    });
            }
            flightRepository.save(flight);
        });

        userRepository.save(user);
        return booking;
    }

    // ── Hotel cancellation ───────────────────────────────────────────────────

    private Booking cancelHotelBooking(Users user, Booking booking, String reason) {
        // Hotel refund policy: flat 75% if cancelled (no departure time concept)
        // More than 48h before check-in → 100%, within 48h → 50%, same day → 0%
        // Since hotels don't store check-in date on booking yet, use a flat 75% refund
        RefundPolicyService.RefundResult refund = new RefundPolicyService.RefundResult();
        refund.refundPercentage = 75;
        refund.refundAmount = booking.getTotalPrice() * 0.75;
        refund.tierDescription = "Hotel cancellation — 75% refund";
        refund.estimatedEta = "5-7 business days";

        applyCancellation(booking, reason, refund);

        // Release rooms back to inventory
        hotelRepository.findById(booking.getBookingId()).ifPresent(hotel -> {
            hotel.setAvailableRooms(hotel.getAvailableRooms() + booking.getQuantity());
            // Release specific room type count if selected
            if (booking.getSelectedRoomType() != null) {
                hotel.getRoomTypes().stream()
                    .filter(rt -> rt.getType().equals(booking.getSelectedRoomType()))
                    .findFirst()
                    .ifPresent(rt -> rt.setAvailableRooms(rt.getAvailableRooms() + booking.getQuantity()));
            }
            hotelRepository.save(hotel);
        });

        userRepository.save(user);
        return booking;
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

    private Booking findActiveBooking(Users user, String bookingId) {
        return user.getBookings().stream()
            .filter(b -> b.getBookingId().equals(bookingId) && "CONFIRMED".equals(b.getBookingStatus()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Active booking not found"));
    }

    private LocalDateTime parseDepartureTime(Booking booking) {
        try {
            return LocalDateTime.parse(booking.getDepartureTime());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse flight departure time for refund calculation");
        }
    }

    private void applyCancellation(Booking booking, String reason, RefundPolicyService.RefundResult refund) {
        booking.setBookingStatus("CANCELLED");
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now().toString());
        booking.setRefundAmount(refund.refundAmount);
        booking.setRefundStatus(refund.refundPercentage > 0 ? "PENDING" : "REJECTED");
        booking.setRefundRequestedAt(LocalDateTime.now().toString());
        booking.setRefundEta(refund.estimatedEta);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<Booking> getBookingsForUser(String userId) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getBookings();
    }

    public Booking updateRefundStatus(String userId, String bookingId, String newStatus) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = user.getBookings().stream()
            .filter(b -> b.getBookingId().equals(bookingId) && "CANCELLED".equals(b.getBookingStatus()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Cancelled booking not found"));
        booking.setRefundStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            booking.setRefundCompletedAt(LocalDateTime.now().toString());
        }
        userRepository.save(user);
        return booking;
    }

    public List<Map<String, Object>> getAllCancelledBookings() {
        List<Users> allUsers = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Users user : allUsers) {
            for (Booking booking : user.getBookings()) {
                if ("CANCELLED".equals(booking.getBookingStatus())) {
                    Map<String, Object> entry = new java.util.HashMap<>();
                    entry.put("userId", user.getId());
                    entry.put("userEmail", user.getEmail());
                    entry.put("userName", user.getFirstName() + " " + user.getLastName());
                    entry.put("booking", booking);
                    result.add(entry);
                }
            }
        }
        return result;
    }
}

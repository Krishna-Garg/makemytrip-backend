package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.models.PriceFreeze;
import com.makemytrip.makemytrip.models.PriceSnapshot;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import com.makemytrip.makemytrip.repositories.PriceFreezeRepository;
import com.makemytrip.makemytrip.repositories.PriceSnapshotRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.*;

@Service
public class PricingEngine {

    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PriceSnapshotRepository snapshotRepository;
    @Autowired private PriceFreezeRepository freezeRepository;

    private static final double MIN_BOOKING_PRICE = 2000.0;

    // ── Peak / holiday date ranges ────────────────────────────────────────────
    // Format: MM-DD inclusive ranges. +20% applied when today falls in any range.
    private static final List<MonthDay[]> PEAK_PERIODS = List.of(
        new MonthDay[]{MonthDay.of(12, 20), MonthDay.of(1, 5)},   // Christmas + New Year
        new MonthDay[]{MonthDay.of(6, 1),  MonthDay.of(7, 31)},   // Summer peak
        new MonthDay[]{MonthDay.of(10, 2), MonthDay.of(10, 6)},   // Dussehra / Gandhi Jayanti
        new MonthDay[]{MonthDay.of(10, 20), MonthDay.of(11, 5)},  // Diwali window
        new MonthDay[]{MonthDay.of(3, 25), MonthDay.of(4, 5)}     // Holi
    );

    private boolean isPeakSeason(LocalDate date) {
        MonthDay today = MonthDay.from(date);
        for (MonthDay[] range : PEAK_PERIODS) {
            MonthDay start = range[0];
            MonthDay end   = range[1];
            // Handle year wrap (Dec-Jan)
            if (start.isAfter(end)) {
                if (!today.isBefore(start) || !today.isAfter(end)) return true;
            } else {
                if (!today.isBefore(start) && !today.isAfter(end)) return true;
            }
        }
        return false;
    }

    // ── Tier logic ────────────────────────────────────────────────────────────

    public String getUserTier(String userId) {
        if (userId == null) return "BASIC";
        Users user = userRepository.findById(userId).orElse(null);
        if (user == null) return "BASIC";
        long qualifying = user.getBookings().stream()
            .filter(b -> b.getTotalPrice() >= MIN_BOOKING_PRICE)
            .count();
        if (qualifying >= 15) return "PLATINUM";
        if (qualifying >= 7)  return "GOLD";
        if (qualifying >= 3)  return "SILVER";
        return "BASIC";
    }

    public double getTierDiscount(String tier) {
        return switch (tier) {
            case "SILVER"   -> 0.05;
            case "GOLD"     -> 0.12;
            case "PLATINUM" -> 0.20;
            default         -> 0.0;
        };
    }

    // ── Flight demand multiplier ──────────────────────────────────────────────

    public double getDemandMultiplier(Flight flight) {
        int total = flight.getAvailableSeats() + countFlightBookings(flight.getId());
        if (total == 0) return 1.5;
        double remaining = (double) flight.getAvailableSeats() / total;
        if (remaining > 0.70) return 1.0;
        if (remaining > 0.40) return 1.15;
        if (remaining > 0.20) return 1.30;
        return 1.50;
    }

    // ── Hotel demand multiplier ───────────────────────────────────────────────

    public double getHotelDemandMultiplier(Hotel hotel) {
        int total = hotel.getAvailableRooms() + countHotelBookings(hotel.getId());
        if (total == 0) return 1.5;
        double remaining = (double) hotel.getAvailableRooms() / total;
        if (remaining > 0.70) return 1.0;
        if (remaining > 0.40) return 1.10;
        if (remaining > 0.20) return 1.25;
        return 1.45;
    }

    private int countFlightBookings(String flightId) {
        return userRepository.findAll().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> flightId.equals(b.getBookingId()) && "Flight".equals(b.getType()))
            .mapToInt(Users.Booking::getQuantity).sum();
    }

    private int countHotelBookings(String hotelId) {
        return userRepository.findAll().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> hotelId.equals(b.getBookingId()) && "Hotel".equals(b.getType()))
            .mapToInt(Users.Booking::getQuantity).sum();
    }

    // ── Effective price — FLIGHT ──────────────────────────────────────────────

    public Map<String, Object> getEffectivePrice(String flightId, String userId) {
        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new RuntimeException("Flight not found"));

        double demandMultiplier = getDemandMultiplier(flight);
        boolean peak = isPeakSeason(LocalDate.now());
        double seasonalMultiplier = peak ? 1.20 : 1.0;
        double combinedMultiplier = Math.min(demandMultiplier * seasonalMultiplier, 2.0);
        double dynamicPrice = Math.round(flight.getPrice() * combinedMultiplier);

        String tier = getUserTier(userId);
        double discount = getTierDiscount(tier);
        double finalPrice = Math.round(dynamicPrice * (1 - discount));

        boolean hasFrozen = false;
        double frozenPrice = 0;
        if (userId != null) {
            var freeze = freezeRepository.findByUserIdAndFlightIdAndUsedFalse(userId, flightId);
            if (freeze.isPresent()) {
                LocalDateTime expiry = LocalDateTime.parse(freeze.get().getExpiresAt());
                if (expiry.isAfter(LocalDateTime.now())) {
                    hasFrozen = true;
                    frozenPrice = freeze.get().getFrozenPrice();
                    finalPrice = frozenPrice;
                }
            }
        }

        String demandReason = demandMultiplier == 1.0 ? "Standard demand"
            : demandMultiplier <= 1.15 ? "Moderate demand"
            : demandMultiplier <= 1.30 ? "High demand"
            : "Very high demand";
        String multiplierReason = peak ? demandReason + " + Peak season (+20%)" : demandReason;

        Map<String, Object> result = new HashMap<>();
        result.put("flightId", flightId);
        result.put("basePrice", flight.getPrice());
        result.put("demandMultiplier", demandMultiplier);
        result.put("seasonalMultiplier", seasonalMultiplier);
        result.put("multiplier", combinedMultiplier);
        result.put("multiplierReason", multiplierReason);
        result.put("isPeakSeason", peak);
        result.put("dynamicPrice", dynamicPrice);
        result.put("userTier", tier);
        result.put("tierDiscount", (int)(discount * 100) + "%");
        result.put("finalPrice", finalPrice);
        result.put("hasFrozen", hasFrozen);
        result.put("frozenPrice", frozenPrice);
        return result;
    }

    // ── Effective price — HOTEL ───────────────────────────────────────────────

    public Map<String, Object> getHotelEffectivePrice(String hotelId, String userId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));

        double demandMultiplier = getHotelDemandMultiplier(hotel);
        boolean peak = isPeakSeason(LocalDate.now());
        double seasonalMultiplier = peak ? 1.20 : 1.0;
        double combinedMultiplier = Math.min(demandMultiplier * seasonalMultiplier, 2.0);
        double dynamicPrice = Math.round(hotel.getPricePerNight() * combinedMultiplier);

        String tier = getUserTier(userId);
        double discount = getTierDiscount(tier);
        double finalPrice = Math.round(dynamicPrice * (1 - discount));

        String demandReason = demandMultiplier == 1.0 ? "Standard availability"
            : demandMultiplier <= 1.10 ? "Moderate demand"
            : demandMultiplier <= 1.25 ? "High demand"
            : "Very high demand — few rooms left";
        String multiplierReason = peak ? demandReason + " + Peak season (+20%)" : demandReason;

        Map<String, Object> result = new HashMap<>();
        result.put("hotelId", hotelId);
        result.put("basePrice", hotel.getPricePerNight());
        result.put("multiplier", combinedMultiplier);
        result.put("multiplierReason", multiplierReason);
        result.put("isPeakSeason", peak);
        result.put("dynamicPrice", dynamicPrice);
        result.put("userTier", tier);
        result.put("tierDiscount", (int)(discount * 100) + "%");
        result.put("finalPrice", finalPrice);
        return result;
    }

    // ── Price freeze — flight only ────────────────────────────────────────────

    public PriceFreeze freezePrice(String userId, String flightId) {
        String tier = getUserTier(userId);
        int freezeMinutes = "PLATINUM".equals(tier) ? 120 : 30;
        Map<String, Object> pricing = getEffectivePrice(flightId, userId);
        double priceToFreeze = (double) pricing.get("finalPrice");

        PriceFreeze freeze = new PriceFreeze();
        freeze.setUserId(userId);
        freeze.setFlightId(flightId);
        freeze.setFrozenPrice(priceToFreeze);
        freeze.setCreatedAt(LocalDateTime.now().toString());
        freeze.setExpiresAt(LocalDateTime.now().plusMinutes(freezeMinutes).toString());
        return freezeRepository.save(freeze);
    }

    // ── Price history snapshots (hourly) ──────────────────────────────────────

    @Scheduled(fixedRate = 3600000)
    public void logPriceSnapshots() {
        // Flights
        flightRepository.findAll().stream()
            .filter(f -> !f.isTemplate())
            .forEach(flight -> {
                double multiplier = getDemandMultiplier(flight);
                double effective = Math.round(flight.getPrice() * multiplier);
                PriceSnapshot snap = new PriceSnapshot();
                snap.setFlightId(flight.getId());
                snap.setBasePrice(flight.getPrice());
                snap.setEffectivePrice(effective);
                snap.setMultiplier(multiplier);
                snap.setReason(multiplier == 1.0 ? "Standard" : "Demand-based");
                snap.setTimestamp(LocalDateTime.now().toString());
                snapshotRepository.save(snap);
            });
    }

    public List<PriceSnapshot> getPriceHistory(String flightId) {
        return snapshotRepository.findByFlightIdOrderByTimestampAsc(flightId);
    }

    public List<PriceFreeze> getUserFreezes(String userId) {
        return freezeRepository.findByUserId(userId);
    }
}

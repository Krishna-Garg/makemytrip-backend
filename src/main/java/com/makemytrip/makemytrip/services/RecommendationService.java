package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.*;
import com.makemytrip.makemytrip.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RecommendationFeedbackRepository feedbackRepository;

    // Cache: userId → list of recommendation maps
    private final Map<String, List<Map<String, Object>>> cache = new HashMap<>();
    private final Map<String, String> cacheTimestamp = new HashMap<>();

    // ── Public entry point ───────────────────────────────────────────────────

    public List<Map<String, Object>> getRecommendations(String userId) {
        // Return cached if less than 24h old
        if (cache.containsKey(userId)) {
            String ts = cacheTimestamp.get(userId);
            if (ts != null) {
                LocalDateTime cached = LocalDateTime.parse(ts);
                if (cached.isAfter(LocalDateTime.now().minusHours(24))) {
                    return cache.get(userId);
                }
            }
        }
        return computeAndCache(userId);
    }

    public List<Map<String, Object>> refreshRecommendations(String userId) {
        return computeAndCache(userId);
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    public void saveFeedback(String userId, String targetId, String targetType, String feedback) {
        // Remove old feedback for same target if exists
        feedbackRepository.findByUserId(userId).stream()
            .filter(f -> f.getTargetId().equals(targetId))
            .forEach(f -> feedbackRepository.deleteById(f.getId()));

        RecommendationFeedback fb = new RecommendationFeedback();
        fb.setUserId(userId);
        fb.setTargetId(targetId);
        fb.setTargetType(targetType);
        fb.setFeedback(feedback);
        fb.setCreatedAt(LocalDateTime.now().toString());
        feedbackRepository.save(fb);

        // Bust cache so next call recomputes
        cache.remove(userId);
    }

    // ── Nightly refresh for all users ─────────────────────────────────────────
    @Scheduled(cron = "0 0 3 * * *") // 3am daily
    public void refreshAllUsers() {
        userRepository.findAll().forEach(u -> computeAndCache(u.getId()));
    }

    // ── Core computation ──────────────────────────────────────────────────────

    private List<Map<String, Object>> computeAndCache(String userId) {
        List<Map<String, Object>> results;

        Users user = userRepository.findById(userId).orElse(null);

        // Collect irrelevant feedback to exclude those targets
        Set<String> irrelevantTargets = new HashSet<>();
        if (userId != null) {
            feedbackRepository.findByUserId(userId).stream()
                .filter(f -> "IRRELEVANT".equals(f.getFeedback()))
                .forEach(f -> irrelevantTargets.add(f.getTargetId()));
        }

        if (user == null || user.getBookings() == null || user.getBookings().isEmpty()) {
            results = coldStartRecommendations(irrelevantTargets);
        } else {
            results = personalizedRecommendations(user, irrelevantTargets);
        }

        // Cap at 4
        if (results.size() > 4) results = results.subList(0, 4);

        if (userId != null) {
            cache.put(userId, results);
            cacheTimestamp.put(userId, LocalDateTime.now().toString());
        }
        return results;
    }

    // ── Cold start: most-booked destinations ─────────────────────────────────

    private List<Map<String, Object>> coldStartRecommendations(Set<String> exclude) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Count bookings per flight
        Map<String, Long> flightBookingCount = userRepository.findAll().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "Flight".equals(b.getType()))
            .collect(Collectors.groupingBy(Users.Booking::getBookingId, Collectors.counting()));

        // Top flights by booking count
        flightBookingCount.entrySet().stream()
            .filter(e -> !exclude.contains(e.getKey()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> flightRepository.findById(e.getKey()).ifPresent(f -> {
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "FLIGHT");
                rec.put("targetId", f.getId());
                rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                rec.put("subtitle", "₹" + (long) f.getPrice());
                rec.put("reason", "Popular route — booked " + e.getValue() + " times by travelers");
                rec.put("tag", "TRENDING");
                results.add(rec);
            }));

        // Count bookings per hotel
        Map<String, Long> hotelBookingCount = userRepository.findAll().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "Hotel".equals(b.getType()))
            .collect(Collectors.groupingBy(Users.Booking::getBookingId, Collectors.counting()));

        hotelBookingCount.entrySet().stream()
            .filter(e -> !exclude.contains(e.getKey()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> hotelRepository.findById(e.getKey()).ifPresent(h -> {
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "HOTEL");
                rec.put("targetId", h.getId());
                rec.put("title", h.getHotelName());
                rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                rec.put("reason", "Trending stay — booked " + e.getValue() + " times by travelers");
                rec.put("tag", "POPULAR");
                results.add(rec);
            }));

        return results;
    }

    // ── Personalized: content-based + collaborative ───────────────────────────

    private List<Map<String, Object>> personalizedRecommendations(
            Users user, Set<String> exclude) {

        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> alreadyBooked = user.getBookings().stream()
            .map(Users.Booking::getBookingId).collect(Collectors.toSet());

        // ── 1. Content-based: extract signals from user history ───────────────

        // Most frequent destination (flight "to" city)
        Map<String, Long> destCount = new HashMap<>();
        Map<String, Long> originCount = new HashMap<>();
        double totalSpend = 0;
        int flightCount = 0;

        for (Users.Booking b : user.getBookings()) {
            totalSpend += b.getTotalPrice();
            if ("Flight".equals(b.getType())) {
                flightCount++;
                flightRepository.findById(b.getBookingId()).ifPresent(f -> {
                    destCount.merge(f.getTo(), 1L, Long::sum);
                    originCount.merge(f.getFrom(), 1L, Long::sum);
                });
            }
        }

        double avgSpend = user.getBookings().isEmpty() ? 0 : totalSpend / user.getBookings().size();

        // Top destination
        String favDest = destCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);

        // Recommend flights TO favourite destination from any origin
        if (favDest != null) {
            final String dest = favDest;
            flightRepository.findAll().stream()
                .filter(f -> dest.equalsIgnoreCase(f.getTo()))
                .filter(f -> !alreadyBooked.contains(f.getId()))
                .filter(f -> !exclude.contains(f.getId()))
                .filter(f -> !f.isTemplate())
                .limit(1)
                .forEach(f -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "FLIGHT");
                    rec.put("targetId", f.getId());
                    rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                    rec.put("subtitle", "₹" + (long) f.getPrice());
                    rec.put("reason", "You've flown to " + dest + " before — here's another option");
                    rec.put("tag", "BASED ON YOUR HISTORY");
                    results.add(rec);
                });
        }

        // Recommend hotels in favourite destination city
        if (favDest != null) {
            final String dest = favDest;
            hotelRepository.findAll().stream()
                .filter(h -> h.getLocation() != null && h.getLocation().toLowerCase().contains(dest.toLowerCase()))
                .filter(h -> !alreadyBooked.contains(h.getId()))
                .filter(h -> !exclude.contains(h.getId()))
                .limit(1)
                .forEach(h -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "HOTEL");
                    rec.put("targetId", h.getId());
                    rec.put("title", h.getHotelName());
                    rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                    rec.put("reason", "You love " + dest + " — stay here next time");
                    rec.put("tag", "YOU MIGHT LIKE");
                    results.add(rec);
                });
        }

        // ── 2. Collaborative filtering: find similar users ────────────────────

        Set<String> userDestinations = new HashSet<>(destCount.keySet());
        double finalAvgSpend = avgSpend;

        List<Users> similarUsers = userRepository.findAll().stream()
            .filter(u -> !u.getId().equals(user.getId()))
            .filter(u -> u.getBookings() != null && !u.getBookings().isEmpty())
            .filter(u -> {
                // Similar if they share at least one destination
                Set<String> theirDests = new HashSet<>();
                u.getBookings().stream()
                    .filter(b -> "Flight".equals(b.getType()))
                    .forEach(b -> flightRepository.findById(b.getBookingId())
                        .ifPresent(f -> theirDests.add(f.getTo())));
                return theirDests.stream().anyMatch(userDestinations::contains);
            })
            .limit(10)
            .collect(Collectors.toList());

        // What did similar users book that this user hasn't?
        similarUsers.stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> !alreadyBooked.contains(b.getBookingId()))
            .filter(b -> !exclude.contains(b.getBookingId()))
            .filter(b -> b.getTotalPrice() > 0 && Math.abs(b.getTotalPrice() - finalAvgSpend) < finalAvgSpend * 0.5)
            .map(Users.Booking::getBookingId)
            .distinct()
            .limit(2)
            .forEach(targetId -> {
                // Try as flight first
                flightRepository.findById(targetId).ifPresent(f -> {
                    if (!f.isTemplate()) {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("type", "FLIGHT");
                        rec.put("targetId", f.getId());
                        rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                        rec.put("subtitle", "₹" + (long) f.getPrice());
                        rec.put("reason", "Travelers with similar taste to you booked this");
                        rec.put("tag", "TRAVELERS LIKE YOU");
                        results.add(rec);
                    }
                });
                // Try as hotel
                hotelRepository.findById(targetId).ifPresent(h -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "HOTEL");
                    rec.put("targetId", h.getId());
                    rec.put("title", h.getHotelName());
                    rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                    rec.put("reason", "Travelers with similar taste to you stayed here");
                    rec.put("tag", "TRAVELERS LIKE YOU");
                    results.add(rec);
                });
            });

        // If not enough personalized, pad with cold start
        if (results.size() < 4) {
            Set<String> existingIds = results.stream()
                .map(r -> (String) r.get("targetId")).collect(Collectors.toSet());
            exclude.addAll(existingIds);
            exclude.addAll(alreadyBooked);
            List<Map<String, Object>> padding = coldStartRecommendations(exclude);
            for (Map<String, Object> p : padding) {
                if (results.size() >= 4) break;
                results.add(p);
            }
        }

        return results;
    }
}

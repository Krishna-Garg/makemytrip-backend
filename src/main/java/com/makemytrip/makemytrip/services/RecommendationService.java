package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.*;
import com.makemytrip.makemytrip.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // FIX #12
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RecommendationFeedbackRepository feedbackRepository;

    // FIX #12: ConcurrentHashMap — safe for @Scheduled concurrent writes
    private final Map<String, List<Map<String, Object>>> cache = new ConcurrentHashMap<>();
    private final Map<String, String> cacheTimestamp = new ConcurrentHashMap<>();

    public List<Map<String, Object>> getRecommendations(String userId) {
        if (userId != null && cache.containsKey(userId)) {
            String ts = cacheTimestamp.get(userId);
            if (ts != null && LocalDateTime.parse(ts).isAfter(LocalDateTime.now().minusHours(24))) {
                return cache.get(userId);
            }
        }
        return computeAndCache(userId);
    }

    public List<Map<String, Object>> refreshRecommendations(String userId) {
        return computeAndCache(userId);
    }

    public void saveFeedback(String userId, String targetId, String targetType, String feedback) {
        feedbackRepository.findByUserId(userId).stream()
            .filter(f -> f.getTargetId().equals(targetId))
            .forEach(f -> feedbackRepository.deleteById(f.getId()));

        RecommendationFeedback fb = new RecommendationFeedback();
        fb.setUserId(userId); fb.setTargetId(targetId);
        fb.setTargetType(targetType); fb.setFeedback(feedback);
        fb.setCreatedAt(LocalDateTime.now().toString());
        feedbackRepository.save(fb);
        cache.remove(userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void refreshAllUsers() {
        userRepository.findAll().forEach(u -> computeAndCache(u.getId()));
    }

    private List<Map<String, Object>> computeAndCache(String userId) {
        Set<String> irrelevant = new HashSet<>();
        if (userId != null) {
            feedbackRepository.findByUserId(userId).stream()
                .filter(f -> "IRRELEVANT".equals(f.getFeedback()))
                .forEach(f -> irrelevant.add(f.getTargetId()));
        }

        Users user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // FIX #13: preload ALL flights and hotels ONCE — no N+1 findById
        Map<String, Flight> flightIndex = new HashMap<>();
        flightRepository.findAll().forEach(f -> flightIndex.put(f.getId(), f));

        Map<String, Hotel> hotelIndex = new HashMap<>();
        hotelRepository.findAll().forEach(h -> hotelIndex.put(h.getId(), h));

        List<Map<String, Object>> results;

        if (user == null || user.getBookings() == null || user.getBookings().isEmpty()) {
            results = coldStart(irrelevant, flightIndex, hotelIndex);
        } else {
            results = personalized(user, irrelevant, flightIndex, hotelIndex);
        }

        if (results.size() > 4) results = results.subList(0, 4);

        if (userId != null) {
            cache.put(userId, results);
            cacheTimestamp.put(userId, LocalDateTime.now().toString());
        }
        return results;
    }

    private List<Map<String, Object>> coldStart(Set<String> exclude,
                                                 Map<String, Flight> flightIndex,
                                                 Map<String, Hotel> hotelIndex) {
        List<Map<String, Object>> results = new ArrayList<>();

        // FIX #23: separate counts for flights and hotels — no ID collision
        Map<String, Long> flightCount = new HashMap<>();
        Map<String, Long> hotelCount  = new HashMap<>();

        userRepository.findAll().forEach(u -> u.getBookings().forEach(b -> {
            if ("Flight".equals(b.getType())) flightCount.merge(b.getBookingId(), 1L, Long::sum);
            else if ("Hotel".equals(b.getType())) hotelCount.merge(b.getBookingId(), 1L, Long::sum);
        }));

        flightCount.entrySet().stream()
            .filter(e -> !exclude.contains(e.getKey()) && flightIndex.containsKey(e.getKey()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> {
                Flight f = flightIndex.get(e.getKey());
                if (f == null || f.isTemplate()) return;
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "FLIGHT"); rec.put("targetId", f.getId());
                rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                rec.put("subtitle", "₹" + (long) f.getPrice());
                rec.put("reason", "Popular route — booked " + e.getValue() + " times");
                rec.put("tag", "TRENDING");
                results.add(rec);
            });

        hotelCount.entrySet().stream()
            .filter(e -> !exclude.contains(e.getKey()) && hotelIndex.containsKey(e.getKey()))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> {
                Hotel h = hotelIndex.get(e.getKey());
                if (h == null) return;
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "HOTEL"); rec.put("targetId", h.getId());
                rec.put("title", h.getHotelName());
                rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                rec.put("reason", "Trending stay — booked " + e.getValue() + " times");
                rec.put("tag", "POPULAR");
                results.add(rec);
            });

        return results;
    }

    private List<Map<String, Object>> personalized(Users user, Set<String> exclude,
                                                     Map<String, Flight> flightIndex,
                                                     Map<String, Hotel> hotelIndex) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> alreadyBooked = user.getBookings().stream()
            .map(Users.Booking::getBookingId).collect(Collectors.toSet());

        // Extract favourite destination
        Map<String, Long> destCount = new HashMap<>();
        double totalSpend = 0;
        for (Users.Booking b : user.getBookings()) {
            totalSpend += b.getTotalPrice();
            if ("Flight".equals(b.getType())) {
                // FIX #13: use preloaded index
                Flight f = flightIndex.get(b.getBookingId());
                if (f != null) destCount.merge(f.getTo(), 1L, Long::sum);
            }
        }

        String favDest = destCount.entrySet().stream()
            .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        double avgSpend = user.getBookings().isEmpty() ? 0 : totalSpend / user.getBookings().size();

        if (favDest != null) {
            final String dest = favDest;
            flightIndex.values().stream()
                .filter(f -> !f.isTemplate() && dest.equalsIgnoreCase(f.getTo()))
                .filter(f -> !alreadyBooked.contains(f.getId()) && !exclude.contains(f.getId()))
                .limit(1).forEach(f -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "FLIGHT"); rec.put("targetId", f.getId());
                    rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                    rec.put("subtitle", "₹" + (long) f.getPrice());
                    rec.put("reason", "You've flown to " + dest + " before");
                    rec.put("tag", "BASED ON YOUR HISTORY");
                    results.add(rec);
                });

            hotelIndex.values().stream()
                .filter(h -> h.getLocation() != null && h.getLocation().toLowerCase().contains(dest.toLowerCase()))
                .filter(h -> !alreadyBooked.contains(h.getId()) && !exclude.contains(h.getId()))
                .limit(1).forEach(h -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "HOTEL"); rec.put("targetId", h.getId());
                    rec.put("title", h.getHotelName());
                    rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                    rec.put("reason", "You love " + dest + " — stay here next time");
                    rec.put("tag", "YOU MIGHT LIKE");
                    results.add(rec);
                });
        }

        // Collaborative
        Set<String> userDests = new HashSet<>(destCount.keySet());
        double finalAvg = avgSpend;

        userRepository.findAll().stream()
            .filter(u -> !u.getId().equals(user.getId()))
            .filter(u -> u.getBookings() != null && !u.getBookings().isEmpty())
            .filter(u -> u.getBookings().stream().anyMatch(b -> {
                if (!"Flight".equals(b.getType())) return false;
                Flight f = flightIndex.get(b.getBookingId());
                return f != null && userDests.contains(f.getTo());
            }))
            .limit(10)
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> !alreadyBooked.contains(b.getBookingId()) && !exclude.contains(b.getBookingId()))
            .filter(b -> b.getTotalPrice() > 0 && Math.abs(b.getTotalPrice() - finalAvg) < finalAvg * 0.5)
            .map(Users.Booking::getBookingId)
            .distinct().limit(2)
            .forEach(targetId -> {
                Flight f = flightIndex.get(targetId);
                if (f != null && !f.isTemplate()) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "FLIGHT"); rec.put("targetId", f.getId());
                    rec.put("title", f.getFlightName() + " · " + f.getFrom() + " → " + f.getTo());
                    rec.put("subtitle", "₹" + (long) f.getPrice());
                    rec.put("reason", "Travelers with similar taste booked this");
                    rec.put("tag", "TRAVELERS LIKE YOU");
                    results.add(rec);
                    return;
                }
                Hotel h = hotelIndex.get(targetId);
                if (h != null) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("type", "HOTEL"); rec.put("targetId", h.getId());
                    rec.put("title", h.getHotelName());
                    rec.put("subtitle", h.getLocation() + " · ₹" + (long) h.getPricePerNight() + "/night");
                    rec.put("reason", "Travelers with similar taste stayed here");
                    rec.put("tag", "TRAVELERS LIKE YOU");
                    results.add(rec);
                }
            });

        if (results.size() < 4) {
            Set<String> seen = results.stream().map(r -> (String) r.get("targetId")).collect(Collectors.toSet());
            seen.addAll(alreadyBooked); seen.addAll(exclude);
            List<Map<String, Object>> pad = coldStart(seen, flightIndex, hotelIndex);
            for (Map<String, Object> p : pad) {
                if (results.size() >= 4) break;
                results.add(p);
            }
        }

        return results;
    }
}

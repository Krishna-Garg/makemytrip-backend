package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Flight.Seat;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.models.Hotel.RoomType;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SeatService {

    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MongoTemplate mongoTemplate;

    // ── Aircraft model definitions ───────────────────────────
    // Each entry: { totalSeats, rows per class, seatsPerRow, class, color }
    private static final Map<String, List<SeatClassDef>> MODELS = Map.of(
        "AIR_ASIA", List.of(
            new SeatClassDef("BUSINESS", "#3B82F6", 3, 6, 3.0),   // rows 1-3,  18 seats
            new SeatClassDef("PREMIUM",  "#8B5CF6", 4, 6, 1.8),   // rows 4-7,  24 seats — but trimmed to 40 total
            new SeatClassDef("ECONOMY",  "#6B7280", 0, 6, 1.0)    // not used — AI Asia is 40 seat: biz+prem only
            // Air Asia 40 seats: 6 Business (rows 1), 14 Premium (rows 2-3), 20 Economy (rows 4-7) — 6+14+20=40
        ),
        "INTER_TRAVELS", List.of(
            new SeatClassDef("BUSINESS", "#3B82F6", 2, 6, 3.0),   // rows 1-2,  12 seats
            new SeatClassDef("PREMIUM",  "#8B5CF6", 3, 6, 1.8),   // rows 3-5,  18 seats
            new SeatClassDef("ECONOMY",  "#6B7280", 5, 6, 1.0)    // rows 6-10, 30 seats — total 60
        ),
        "WORLD_GUIDE", List.of(
            new SeatClassDef("BUSINESS", "#3B82F6", 3, 6, 3.0),   // rows 1-3,  18 seats
            new SeatClassDef("PREMIUM",  "#8B5CF6", 2, 6, 1.8)    // rows 4-5,  12 seats — total 30, no economy
        )
    );

    static class SeatClassDef {
        String seatClass, colorHex;
        int rows, seatsPerRow;
        double priceMultiplier;
        SeatClassDef(String c, String hex, int r, int s, double p) {
            seatClass = c; colorHex = hex; rows = r; seatsPerRow = s; priceMultiplier = p;
        }
    }

    // ── Generate seat map for a flight ──────────────────────
    public Flight generateSeatMap(String flightId, String aircraftModel) {
        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new RuntimeException("Flight not found"));

        List<SeatClassDef> defs = MODELS.get(aircraftModel);
        if (defs == null) throw new RuntimeException("Unknown aircraft model: " + aircraftModel);

        List<Seat> seats = new ArrayList<>();
        int row = 1;
        for (SeatClassDef def : defs) {
            if (def.rows == 0) continue;
            for (int r = 0; r < def.rows; r++) {
                for (int col = 0; col < def.seatsPerRow; col++) {
                    char letter = (char)('A' + col);
                    Seat seat = new Seat();
                    seat.setSeatNumber(row + "" + letter);
                    seat.setSeatClass(def.seatClass);
                    seat.setColorHex(def.colorHex);
                    seat.setPrice(Math.round(flight.getPrice() * def.priceMultiplier));
                    seat.setStatus("AVAILABLE");
                    seats.add(seat);
                }
                row++;
            }
        }

        flight.setAircraftModel(aircraftModel);
        flight.setSeats(seats);
        flight.setAvailableSeats(seats.size());
        return flightRepository.save(flight);
    }

    // ── Get seat map ─────────────────────────────────────────
    public Flight getSeatMap(String flightId) {
        return flightRepository.findById(flightId)
            .orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    // ── Lock seats (atomic conditional update) ───────────────
    public boolean lockSeats(String flightId, List<String> seatNumbers, String userId) {
        for (String seatNum : seatNumbers) {
            Query query = new Query(
                Criteria.where("_id").is(flightId)
                    .and("seats").elemMatch(
                        Criteria.where("seatNumber").is(seatNum)
                            .and("status").is("AVAILABLE")
                    )
            );
            Update update = new Update()
                .set("seats.$.status", "LOCKED")
                .set("seats.$.lockedByUserId", userId);
            var result = mongoTemplate.updateFirst(query, update, Flight.class);
            if (result.getModifiedCount() == 0) {
                unlockSeats(flightId, seatNumbers, userId);
                throw new RuntimeException("Seat " + seatNum + " is no longer available");
            }
        }
        return true;
    }

    // ── Confirm seats (LOCKED → BOOKED) ─────────────────────
    public void confirmSeats(String flightId, List<String> seatNumbers) {
        for (String seatNum : seatNumbers) {
            Query query = new Query(
                Criteria.where("_id").is(flightId)
                    .and("seats").elemMatch(
                        Criteria.where("seatNumber").is(seatNum)
                    )
            );
            Update update = new Update().set("seats.$.status", "BOOKED");
            mongoTemplate.updateFirst(query, update, Flight.class);
        }
    }

    // ── Unlock seats ─────────────────────────────────────────
    public void unlockSeats(String flightId, List<String> seatNumbers, String userId) {
        for (String seatNum : seatNumbers) {
            Query query = new Query(
                Criteria.where("_id").is(flightId)
                    .and("seats").elemMatch(
                        Criteria.where("seatNumber").is(seatNum)
                            .and("lockedByUserId").is(userId)
                    )
            );
            Update update = new Update()
                .set("seats.$.status", "AVAILABLE")
                .unset("seats.$.lockedByUserId");
            mongoTemplate.updateFirst(query, update, Flight.class);
        }
    }

    // ── Generate room types for hotel (Option B auto-split) ──
    public Hotel generateRoomTypes(String hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));

        if (!hotel.getRoomTypes().isEmpty()) return hotel; // already generated

        int total = hotel.getAvailableRooms();
        int standard = (int) Math.round(total * 0.60);
        int deluxe   = (int) Math.round(total * 0.30);
        int suite    = total - standard - deluxe;

        List<RoomType> roomTypes = new ArrayList<>();

        RoomType std = new RoomType();
        std.setType("STANDARD");
        std.setColorHex("#6B7280");
        std.setPriceMultiplier(1.0);
        std.setTotalRooms(standard);
        std.setAvailableRooms(standard);
        std.setDescription("Comfortable room with essential amenities, queen bed, city view.");
        std.setFloorplanType("STANDARD");
        roomTypes.add(std);

        RoomType dlx = new RoomType();
        dlx.setType("DELUXE");
        dlx.setColorHex("#8B5CF6");
        dlx.setPriceMultiplier(1.5);
        dlx.setTotalRooms(deluxe);
        dlx.setAvailableRooms(deluxe);
        dlx.setDescription("Spacious room with premium furnishings, king bed, and pool view.");
        dlx.setFloorplanType("DELUXE");
        roomTypes.add(dlx);

        RoomType ste = new RoomType();
        ste.setType("SUITE");
        ste.setColorHex("#3B82F6");
        ste.setPriceMultiplier(2.0);
        ste.setTotalRooms(suite);
        ste.setAvailableRooms(suite);
        ste.setDescription("Luxury suite with separate living area, jacuzzi, and panoramic view.");
        ste.setFloorplanType("SUITE");
        roomTypes.add(ste);

        hotel.setRoomTypes(roomTypes);
        return hotelRepository.save(hotel);
    }

    public Hotel getRoomTypes(String hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
        if (hotel.getRoomTypes().isEmpty()) return generateRoomTypes(hotelId);
        return hotel;
    }

    // ── Book a room type (decrement available) ───────────────
    public Hotel bookRoom(String hotelId, String roomType, int quantity) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
        hotel.getRoomTypes().stream()
            .filter(r -> r.getType().equals(roomType))
            .findFirst()
            .ifPresent(r -> {
                if (r.getAvailableRooms() < quantity)
                    throw new RuntimeException("Not enough " + roomType + " rooms available");
                r.setAvailableRooms(r.getAvailableRooms() - quantity);
            });
        return hotelRepository.save(hotel);
    }

    // ── Save user preferences ────────────────────────────────
    public Users savePreferences(String userId, String seatPref, String roomPref) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (seatPref != null) user.setSavedSeatPreference(seatPref);
        if (roomPref != null) user.setSavedRoomPreference(roomPref);
        return userRepository.save(user);
    }
}

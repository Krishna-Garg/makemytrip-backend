package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.models.Users.Booking;
import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.repositories.UserRepository;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private SeatService seatService;

    public Booking bookFlight(String userId, String flightId, int seats,
                               double price, List<String> selectedSeats) {
        Optional<Users> usersOptional = userRepository.findById(userId);
        Optional<Flight> flightOptional = flightRepository.findById(flightId);

        if (usersOptional.isPresent() && flightOptional.isPresent()) {
            Users user = usersOptional.get();
            Flight flight = flightOptional.get();

            // FIX: for seat-map flights, count truly AVAILABLE seats
            // not just the stored availableSeats number (which may include locked)
            int effectiveAvailable;
            if (flight.getSeats() != null && !flight.getSeats().isEmpty()) {
                effectiveAvailable = (int) flight.getSeats().stream()
                    .filter(s -> "AVAILABLE".equals(s.getStatus()))
                    .count();
            } else {
                effectiveAvailable = flight.getAvailableSeats();
            }

            if (effectiveAvailable < seats) {
                throw new RuntimeException("Not enough seats available");
            }

            // Decrement available seats count
            flight.setAvailableSeats(Math.max(0, flight.getAvailableSeats() - seats));
            flightRepository.save(flight);

            // Confirm locked seats → BOOKED in seat map
            if (selectedSeats != null && !selectedSeats.isEmpty()) {
                seatService.confirmSeats(flightId, selectedSeats);
            }

            Booking booking = new Booking();
            booking.setType("Flight");
            booking.setBookingId(flightId);
            booking.setDate(LocalDate.now().toString());
            booking.setQuantity(seats);
            booking.setTotalPrice(price);
            booking.setBookingStatus("CONFIRMED");
            booking.setDepartureTime(flight.getDepartureTime());
            if (selectedSeats != null) {
                booking.setSelectedSeats(selectedSeats);
            }

            user.getBookings().add(booking);
            userRepository.save(user);
            return booking;
        }
        throw new RuntimeException("User or flight not found");
    }

    public Booking bookhotel(String userId, String hotelId, int rooms,
                              double price, String selectedRoomType) {
        Optional<Users> usersOptional = userRepository.findById(userId);
        Optional<Hotel> hotelOptional = hotelRepository.findById(hotelId);

        if (usersOptional.isPresent() && hotelOptional.isPresent()) {
            Users user = usersOptional.get();
            Hotel hotel = hotelOptional.get();

            if (hotel.getAvailableRooms() < rooms) {
                throw new RuntimeException("Not enough rooms available");
            }

            hotel.setAvailableRooms(Math.max(0, hotel.getAvailableRooms() - rooms));
            hotelRepository.save(hotel);

            if (selectedRoomType != null && !selectedRoomType.isEmpty()) {
                seatService.bookRoom(hotelId, selectedRoomType, rooms);
            }

            Booking booking = new Booking();
            booking.setType("Hotel");
            booking.setBookingId(hotelId);
            booking.setDate(LocalDate.now().toString());
            booking.setQuantity(rooms);
            booking.setTotalPrice(price);
            booking.setBookingStatus("CONFIRMED");
            if (selectedRoomType != null) {
                booking.setSelectedRoomType(selectedRoomType);
            }

            user.getBookings().add(booking);
            userRepository.save(user);
            return booking;
        }
        throw new RuntimeException("User or hotel not found");
    }
}

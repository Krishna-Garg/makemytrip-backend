package com.makemytrip.makemytrip.controllers;
import com.makemytrip.makemytrip.services.CancellationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.models.Flight;
import com.makemytrip.makemytrip.models.Hotel;
import com.makemytrip.makemytrip.repositories.UserRepository;
import com.makemytrip.makemytrip.repositories.FlightRepository;
import com.makemytrip.makemytrip.repositories.HotelRepository;
import java.util.Map;
import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private CancellationService cancellationService;

    @GetMapping("/users")
    public ResponseEntity<List<Users>> getallusers(){
        List<Users> users=userRepository.findAll();
        return ResponseEntity.ok(users);
    }
    @PostMapping("/flight")
    public Flight addflight(@RequestBody Flight flight){
        return flightRepository.save(flight);
    }

    @PostMapping("/hotel")
    public Hotel addhotel(@RequestBody Hotel hotel){
        return hotelRepository.save(hotel);
    }
    @PutMapping("flight/{id}")
    public ResponseEntity<Flight> editflight(@PathVariable String id, @RequestBody Flight updatedFlight){
        Optional<Flight> flightOptional=flightRepository.findById(id);
        if(flightOptional.isPresent()){
            Flight flight = flightOptional.get();
            flight.setFlightName(updatedFlight.getFlightName());
            flight.setFrom(updatedFlight.getFrom());
            flight.setTo(updatedFlight.getTo());
            flight.setDepartureTime(updatedFlight.getDepartureTime());
            flight.setArrivalTime(updatedFlight.getArrivalTime());
            flight.setPrice(updatedFlight.getPrice());
            flight.setAvailableSeats(updatedFlight.getAvailableSeats());
            flight.setBoardingMinutes(updatedFlight.getBoardingMinutes());
            flightRepository.save(flight);
            return  ResponseEntity.ok(flight);
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("hotel/{id}")
    public ResponseEntity<Hotel> editHotel (@PathVariable String id, @RequestBody Hotel updatedHotel){
        Optional<Hotel> hotelOptional=hotelRepository.findById(id);
        if(hotelOptional.isPresent()){
            Hotel hotel = hotelOptional.get();
            hotel.setHotelName(updatedHotel.getHotelName());
            hotel.setLocation(updatedHotel.getLocation());
            hotel.setAvailableRooms(updatedHotel.getAvailableRooms());
            hotel.setPricePerNight(updatedHotel.getPricePerNight());
            hotel.setAmenities((updatedHotel.getAmenities()));
            hotelRepository.save(hotel);
            return ResponseEntity.ok(hotel);
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("/refund/status")
    public ResponseEntity<Users.Booking> updateRefundStatus(@RequestParam String userId, @RequestParam String bookingId, @RequestParam String status) {
        return ResponseEntity.ok(cancellationService.updateRefundStatus(userId, bookingId, status));
    }
    @GetMapping("/refunds")
    public ResponseEntity<List<Map<String, Object>>> getAllRefunds() {
        return ResponseEntity.ok(cancellationService.getAllCancelledBookings());
    }
}

package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServices {

    @Autowired private UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // FIX #3: throws instead of returning null
    // FIX #5: no System.out.println of hash
    public Users login(String email, String password) {
        Users user = userRepository.findAll().stream()
            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return user;
    }

    public Users signup(Users user) {
        // Check duplicate email
        boolean exists = userRepository.findAll().stream()
            .anyMatch(u -> user.getEmail().equalsIgnoreCase(u.getEmail()));
        if (exists) throw new RuntimeException("Email already registered");

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("USER");
        return userRepository.save(user);
    }

    public Users getuserbyemail(String email) {
        return userRepository.findAll().stream()
            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
            .findFirst()
            .orElse(null);
    }

    // FIX #3 (Quick Win): also updates email
    public Users editprofile(String id, String firstName, String lastName,
                              String email, String phoneNumber) {
        Users user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (firstName != null)   user.setFirstName(firstName);
        if (lastName != null)    user.setLastName(lastName);
        if (email != null)       user.setEmail(email);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        return userRepository.save(user);
    }
}

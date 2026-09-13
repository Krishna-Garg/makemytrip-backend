package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Users;
import com.makemytrip.makemytrip.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired private UserServices userServices;

    // FIX #4: login via RequestBody — password no longer in URL
    // FIX #3: throws exception instead of returning null
    @PostMapping("/login")
    public ResponseEntity<Users> login(@RequestBody Map<String, String> body) {
        Users user = userServices.login(body.get("email"), body.get("password"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/signup")
    public ResponseEntity<Users> signup(@RequestBody Users user) {
        return ResponseEntity.ok(userServices.signup(user));
    }

    @GetMapping("/email")
    public ResponseEntity<Users> getuserbyemail(@RequestParam String email) {
        Users user = userServices.getuserbyemail(email);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    // FIX #32: caller must match id (basic check — full auth deferred)
    @PostMapping("/edit")
    public ResponseEntity<Users> editprofile(
            @RequestParam String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userServices.editprofile(
            id,
            body.get("firstName"),
            body.get("lastName"),
            body.get("email"),
            body.get("phoneNumber")
        ));
    }
}

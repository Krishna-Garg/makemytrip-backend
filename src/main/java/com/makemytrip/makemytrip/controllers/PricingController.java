package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.PriceFreeze;
import com.makemytrip.makemytrip.models.PriceSnapshot;
import com.makemytrip.makemytrip.services.PricingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
@CrossOrigin(origins = "*")
public class PricingController {

    @Autowired private PricingEngine pricingEngine;

    // GET /pricing/{flightId}?userId=xxx
    @GetMapping("/{flightId}")
    public ResponseEntity<Map<String, Object>> getPrice(
            @PathVariable String flightId,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(pricingEngine.getEffectivePrice(flightId, userId));
    }

    // GET /pricing/{flightId}/history
    @GetMapping("/{flightId}/history")
    public ResponseEntity<List<PriceSnapshot>> getHistory(@PathVariable String flightId) {
        return ResponseEntity.ok(pricingEngine.getPriceHistory(flightId));
    }

    // POST /pricing/{flightId}/freeze?userId=xxx
    @PostMapping("/{flightId}/freeze")
    public ResponseEntity<PriceFreeze> freezePrice(
            @PathVariable String flightId,
            @RequestParam String userId) {
        return ResponseEntity.ok(pricingEngine.freezePrice(userId, flightId));
    }

    // GET /pricing/freezes?userId=xxx
    @GetMapping("/freezes")
    public ResponseEntity<List<PriceFreeze>> getUserFreezes(@RequestParam String userId) {
        return ResponseEntity.ok(pricingEngine.getUserFreezes(userId));
    }

    // GET /pricing/tier?userId=xxx
    @GetMapping("/tier")
    public ResponseEntity<Map<String, Object>> getUserTier(@RequestParam String userId) {
        String tier = pricingEngine.getUserTier(userId);
        double discount = pricingEngine.getTierDiscount(tier);
        int freezeMinutes = "PLATINUM".equals(tier) ? 120 : 30;
        return ResponseEntity.ok(Map.of(
            "tier", tier,
            "discount", (int)(discount * 100) + "%",
            "freezeMinutes", freezeMinutes,
            "nextTier", switch (tier) {
                case "BASIC" -> "3 qualifying bookings for SILVER";
                case "SILVER" -> "7 qualifying bookings for GOLD";
                case "GOLD" -> "15 qualifying bookings for PLATINUM";
                default -> "You are at the highest tier";
            }
        ));
    }
}

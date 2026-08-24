package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.services.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired private RecommendationService recommendationService;

    // GET /recommendations?userId=xxx
    // Returns cached recommendations (recomputes if >24h old)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }

    // POST /recommendations/refresh?userId=xxx
    // Forces immediate recomputation
    @PostMapping("/refresh")
    public ResponseEntity<List<Map<String, Object>>> refreshRecommendations(
            @RequestParam String userId) {
        return ResponseEntity.ok(recommendationService.refreshRecommendations(userId));
    }

    // POST /recommendations/feedback
    // Body: { userId, targetId, targetType, feedback: "HELPFUL"|"IRRELEVANT" }
    @PostMapping("/feedback")
    public ResponseEntity<Void> saveFeedback(@RequestBody Map<String, String> body) {
        recommendationService.saveFeedback(
            body.get("userId"),
            body.get("targetId"),
            body.get("targetType"),
            body.get("feedback")
        );
        return ResponseEntity.ok().build();
    }
}

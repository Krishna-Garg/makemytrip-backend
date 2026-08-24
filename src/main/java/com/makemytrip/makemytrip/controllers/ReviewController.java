package com.makemytrip.makemytrip.controllers;

import com.makemytrip.makemytrip.models.Review;
import com.makemytrip.makemytrip.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/{targetId}")
    public ResponseEntity<List<Review>> getReviews(@PathVariable String targetId, @RequestParam(defaultValue = "newest") String sort) {
        return ResponseEntity.ok(reviewService.getReviews(targetId, sort));
    }

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Map<String, Object> body) {
        Review review = reviewService.createReview(
                (String) body.get("targetId"),
                (String) body.get("targetType"),
                (String) body.get("userId"),
                (String) body.get("userFullName"),
                (Integer) body.get("rating"),
                (String) body.get("reviewText"),
                (String) body.getOrDefault("photoUrl", null)
        );

        return ResponseEntity.ok(review);
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<Review> addReply(@PathVariable String reviewId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reviewService.addReply(
                reviewId,
                body.get("userId"),
                body.get("userFullName"),
                body.get("text")
        ));
    }

    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<Review> markHelpful(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.markHelpFul(reviewId));
    }

    @PostMapping("/{reviewId}/flag")
    public ResponseEntity<Review> flagReview(@PathVariable String reviewId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reviewService.flagReview(reviewId, body.get("reason")));
    }

    @GetMapping("/admin/flagged")
    public ResponseEntity<List<Review>> getFlagged() {
        return ResponseEntity.ok(reviewService.getFlaggedReview());
    }

    @DeleteMapping("/admin/{reviewId}")
    public ResponseEntity<Void> deleteReviews(@PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/{reviewId}/unflag")
    public ResponseEntity<Review> unflag(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.unflagReview(reviewId));
    }
}

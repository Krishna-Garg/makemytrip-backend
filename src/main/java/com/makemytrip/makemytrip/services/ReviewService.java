package com.makemytrip.makemytrip.services;

import com.makemytrip.makemytrip.models.Review;
import com.makemytrip.makemytrip.models.Review.Reply;
import com.makemytrip.makemytrip.repositories.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public Review createReview(String targetId, String targetType, String userId, String userFullName, int rating, String reviewText, String photoUrl) {
        if(reviewRepository.existsByTargetIdAndUserId(targetId, userId)) {
            throw new RuntimeException("You have already reviewed this" + targetType + "!");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5!!!");
        }

        Review review = new Review();
        review.setTargetId(targetId);
        review.setTargetType(targetType.toUpperCase());
        review.setUserId(userId);
        review.setUserFullName(userFullName);
        review.setRating(rating);
        review.setReviewText(reviewText);
        review.setPhotoUrl(photoUrl);
        review.setCreatedAt(LocalDateTime.now().toString());

        return reviewRepository.save(review);
    }

    public List<Review> getReviews(String targetId, String sortBy) {
        List<Review> reviews = reviewRepository.findByTargetIdAndFlaggedFalse(targetId);

        switch (sortBy == null ? "newest" : sortBy) {
            case "highest" -> reviews.sort(Comparator.comparingInt(Review::getRating).reversed());

            case "lowest" -> reviews.sort(Comparator.comparingInt(Review::getRating));

            case "helpful" -> reviews.sort(Comparator.comparingInt(Review::getHelpfulCount).reversed());

            default -> reviews.sort(Comparator.comparing(Review::getCreatedAt).reversed());
        }
        return reviews;
    }

    public Review addReply(String reviewId, String userId, String userFullName, String text) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        Reply reply = new Reply();

        reply.setUserId(userId);
        reply.setUserFullName(userFullName);
        reply.setText(text);
        reply.setCreatedAt(LocalDateTime.now().toString());

        review.getReplies().add(reply);
        return reviewRepository.save(review);
    }

    public Review markHelpFul(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewRepository.save(review);
    }

    public Review flagReview(String reviewId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        review.setFlagged(true);
        review.setFlagReason(reason);
        return reviewRepository.save(review);
    }

    public List<Review> getFlaggedReview() {
        return reviewRepository.findByFlaggedTrue();
    }

    public void deleteReview(String reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public Review unflagReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        review.setFlagged(false);
        review.setFlagReason(null);
        return reviewRepository.save(review);
    }
}

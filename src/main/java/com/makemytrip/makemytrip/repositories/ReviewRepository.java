package com.makemytrip.makemytrip.repositories;

import com.makemytrip.makemytrip.models.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String>{
    List<Review> findByTargetIdAndFlaggedFalse(String targetId);

    List<Review> findByTargetId(String targetId);

    List<Review> findByFlaggedTrue();

    boolean existsByTargetIdAndUserId(String targetId, String userId);
}

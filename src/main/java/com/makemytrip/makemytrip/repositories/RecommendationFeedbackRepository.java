package com.makemytrip.makemytrip.repositories;

import com.makemytrip.makemytrip.models.RecommendationFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RecommendationFeedbackRepository extends MongoRepository<RecommendationFeedback, String> {
    List<RecommendationFeedback> findByUserId(String userId);
    boolean existsByUserIdAndTargetId(String userId, String targetId);
}

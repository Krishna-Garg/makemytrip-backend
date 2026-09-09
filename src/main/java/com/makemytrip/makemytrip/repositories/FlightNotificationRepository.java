// ═══════════════════════════════════════════════════════════
// repositories/FlightNotificationRepository.java
// ═══════════════════════════════════════════════════════════

package com.makemytrip.makemytrip.repositories;

import com.makemytrip.makemytrip.models.FlightNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FlightNotificationRepository extends MongoRepository<FlightNotification, String> {
    List<FlightNotification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<FlightNotification> findByUserIdAndReadFalse(String userId);
    long countByUserIdAndReadFalse(String userId);
}

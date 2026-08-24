package com.makemytrip.makemytrip.repositories;
 
import com.makemytrip.makemytrip.models.PriceFreeze;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
 
public interface PriceFreezeRepository extends MongoRepository<PriceFreeze, String> {
    Optional<PriceFreeze> findByUserIdAndFlightIdAndUsedFalse(String userId, String flightId);
    List<PriceFreeze> findByUserId(String userId);
}

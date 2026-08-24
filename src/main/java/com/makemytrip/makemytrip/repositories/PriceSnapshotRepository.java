package com.makemytrip.makemytrip.repositories;
 
import com.makemytrip.makemytrip.models.PriceSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
 
public interface PriceSnapshotRepository extends MongoRepository<PriceSnapshot, String> {
    List<PriceSnapshot> findByFlightIdOrderByTimestampAsc(String flightId);
}


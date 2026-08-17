package com.transit.reliability.repository;

import com.transit.reliability.model.TripUpdate;
import com.transit.reliability.model.TripUpdateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripUpdateRepository extends JpaRepository<TripUpdate, TripUpdateId> {
}

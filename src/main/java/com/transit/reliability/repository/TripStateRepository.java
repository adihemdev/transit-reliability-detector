package com.transit.reliability.repository;

import com.transit.reliability.model.TripState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripStateRepository extends JpaRepository<TripState, String> {
}

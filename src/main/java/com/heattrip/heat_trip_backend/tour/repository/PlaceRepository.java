package com.heattrip.heat_trip_backend.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.heattrip.heat_trip_backend.tour.domain.Place;

public interface PlaceRepository extends JpaRepository<Place, Long>{
    
}

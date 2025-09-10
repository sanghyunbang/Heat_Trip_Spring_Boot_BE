package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** places CRUD. contentid가 PK */
@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {}

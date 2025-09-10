package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.PlaceFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** place_features CRUD. link_id가 PK(=places.contentid) */
@Repository
public interface PlaceFeaturesRepository extends JpaRepository<PlaceFeatures, Long> {}

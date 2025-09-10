package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.PlaceFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** place_features 테이블 */
@Repository("curationPlaceFeaturesRepository") // explore와 충돌 방지
public interface CurationPlaceFeaturesRepository extends JpaRepository<PlaceFeatures, Long> { }

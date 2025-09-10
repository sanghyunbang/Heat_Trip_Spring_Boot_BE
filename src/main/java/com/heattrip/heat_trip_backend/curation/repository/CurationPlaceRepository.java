package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** places 테이블(curation 도메인) */
@Repository("curationPlaceRepository") // ← 빈 이름도 명시적으로 다르게
public interface CurationPlaceRepository extends JpaRepository<Place, Long> { }

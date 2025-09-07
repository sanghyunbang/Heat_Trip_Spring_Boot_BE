package com.heattrip.heat_trip_backend.bookmark.repository;

import com.heattrip.heat_trip_backend.tour.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceImgRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByContentid(Long contentid);            // (선택)
    List<Place> findByContentidIn(Collection<Long> contentids); // (배치용 선택, 없으면 findAllById 사용)
}

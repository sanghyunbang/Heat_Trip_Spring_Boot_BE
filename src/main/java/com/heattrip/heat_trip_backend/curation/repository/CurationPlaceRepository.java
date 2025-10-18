package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository("curationPlaceRepository")
public interface CurationPlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByCat3In(Collection<String> cat3Codes);
}

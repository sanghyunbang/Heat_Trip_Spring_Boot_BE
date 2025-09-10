package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.Cat3CategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface Cat3CategoryMappingRepository extends JpaRepository<Cat3CategoryMapping, Long> {
    List<Cat3CategoryMapping> findByCat3CodeIn(Set<String> codes);
}

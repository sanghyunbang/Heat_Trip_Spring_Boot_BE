package com.heattrip.heat_trip_backend.curation.repository;

import com.heattrip.heat_trip_backend.curation.entity.PlaceStarRating;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * place_star_ratings 접근
 * - 테이블 구조가 link_id 단일 PK라도, 안전하게 AVG/COUNT를 한 번에 뽑을 수 있게 집계 쿼리를 제공
 * - (행이 1개면 AVG=rating, COUNT=1)
 */
@Repository
public interface PlaceStarRatingRepository extends JpaRepository<PlaceStarRating, Long> {

    /** 집계 결과를 가볍게 받기 위한 인터페이스 프로젝션 */
    interface RatingAgg {
        Long getLinkId();
        Double getAvgRating();
        Long getNRows(); // 집계된 행 수(=1일 수도 있음)
    }

    @Query(value = """
        SELECT link_id   AS linkId,
               AVG(rating) AS avgRating,
               COUNT(*)    AS nRows
          FROM place_star_ratings
         WHERE link_id IN (:ids)
         GROUP BY link_id
        """, nativeQuery = true)
    List<RatingAgg> avgByLinkIds(@Param("ids") Collection<Long> ids);
}

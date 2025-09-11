package com.heattrip.heat_trip_backend.explore.adapter.jpa;

import com.heattrip.heat_trip_backend.explore.dto.PageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSummaryDto;
import com.heattrip.heat_trip_backend.explore.dto.search.PlaceSearchCond;
import com.heattrip.heat_trip_backend.explore.port.ExploreSearchPort;
import com.heattrip.heat_trip_backend.curation.entity.Cat3CategoryMapping;
import com.heattrip.heat_trip_backend.tour.domain.Place;
import com.heattrip.heat_trip_backend.tour.domain.PlaceTraitSnapshot;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JPA Criteria 기반 동적 검색 어댑터.
 *
 * 설계 요점
 *  - (저결합) Service는 Port(ExploreSearchPort)에만 의존하고, 실제 구현은 Adapter가 담당. ※A-1
 *  - (안정성) FK가 없는 테이블 결합은 JOIN 대신 상관 서브쿼리/EXISTS로 처리. ※A-2
 *  - (이식성) DB/엔진 교체 시 Adapter만 갈아끼우면 됨(JPA → ES 등). ※A-3
 */
@Component
public class ExploreSearchJpaAdapter implements ExploreSearchPort {

    private final EntityManager em;

    public ExploreSearchJpaAdapter(EntityManager em) {
        this.em = em;
    }

    // src/main/java/.../explore/adapter/jpa/ExploreSearchJpaAdapter.java
    @Override
    public PageResponse<PlaceSummaryDto> search(PlaceSearchCond c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Place> p = cq.from(Place.class);

        Subquery<String> cat3nameSq   = subSelectString(cq, cb, p, "cat3Name");
        Subquery<String> shortDesc1Sq = subSelectString(cq, cb, p, "shortDesc1");
        Subquery<String> shortDesc2Sq = subSelectString(cq, cb, p, "shortDesc2");

        List<Predicate> predicates = buildPredicates(cb, cq, p, c);

        cq.multiselect(
            p.get("contentid").alias("contentid"),
            p.get("title").alias("title"),
            p.get("firstimage").alias("firstimage"),
            p.get("areacode").alias("areacode"),
            p.get("sigungucode").alias("sigungucode"),
            p.get("createdtime").alias("createdtime"),
            p.get("addr1").alias("addr1"),
            p.get("addr2").alias("addr2"),
            p.get("firstimage2").alias("firstimage2"),
            p.get("contenttypeid").alias("contenttypeid"),
            p.get("cat3").alias("cat3"),
            cat3nameSq.getSelection().alias("cat3Name"),
            shortDesc1Sq.getSelection().alias("shortDesc1"),
            shortDesc2Sq.getSelection().alias("shortDesc2")
        ).where(predicates.toArray(Predicate[]::new));

        cq.orderBy(buildOrder(cb, p, c.sort()));

        TypedQuery<Tuple> query = em.createQuery(cq)
            .setFirstResult(c.offset())
            .setMaxResults(c.limit());

        List<Tuple> tuples = query.getResultList();

        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Place> p2 = countCq.from(Place.class);
        List<Predicate> predsForCount = buildPredicates(cb, countCq, p2, c);
        countCq.select(cb.count(p2)).where(predsForCount.toArray(Predicate[]::new));
        long total = em.createQuery(countCq).getSingleResult();

        List<PlaceSummaryDto> content = tuples.stream().map(t -> {
            Long contentid = t.get("contentid", Long.class);
            String title = t.get("title", String.class);
            String firstimage = t.get("firstimage", String.class);
            Integer areacode = t.get("areacode", Integer.class);
            Integer sigungucode = t.get("sigungucode", Integer.class);
            java.time.LocalDateTime createdtime = t.get("createdtime", java.time.LocalDateTime.class);

            PlaceSummaryDto dto = new PlaceSummaryDto(
                contentid, title, firstimage, areacode, sigungucode, createdtime
            );
            dto.setAddr1(t.get("addr1", String.class));
            dto.setAddr2(t.get("addr2", String.class));
            dto.setFirstimage2(t.get("firstimage2", String.class));
            dto.setCat3(t.get("cat3", String.class));
            dto.setCat3Name(t.get("cat3Name", String.class));
            dto.setShortDesc1(t.get("shortDesc1", String.class));
            dto.setShortDesc2(t.get("shortDesc2", String.class));
            dto.setContentTypeId(parseIntOrNull(t.get("contenttypeid", String.class)));

            dto.setHashtags(java.util.Collections.emptyList());
            dto.setSimpleTags(java.util.Collections.emptyList());
            return dto;
        }).toList();

        int page = (c.page() == null) ? 0 : c.page();
        int size = (c.size() == null) ? 20 : c.size();
        boolean last = ((page + 1) * size) >= total;

        // ✅ PageResponse 시그니처 통일
        return new PageResponse<>(content, page, size, total, last);
    }


    // ─────────────────────────────────────────────
    // 헬퍼: WHERE 동적 구성
    // ─────────────────────────────────────────────
    private <T> List<Predicate> buildPredicates(
            CriteriaBuilder cb, CriteriaQuery<T> cq, Root<Place> p, PlaceSearchCond c
    ) {
        List<Predicate> preds = new ArrayList<>();

        // q: title/cat1/cat2/cat3 + cat3Name LIKE(EXISTS) ※D-1
        if (c.q() != null && !c.q().isBlank()) {
            String like = "%" + c.q().trim().toLowerCase() + "%";

            // PlaceTraitSnapshot 에서 cat3name LIKE 확인 (EXISTS 상관 서브쿼리)
            Subquery<Integer> nameExists = cq.subquery(Integer.class);
            Root<PlaceTraitSnapshot> t = nameExists.from(PlaceTraitSnapshot.class);
            nameExists.select(cb.literal(1))
                    .where(
                            cb.equal(t.get("cat3"), p.get("cat3")),
                            cb.like(cb.lower(t.get("cat3Name")), like)
                    );

            preds.add(cb.or(
                    cb.like(cb.lower(p.get("title")), like),
                    cb.like(cb.lower(p.get("cat1")), like),
                    cb.like(cb.lower(p.get("cat2")), like),
                    cb.like(cb.lower(p.get("cat3")), like),
                    cb.exists(nameExists)
            ));
        }

        // contentTypeId (DB는 VARCHAR → 문자열 비교가 안전) ※D-2
        if (c.contentTypeId() != null) {
            preds.add(cb.equal(p.get("contenttypeid"), String.valueOf(c.contentTypeId())));
        }

        // cat3 IN
        if (c.cat3() != null && !c.cat3().isEmpty()) {
            preds.add(p.get("cat3").in(c.cat3()));
        }

        // emotionCategoryId → cat3_category_mapping EXISTS ※D-3
        if (c.emotionCategoryId() != null) {
            Subquery<Integer> mapExists = cq.subquery(Integer.class);
            Root<Cat3CategoryMapping> m = mapExists.from(Cat3CategoryMapping.class);
            mapExists.select(cb.literal(1))
                    .where(
                            cb.equal(m.get("categoryId"), c.emotionCategoryId()),
                            cb.equal(m.get("cat3Code"), p.get("cat3"))
                    );
            preds.add(cb.exists(mapExists));
        }

        return preds;
    }

    // ─────────────────────────────────────────────
    // 헬퍼: 상관 스칼라 서브쿼리 (String 컬럼 1개)
    // ─────────────────────────────────────────────
    private Subquery<String> subSelectString(
            CriteriaQuery<?> parent, CriteriaBuilder cb, Root<Place> p, String column
    ) {
        Subquery<String> sq = parent.subquery(String.class);
        Root<PlaceTraitSnapshot> t = sq.from(PlaceTraitSnapshot.class);
        sq.select(t.get(column)).where(cb.equal(t.get("cat3"), p.get("cat3")));
        return sq;
    }

    // ─────────────────────────────────────────────
    // 헬퍼: 정렬 (Order 반환)
    // ─────────────────────────────────────────────
    private Order buildOrder(CriteriaBuilder cb, Root<Place> p, String sort) {
        if (sort == null) sort = "";
        return switch (sort) {
            case "title"  -> cb.asc(p.get("title"));
            case "recent" -> cb.desc(p.get("modifiedtime"));   // 최신 수정 시간
            case "random" -> cb.asc(cb.function("RAND", Double.class)); // ★ Order 로 래핑
            default       -> cb.desc(p.get("contentid"));      // 안전 기본값
        };
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
}

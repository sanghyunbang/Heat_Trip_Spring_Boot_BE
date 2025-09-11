package com.heattrip.heat_trip_backend.schedules.repository;

import com.heattrip.heat_trip_backend.schedules.entity.Journey;
import com.heattrip.heat_trip_backend.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

@Repository
@Primary // ⬅ 기존 JourneyRepository와 공존한다면 v2를 우선 주입하도록 설정(선택)
public interface JourneyRepository_v2 extends JpaRepository<Journey, Long> {

    /** 유저의 전체 여정 */
    List<Journey> findByUser(User user);

    /** 필요 시 N+1 방지를 위해 스케줄을 즉시 로딩하는 변형(옵션) */
    @EntityGraph(attributePaths = {"schedule"})
    List<Journey> findWithScheduleByUser(User user);
    int countBySchedule_ScheduleId(Integer scheduleId);
}

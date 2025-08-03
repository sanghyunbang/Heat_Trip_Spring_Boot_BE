package com.heattrip.heat_trip_backend.schedules.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.heattrip.heat_trip_backend.schedules.entity.*;

public interface ScheduleRepository extends JpaRepository<Schedule, String>{

}
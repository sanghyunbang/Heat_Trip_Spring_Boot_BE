package com.heattrip.heat_trip_backend.schedules.Service;

import org.springframework.stereotype.Service;

import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.schedules.repository.ScheduleRepository;
import java.util.List;

@Service
public class ScheduleService {
    private final ScheduleRepository schedulerepository;

    public ScheduleService(ScheduleRepository schedulerepository){
        this.schedulerepository = schedulerepository;
    }

    public List<Schedule> findAll() {
        return schedulerepository.findAll();
    }
    public Schedule save(Schedule schedule) {
    return schedulerepository.save(schedule);
}

}

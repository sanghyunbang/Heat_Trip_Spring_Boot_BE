package com.heattrip.heat_trip_backend.schedules.Service;

import org.springframework.stereotype.Service;

import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.schedules.repository.ScheduleRepository;
import com.heattrip.heat_trip_backend.user.entity.*;
import com.heattrip.heat_trip_backend.user.repository.*;
import java.util.List;

@Service
public class ScheduleService {
    private final ScheduleRepository schedulerepository;
    private final UserRepository repository;

    public ScheduleService(ScheduleRepository schedulerepository, UserRepository repository){
        this.schedulerepository = schedulerepository;
        this.repository = repository;
    }

    public List<Schedule> findAll() {
        return schedulerepository.findAll();
    }
    public Schedule save(Schedule schedule) {
    return schedulerepository.save(schedule);
    }
    public List<User> userinfo(){
        return repository.findAll();
    }
    public List<Schedule> findByUser(User user) {
    return schedulerepository.findByUser(user);
}

}

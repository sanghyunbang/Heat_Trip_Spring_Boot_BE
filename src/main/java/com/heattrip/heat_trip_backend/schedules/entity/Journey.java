package com.heattrip.heat_trip_backend.schedules.entity;
import com.heattrip.heat_trip_backend.user.entity.User;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    private String title;
    private LocalDate date;
    private String location;
    private String weatherLabel;
    private String moodLabel;
    private String body;

    @ElementCollection
    @CollectionTable(name = "journey_photos", joinColumns = @JoinColumn(name = "journey_id"))
    @Column(name = "photo_url")
    private List<String> photos;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



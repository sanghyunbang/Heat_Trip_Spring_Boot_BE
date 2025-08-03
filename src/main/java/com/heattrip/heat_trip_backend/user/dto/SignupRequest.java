package com.heattrip.heat_trip_backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;
    private String name;
    private String gender;
}

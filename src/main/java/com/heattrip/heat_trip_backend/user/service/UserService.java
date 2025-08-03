package com.heattrip.heat_trip_backend.user.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.user.dto.LoginRequest;
import com.heattrip.heat_trip_backend.user.dto.SignupRequest;
import com.heattrip.heat_trip_backend.user.entity.User;
import com.heattrip.heat_trip_backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 이건 뭐지?
    private final JWTProvider jwtProvider;

    public void signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User.Gender gender = switch (request.getGender().toLowerCase()) {
            case "male" -> User.Gender.MALE;
            case "female" -> User.Gender.FEMALE;
            default -> User.Gender.OTHER;
        };

        User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .nickname(request.getNickname())
                    .name(request.getName())
                    .gender(gender)
                    .createdAt(LocalDateTime.now())
                    .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request) throws IllegalAccessException {
        User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalAccessException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalAccessException("비밀번호가 일치하지 않습니다");
        }

        // userId에 email을 넣음(회원 가입된 회원의 경우)
        return jwtProvider.createAccessToken(user.getEmail(), "ROLE_USER");

    }
}

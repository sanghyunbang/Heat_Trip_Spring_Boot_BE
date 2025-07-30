package com.heattrip.heat_trip_backend.config;

import java.security.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.heattrip.heat_trip_backend.OAuth.handler.OAuth2SuccessHandler;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTAuthenticationFilter;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.OAuth.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

// 기능 : Sping Security 설정을 구성하며, OAuth2 로그인 처리 및 JWT 필터 등록을 포함


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTProvider jwtProvider; //JWT 유틸리티 주입
    private final CustomOAuth2UserService customOAuth2UserService; // 사용자 정의 OAuth2 서비스 주입
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // OAuth2 로그인 성공 핸들러 주입
    
    // Spring Security 필터 체인을 구성하는 메서드
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (세션 기반이 아니라 JWT기반의 REST API 사용, 브라우저가 아닌 앱 사용)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 생성하지 않음 (JWT 인증 방식이므로)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/oauth2/**", "/login/**", "/public/**").permitAll() // 인증 없이 접근 허용
                .anyRequest().authenticated()// 나머지 요청은 인증 필요
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)) // 사용자 정의 OAuth2 서비스 설정
                .successHandler(oAuth2SuccessHandler) // OAuth2 로그인 성공 핸들러 설정
            );

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
        http.addFilterBefore(new JWTAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        
        return http.build(); // 설정된 SecurityFilterChain 반환
    }
}


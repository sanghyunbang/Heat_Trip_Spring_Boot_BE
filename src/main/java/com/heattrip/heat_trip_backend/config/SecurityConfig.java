package com.heattrip.heat_trip_backend.config;

import java.security.Security;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.heattrip.heat_trip_backend.OAuth.handler.OAuth2SuccessHandler;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTAuthenticationFilter;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.OAuth.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

// 기능 : Sping Security 설정을 구성하며, OAuth2 로그인 처리 및 JWT 필터 등록을 포함


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // 비밀번호 암호화용 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 인증 매니저 등록 (일반 로그인 시 사용됨)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private final JWTProvider jwtProvider; //JWT 유틸리티 주입
    private final CustomOAuth2UserService customOAuth2UserService; // 사용자 정의 OAuth2 서비스 주입
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // OAuth2 로그인 성공 핸들러 주입
    
    // Spring Security 필터 체인을 구성하는 메서드
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (세션 기반이 아니라 JWT기반의 REST API 사용, 브라우저가 아닌 앱 사용)
            .formLogin(login->login.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 생성하지 않음 (JWT 인증 방식이므로)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/oauth2/**", "/login/**", "/public/**", "/auth/**", "/error").permitAll() // 인증 없이 접근 허용
                .anyRequest().authenticated()// 나머지 요청은 인증 필요
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)) // 사용자 정보 파싱을 내가 만들어놓은 서비스 클래스로
                .successHandler(oAuth2SuccessHandler) // OAuth2 로그인 성공 핸들러 설정
            );
            // endpoint는 .oauth2Login에서 .authoizationEndpoint(auth->auth.baseUri("")) 이런식으로 따로 커스텀을 안했음
            // 그러면 default형식 /oauth2/authorization/{provider} 형식으로 됨 -> 플러터에서 *******social_login_service******* 와 일치.

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
        http.addFilterBefore(new JWTAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        
        return http.build(); // 설정된 SecurityFilterChain 반환
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:8080", "http://10.0.2.2:8080")); // Flutter 앱에서 요청 허용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 인증정보 포함 가능 (예: 쿠키)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}


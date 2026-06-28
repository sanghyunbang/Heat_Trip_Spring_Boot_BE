package com.heattrip.heat_trip_backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러의 수동 JWT 파싱을 제거하고 SecurityFilterChain + @CurrentUser로 일원화한 뒤,
 * 보호된 엔드포인트가 토큰 없이/잘못된 토큰으로 접근 시 (302 리다이렉트가 아니라) 401 JSON을
 * 반환하는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("토큰 없이 보호 엔드포인트 접근 → 401 JSON")
    void protectedEndpoint_withoutToken_returns401Json() throws Exception {
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("잘못된 토큰으로 보호 엔드포인트 접근 → 401")
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/bookmarks").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("공개 엔드포인트(/api/explore/**)는 토큰 없이도 접근 가능")
    void publicEndpoint_withoutToken_isNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/explore/places/search"))
                .andExpect(status().isOk());
    }
}

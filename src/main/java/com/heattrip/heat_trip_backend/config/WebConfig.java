package com.heattrip.heat_trip_backend.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.heattrip.heat_trip_backend.auth.CurrentUserArgumentResolver;

import lombok.RequiredArgsConstructor;

/**
 * Spring MVC 커스터마이징. {@link CurrentUserArgumentResolver}를 등록해
 * 컨트롤러에서 {@code @CurrentUser}를 사용할 수 있게 한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}

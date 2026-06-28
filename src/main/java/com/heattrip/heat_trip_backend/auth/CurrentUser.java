package com.heattrip.heat_trip_backend.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 인증된 사용자를 주입한다.
 *
 * <p>JWTAuthenticationFilter가 SecurityContext에 심어둔 인증 주체(JWT subject)를 기반으로,
 * {@link CurrentUserArgumentResolver}가 값을 채운다.
 *
 * <ul>
 *   <li>{@code @CurrentUser User user} → subject로 조회한 사용자 엔티티</li>
 *   <li>{@code @CurrentUser String email} → JWT subject 문자열(이메일/식별자) 그대로</li>
 * </ul>
 *
 * 이 애너테이션을 쓰면 컨트롤러에서 Authorization 헤더를 직접 파싱할 필요가 없다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}

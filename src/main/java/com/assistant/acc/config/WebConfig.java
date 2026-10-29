package com.assistant.acc.config;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.assistant.acc.security.JwtTokenProvider;
import com.assistant.acc.security.JwtAuthFilter;
import com.assistant.acc.mapper.member.MemberMapper;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import com.assistant.acc.config.web.DemoGuardInterceptor;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    // 생성 이미지는 /files/{토큰} (PublicFileController) 으로만 내보낸다.
    // 예전 공개 경로 /poster-images/**, /static/** 는 회원번호·프로젝트 번호가 드러나고 추측으로 남의 파일을 열 수 있어 없앴다.

    // 로그인 없이 접근 가능한 경로
    private static final String[] PUBLIC_PATHS = {
            "/error",   // 오류 디스패치가 인증에 막히면 403/409 등이 모두 401 로 바뀐다
            "/api/auth/config",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/test-login",
            "/api/auth/oauth/**",
            "/files/**",   // 브라우저용 파일 주소 (<img src> 라 토큰 없이. 주소 자체가 추측 불가)
            "/internal/**" // AI 서버 전용. 로그인 대신 INTERNAL_TOKEN 을 확인한다 (InternalFileController). nginx 는 중계하지 않음
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokens,
            MemberMapper memberMapper) throws Exception {
        log.info("🔐 JWT 인증 모드로 Spring Security 설정");

        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/test/**").hasAnyRole("TEST", "ADMIN")
                        // 화면에서 쓰지 않는 예전 유료 생성 API (생성은 /api/create-jobs, /api/editor/build-jobs 로). 크레딧 차감이 없어 관리자만
                        .requestMatchers("/api/generate-prompt", "/api/create-image", "/api/analyze/**", "/api/liveposter/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(tokens, memberMapper), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다."))
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 콘솔 등 허용 시 필요

        log.info("✅ Security 설정 완료 (공개 경로 외 로그인 필요)");
        return http.build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DemoGuardInterceptor()).addPathPatterns("/api/**");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOriginPatterns));
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Location", "Content-Disposition"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
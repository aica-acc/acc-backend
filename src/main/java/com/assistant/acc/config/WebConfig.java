package com.assistant.acc.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String imagePath = "file:///C:/final_project/ACC/acc-ai/홍보물/";

        log.info("🚀 정적 이미지 리소스 핸들러 등록: /poster-images/** -> " + imagePath);

        registry.addResourceHandler("/poster-images/**") // (웹 주소: /poster-images/)
                .addResourceLocations(imagePath);        // (실제 경로: C:/.../홍보물/
    }

    private static final String[] AUTH_WHITELIST = {
            "/**"   // ✅ 모든 요청 허용
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🚀 개발용: Spring Security 완전 허용 모드 시작");

        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().permitAll()   // ✅ 모든 요청 허용
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "권한이 없습니다."))
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 콘솔 등 허용 시 필요

        log.info("✅ 개발용 Security 설정 완료 (모든 요청 허용)");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
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
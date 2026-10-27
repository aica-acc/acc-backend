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

    // 포스터 아카이브 이미지 폴더 (상대경로면 실행 위치 기준)
    @Value("${files.poster-images-dir:./홍보물}")
    private String posterImagesDir;

    @Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String imagePath = java.nio.file.Paths.get(posterImagesDir).toAbsolutePath().toUri().toString();
        if (!imagePath.endsWith("/")) {
            imagePath += "/";
        }

        log.info("🚀 정적 이미지 리소스 핸들러 등록: /poster-images/** -> " + imagePath);

        registry.addResourceHandler("/poster-images/**") // (웹 주소: /poster-images/)
                .addResourceLocations(imagePath);        // (실제 경로: C:/.../홍보물/
    }

    // 로그인 없이 접근 가능한 경로
    private static final String[] PUBLIC_PATHS = {
            "/error",   // 오류 디스패치가 인증에 막히면 403/409 등이 모두 401 로 바뀐다
            "/api/auth/config",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/test-login",
            "/poster-images/**",
            "/static/**"   // AI 서버 정적 파일 중계 (AiStaticProxyController)
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
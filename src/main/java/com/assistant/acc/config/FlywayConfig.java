package com.assistant.acc.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DB 마이그레이션 기본 동작.
 * application.properties 를 팀원마다 따로 쓰는 프로젝트라, 필수 설정은 설정파일에 의존하지 않고 코드로 보장한다.
 *
 *  - baselineOnMigrate=true, baselineVersion=0
 *    이미 테이블이 있는데 flyway_schema_history 가 없는 기존 DB 도 오류 없이 V1 부터 적용한다.
 *    (V1~V3 는 모두 여러 번 실행해도 안전하게 작성되어 있다.)
 *
 * 끄려면 환경변수 SPRING_FLYWAY_ENABLED=false (또는 spring.flyway.enabled=false).
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayDefaults() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("0");
    }
}

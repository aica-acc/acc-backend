package com.assistant.acc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 백그라운드 생성 작업 실행기.
 * 작업 하나가 외부 AI API 를 여러 번 부르며 수 분 걸린다. 동시에 너무 많이 돌면 비용과 AI 서버 부하가 커지므로 작게 둔다.
 */
@Configuration
public class JobExecutorConfig {

    @Bean(name = "jobExecutor")
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("job-");
        executor.initialize();
        return executor;
    }
}

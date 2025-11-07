package com.assistant.acc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
    excludeName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
    }
)
// 👇 배너 컨트롤러와 배너 서비스 패키지까지 스캔
@ComponentScan(basePackages = {
    "com.assistant.acc.controller.banner",
    "com.assistant.acc.service.banner"
})
public class BannerOnlyApp {
    public static void main(String[] args) {
        SpringApplication.run(BannerOnlyApp.class, args);
    }
}

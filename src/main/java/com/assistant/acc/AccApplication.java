package com.assistant.acc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.assistant.acc.mapper")
public class AccApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccApplication.class, args);
	}

}

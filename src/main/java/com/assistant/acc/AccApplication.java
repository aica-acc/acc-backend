package com.assistant.acc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
    "com.assistant.acc.mapper",     // 기존 mapper들 (ProjectMapper 등)
    "com.assistant.acc.utility"     // FileSave용 ProposalMapper 추가
})
public class AccApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccApplication.class, args);
	}

}

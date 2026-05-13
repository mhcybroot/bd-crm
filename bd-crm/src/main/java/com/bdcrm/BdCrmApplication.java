package com.bdcrm;

import com.bdcrm.config.CrmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CrmProperties.class)
public class BdCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(BdCrmApplication.class, args);
	}

}

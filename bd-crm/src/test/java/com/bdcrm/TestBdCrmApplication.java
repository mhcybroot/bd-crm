package com.bdcrm;

import org.springframework.boot.SpringApplication;

public class TestBdCrmApplication {

	public static void main(String[] args) {
		SpringApplication.from(BdCrmApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

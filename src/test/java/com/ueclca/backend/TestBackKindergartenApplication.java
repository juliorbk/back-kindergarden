package com.ueclca.backend;

import org.springframework.boot.SpringApplication;

public class TestBackKindergartenApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackKindergartenApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

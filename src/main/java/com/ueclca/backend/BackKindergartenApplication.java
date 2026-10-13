package com.ueclca.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackKindergartenApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackKindergartenApplication.class, args);
    }

}

package com.lingua.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LearningApp {

    public static void main(String[] args) {
        SpringApplication.run(LearningApp.class, args);
    }
}

package com.camplog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CampLogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampLogApplication.class, args);
    }
}

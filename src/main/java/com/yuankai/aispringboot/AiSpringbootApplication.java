package com.yuankai.aispringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AiSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpringbootApplication.class, args);
    }

}

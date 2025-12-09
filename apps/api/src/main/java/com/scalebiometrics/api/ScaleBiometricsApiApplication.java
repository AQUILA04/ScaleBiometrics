package com.scalebiometrics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ScaleBiometricsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaleBiometricsApiApplication.class, args);
    }
}

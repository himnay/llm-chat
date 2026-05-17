package com.infiproton.springaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        // Set the default timezone to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(SpringAiDemoApplication.class, args);
    }

}

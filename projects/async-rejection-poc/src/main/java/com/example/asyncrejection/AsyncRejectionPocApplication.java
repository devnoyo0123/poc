package com.example.asyncrejection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AsyncRejectionPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncRejectionPocApplication.class, args);
    }
}

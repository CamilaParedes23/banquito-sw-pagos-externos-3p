package com.banquito.switchpagos.externalpayments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExternalPaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExternalPaymentsApplication.class, args);
    }
}

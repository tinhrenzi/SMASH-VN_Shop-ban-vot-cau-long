package com.smashvn.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
public class SmashVnApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmashVnApplication.class, args);
    }

}

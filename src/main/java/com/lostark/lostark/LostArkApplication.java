package com.lostark.lostark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class LostArkApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostArkApplication.class, args);
    }

}

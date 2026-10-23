package com.nlizzard.horizonhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.nlizzard.horizonhub.mappers")
@EnableTransactionManagement
public class HorizonHubWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(HorizonHubWebApplication.class, args);
    }
}
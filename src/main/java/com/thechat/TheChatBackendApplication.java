package com.thechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TheChatBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheChatBackendApplication.class, args);
    }
}

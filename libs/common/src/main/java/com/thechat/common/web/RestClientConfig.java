package com.thechat.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot 4 split autoconfiguration into many small per-feature modules;
 * RestClient.Builder auto-configuration is no longer pulled in transitively by
 * spring-boot-starter-web in this project's dependency set. RestClient.builder()
 * needs no autoconfiguration magic — it's a plain static factory — so we just
 * declare the bean ourselves, shared by every service that makes outbound HTTP
 * calls to another service (Auth -> User, Chat -> User).
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

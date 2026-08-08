package com.thechat.security;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * RestClient interceptor that stamps every outgoing internal HTTP call with a service token
 * (Phase 5). Wire it into any RestClient.Builder that talks to another service's /internal/**
 * routes, e.g.:
 *
 *   restClientBuilder.baseUrl(userServiceBaseUrl)
 *                     .requestInterceptor(serviceAuthRequestInterceptor)
 *                     .build();
 */
@Component
public class ServiceAuthRequestInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceTokenIssuer serviceTokenIssuer;

    public ServiceAuthRequestInterceptor(ServiceTokenIssuer serviceTokenIssuer) {
        this.serviceTokenIssuer = serviceTokenIssuer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().setBearerAuth(serviceTokenIssuer.issue());
        return execution.execute(request, body);
    }
}

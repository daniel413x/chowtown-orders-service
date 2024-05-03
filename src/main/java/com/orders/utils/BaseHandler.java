package com.orders.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public abstract class BaseHandler {

    protected WebClient webClient;

    protected void initializeWebClient(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<ServerResponse> handleResponseError(Throwable e) {
        if (e instanceof WebClientResponseException ex) {
            return ServerResponse
                    .status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ex.getResponseBodyAsString());
        } else {
            return ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyValue("Internal Server Error: " + e.getMessage());
        }
    }
}

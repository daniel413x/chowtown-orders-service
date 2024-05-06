package com.orders.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public abstract class BaseHandler {

    protected WebClient webClient;
    private ReactiveJwtDecoder jwtDecoder;

    protected void initializeBaseHandler(String baseUrl, ReactiveJwtDecoder jwtDecoder) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.jwtDecoder = jwtDecoder;
    }

    public Mono<String> getAuth0IdFromToken(String authorizationHeader) {
        String tokenValue = authorizationHeader.replace("Bearer ", "");
        return jwtDecoder.decode(tokenValue)
                .map(j -> j.getClaimAsString("sub"));
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

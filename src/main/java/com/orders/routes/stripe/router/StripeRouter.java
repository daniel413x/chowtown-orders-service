package com.orders.routes.stripe.router;

import com.orders.routes.stripe.handlers.StripeRoutesHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class StripeRouter {

    @Bean
    public RouterFunction<ServerResponse> stripeRouterRoutes(StripeRoutesHandler stripeRoutesHandler) {
        return RouterFunctions
                .route()
                .nest(RequestPredicates.path("/api/orders"), builder -> {
                    builder.POST("/create-checkout-session", stripeRoutesHandler::createCheckoutSession);
                    builder.POST("/stripe-checkout-webhook", stripeRoutesHandler::stripeCheckoutWebhook);
                })
                .build();
    }
}

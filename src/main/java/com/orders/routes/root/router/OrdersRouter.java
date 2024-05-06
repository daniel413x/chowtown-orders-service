package com.orders.routes.root.router;

import com.orders.routes.root.handlers.OrdersRoutesHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class OrdersRouter {

    @Bean
    public RouterFunction<ServerResponse> orderRouterRoutes(OrdersRoutesHandler ordersRoutesHandler) {
        return RouterFunctions
                .route()
                .nest(RequestPredicates.path("/api/orders"), builder -> {
                    builder.GET("/user", ordersRoutesHandler::getUserOrders);
                })
                .build();
    }
}

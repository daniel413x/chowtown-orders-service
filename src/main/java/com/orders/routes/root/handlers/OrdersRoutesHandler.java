package com.orders.routes.root.handlers;

import com.orders.model.Order;
import com.orders.routes.root.dto.OrderPATCHReq;
import com.orders.routes.root.dto.OrdersGETReq;
import com.orders.routes.root.dto.Restaurant;
import com.orders.routes.root.repository.OrdersRepository;
import com.orders.utils.BaseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class OrdersRoutesHandler extends BaseHandler {

    private OrdersRepository ordersRepository;

    @Autowired
    public OrdersRoutesHandler(OrdersRepository ordersRepository, ReactiveJwtDecoder jwtDecoder) {
        this.ordersRepository = ordersRepository;
        initializeBaseHandler(System.getenv("RESTAURANT_SVC_ADDRESS"), jwtDecoder);
    }

    public OrdersRoutesHandler() {
    }

    public Mono<ServerResponse> getRestaurantOrders(ServerRequest req) {
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return this.getAuth0IdFromToken(authorizationHeader)
                .flatMap(auth0Id -> {
                    // get the owner's restaurant
                    return this.webClient.get()
                            .uri("/cms" + "/" + auth0Id)
                            .header("Authorization", authorizationHeader)
                            .retrieve()
                            .bodyToMono(Restaurant.class)
                            .flatMap(restaurant -> {
                                // paginated query for all orders where
                                // fetched restaurant id matches
                                PageRequest pageRequest = this.getPageRequest(req);
                                Flux<Order> ordersFlux = ordersRepository.findAllByRestaurantId(restaurant.getId(), pageRequest);
                                Mono<Long> count = ordersRepository.findAllByRestaurantId(restaurant.getId(), Pageable.unpaged()).count();
                                return ordersFlux.collectList()
                                        .zipWith(count, (list, cnt) -> OrdersGETReq.fromOrders(list, pageRequest, cnt))
                                        .flatMap(dtoMono -> dtoMono)
                                        .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(dto));
                            });
                });
    }

    public Mono<ServerResponse> getUserOrders(ServerRequest req) {
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return this.getAuth0IdFromToken(authorizationHeader)
                .flatMap(auth0Id -> {
                    PageRequest pageRequest = this.getPageRequest(req);
                    Flux<Order> ordersFlux = ordersRepository.findAllByUserId(auth0Id, pageRequest);
                    Mono<Long> count = ordersRepository.findAllByUserId(auth0Id, Pageable.unpaged()).count();
                    return ordersFlux.collectList()
                            .zipWith(count, (list, cnt) -> OrdersGETReq.fromOrders(list, pageRequest, cnt))
                            .flatMap(dtoMono -> dtoMono)
                            .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(dto));
                });
    }

    public Mono<ServerResponse> patchOrderStatus(ServerRequest req) {
        String id = req.pathVariable("id");
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return ordersRepository.findById(id)
                .flatMap(order -> this.getAuth0IdFromToken(authorizationHeader)
                        // find the user's restaurant, to which the order being updated must belong
                        .flatMap(decodedAuth0Id -> this.webClient.get()
                                .uri("/cms" + "/" + decodedAuth0Id)
                                .header("Authorization", authorizationHeader)
                                .retrieve()
                                .bodyToMono(Restaurant.class)
                                .flatMap(restaurant -> {
                                    // 403 if the order does not belong to the restaurant
                                    if (!restaurant.getId().equals(order.getRestaurantId())) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Credentials mismatch"));
                                    }
                                    return req.bodyToMono(OrderPATCHReq.class)
                                            .flatMap(body -> {
                                                order.setStatus(body.getStatus());
                                                return this.ordersRepository.save(order)
                                                        .flatMap(res -> ServerResponse.status(HttpStatus.NO_CONTENT).build());
                                            });
                                }))
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found")));
    }

    private PageRequest getPageRequest(ServerRequest req) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Map<String, String> queryParams = req.queryParams().toSingleValueMap();
        int page = Integer.parseInt(queryParams.getOrDefault("page", "1"));
        page = page - 1;
        int size = Integer.parseInt(queryParams.getOrDefault("size", "5"));
        return PageRequest.of(page, size, sort);
    }
}

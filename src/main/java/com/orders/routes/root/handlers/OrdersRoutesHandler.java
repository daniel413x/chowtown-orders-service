package com.orders.routes.root.handlers;

import com.orders.model.Order;
import com.orders.routes.root.dto.OrdersGETReq;
import com.orders.routes.root.repository.OrdersRepository;
import com.orders.utils.BaseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
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

    public OrdersRoutesHandler() {}

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

    private PageRequest getPageRequest(ServerRequest req) {
        String sortBy = req.queryParam("sortBy").orElse("lastUpdated");
        String sortDirection = req.queryParam("sortDir").orElse("desc");
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Map<String, String> queryParams = req.queryParams().toSingleValueMap();
        int page = Integer.parseInt(queryParams.getOrDefault("page", "1"));
        page = page - 1;
        int size = Integer.parseInt(queryParams.getOrDefault("size", "10"));
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}

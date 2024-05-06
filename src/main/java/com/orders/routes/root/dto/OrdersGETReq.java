package com.orders.routes.root.dto;

import com.orders.model.Order;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
public class OrdersGETReq {

    public static Mono<OrdersGETReq> fromOrders(List<Order> orders, PageRequest pageRequest, Long count) {
        OrdersGETReq dto = new OrdersGETReq();
        dto.pagination = new Pagination(pageRequest, count);
        return Flux.fromIterable(orders)
                .flatMap(OrderDto::fromOrder)
                .collectList()
                .map(rows -> {
                    dto.setRows(rows);
                    return dto;
                });
    }

    private List<OrderDto> rows;

    private Pagination pagination;
};

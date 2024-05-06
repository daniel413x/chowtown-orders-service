package com.orders.routes.root.repository;

import com.orders.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrdersRepository extends ReactiveMongoRepository<Order, String> {
    Flux<Order> findAllByUserId(String userId, Pageable pageable);
}

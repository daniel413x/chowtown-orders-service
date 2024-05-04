package com.orders.repository;

import com.orders.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdersRepository extends ReactiveMongoRepository<Order, String> {

}

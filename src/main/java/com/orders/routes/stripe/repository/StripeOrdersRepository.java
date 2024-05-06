package com.orders.routes.stripe.repository;

import com.orders.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeOrdersRepository extends ReactiveMongoRepository<Order, String> {

}

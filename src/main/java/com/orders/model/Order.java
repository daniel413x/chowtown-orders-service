package com.orders.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private ObjectId id;

    @Field("userId")
    private String userId;

    @Field("restaurantId")
    private String restaurantId;

    @Field("deliveryDetails")
    private DeliveryDetails deliveryDetails;

    @Field("cartItems")
    private List<CartItem> cartItems;

    @Field("totalAmount")
    private Long totalAmount;

    @Field("deliveryPrice")
    private Long deliveryPrice;

    @Field("Status")
    private Status status;

    @Field("createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        PLACED, PAID, IN_PROGRESS, OUT_FOR_DELIVERY, DELIVERED
    }
}
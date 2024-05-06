package com.orders.routes.root.dto;

import com.orders.model.CartItem;
import com.orders.model.DeliveryDetails;
import com.orders.model.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    public static Mono<OrderDto> fromOrder(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId().toString());
        dto.setUserId(order.getUserId());
        dto.setRestaurantId(order.getRestaurantId());
        dto.setDeliveryDetails(order.getDeliveryDetails());
        dto.setCartItems(order.getCartItems());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        return Flux.fromIterable(order.getCartItems())
                .map(cartItem -> new CartItem(
                        cartItem.getId(),
                        cartItem.getQuantity(),
                        cartItem.getName()
                ))
                .collectList()
                .map(cartItems -> {
                    dto.setCartItems(cartItems);
                    return dto;
                });
    }

    private String id;

    private String userId;

    private String restaurantId;

    private DeliveryDetails deliveryDetails;
    
    private List<CartItem> cartItems;
    
    private Long totalAmount;

    private Order.Status status;
};
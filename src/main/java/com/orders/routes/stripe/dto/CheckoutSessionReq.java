package com.orders.routes.stripe.dto;

import com.orders.model.CartItem;
import com.orders.model.DeliveryDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionReq {

    private List<CartItem> cartItems;

    private String restaurantSlug;

    private DeliveryDetails deliveryDetails;

};
package com.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    private String id;

    private String restaurantName;

    private String city;

    private String country;

    private Integer deliveryPrice;

    private Integer estimatedDeliveryTime;

    private List<String> cuisines;

    private List<MenuItem> menuItems;

    private String imageUrl;

    private String slug;
};
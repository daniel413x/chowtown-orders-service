package com.orders.routes.root.dto;

import com.orders.model.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPATCHReq {

    private Order.Status status;
};

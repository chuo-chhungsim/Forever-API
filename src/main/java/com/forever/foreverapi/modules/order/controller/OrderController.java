package com.forever.foreverapi.modules.order.controller;

import com.forever.foreverapi.base.AuthUser;
import com.forever.foreverapi.base.PageResponse;
import com.forever.foreverapi.modules.order.dto.OrderDtos.OrderResponse;
import com.forever.foreverapi.modules.order.dto.OrderDtos.PlaceOrderRequest;
import com.forever.foreverapi.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @Operation(summary = "Place an order from my cart (clears the cart). Only COD is supported for now.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PlaceOrderRequest req) {
        return orders.place(user.id(), req);
    }

    @Operation(summary = "My orders, newest first")
    @GetMapping
    public PageResponse<OrderResponse> mine(@AuthenticationPrincipal AuthUser user,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return orders.mine(user.id(), page, size);
    }

    @Operation(summary = "One of my orders")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return orders.getMine(user.id(), id);
    }

    @Operation(summary = "Cancel my order (only while ORDER_PLACED or PACKING)")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return orders.cancel(user.id(), id);
    }
}

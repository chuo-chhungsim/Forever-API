package com.forever.foreverapi.modules.admin.controller;

import com.forever.foreverapi.base.PageResponse;
import com.forever.foreverapi.modules.order.dto.OrderDtos.OrderResponse;
import com.forever.foreverapi.modules.order.dto.OrderDtos.UpdateStatusRequest;
import com.forever.foreverapi.modules.order.model.OrderStatus;
import com.forever.foreverapi.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin - Orders", description = "ADMIN role only")
public class AdminOrderController {

    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @Operation(summary = "All orders, optionally filtered by status")
    @GetMapping
    public PageResponse<OrderResponse> all(@RequestParam(required = false) OrderStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return orders.all(status, page, size);
    }

    @Operation(summary = "Update order status (DELIVERED marks COD orders as paid)")
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return orders.updateStatus(id, req.status());
    }
}

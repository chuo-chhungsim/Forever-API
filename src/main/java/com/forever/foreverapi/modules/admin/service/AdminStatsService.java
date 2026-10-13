package com.forever.foreverapi.modules.admin.service;

import com.forever.foreverapi.modules.admin.dto.AdminDtos.StatsResponse;
import com.forever.foreverapi.modules.order.model.OrderStatus;
import com.forever.foreverapi.modules.order.repository.OrderRepository;
import com.forever.foreverapi.modules.product.repository.ProductRepository;
import com.forever.foreverapi.modules.user.model.Role;
import com.forever.foreverapi.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminStatsService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;

    public AdminStatsService(OrderRepository orders, ProductRepository products, UserRepository users) {
        this.orders = orders;
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), 0L); // every status is present, even with zero orders
        }
        long total = 0;
        for (Object[] row : orders.countGroupedByStatus()) {
            long count = (Long) row[1];
            byStatus.put(((OrderStatus) row[0]).name(), count);
            total += count;
        }
        return new StatsResponse(
                products.countByActive(true),
                products.countByActive(false),
                users.countByRole(Role.USER),
                total,
                byStatus,
                orders.sumTotalExcluding(OrderStatus.CANCELLED));
    }
}

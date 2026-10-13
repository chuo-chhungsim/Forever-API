package com.forever.foreverapi.modules.order.service;

import com.forever.foreverapi.base.PageResponse;
import com.forever.foreverapi.configuration.AppProperties;
import com.forever.foreverapi.exception.ApiException;
import com.forever.foreverapi.modules.cart.model.CartItem;
import com.forever.foreverapi.modules.cart.repository.CartItemRepository;
import com.forever.foreverapi.modules.order.dto.OrderDtos.OrderResponse;
import com.forever.foreverapi.modules.order.dto.OrderDtos.PlaceOrderRequest;
import com.forever.foreverapi.modules.order.mapper.OrderMapper;
import com.forever.foreverapi.modules.order.model.Order;
import com.forever.foreverapi.modules.order.model.OrderItem;
import com.forever.foreverapi.modules.order.model.OrderStatus;
import com.forever.foreverapi.modules.order.model.PaymentMethod;
import com.forever.foreverapi.modules.order.repository.OrderRepository;
import com.forever.foreverapi.modules.product.model.Product;
import com.forever.foreverapi.modules.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.ORDER_PLACED, OrderStatus.PACKING);
    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final OrderRepository orders;
    private final CartItemRepository cartItems;
    private final UserRepository users;
    private final OrderMapper mapper;
    private final AppProperties props;

    public OrderService(OrderRepository orders, CartItemRepository cartItems, UserRepository users,
                        OrderMapper mapper, AppProperties props) {
        this.orders = orders;
        this.cartItems = cartItems;
        this.users = users;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public OrderResponse place(Long userId, PlaceOrderRequest req) {
        if (req.paymentMethod() != PaymentMethod.COD) {
            throw ApiException.badRequest("PAYMENT_METHOD_UNSUPPORTED",
                    "Only cash on delivery (COD) is available right now");
        }
        List<CartItem> cart = cartItems.findAllByUser(userId);
        if (cart.isEmpty()) {
            throw ApiException.badRequest("CART_EMPTY", "Your cart is empty");
        }

        Order order = new Order();
        order.setUser(users.getReferenceById(userId));
        order.setPaymentMethod(req.paymentMethod());
        order.setAddress(mapper.toEntity(req.address()));

        BigDecimal itemsAmount = BigDecimal.ZERO;
        for (CartItem c : cart) {
            Product p = c.getProduct();
            if (!p.isActive()) {
                throw ApiException.conflict("PRODUCT_UNAVAILABLE", p.getName() + " is no longer available");
            }
            OrderItem item = new OrderItem();
            item.setProductId(p.getId());
            item.setName(p.getName());
            item.setImageKey(p.getImages().isEmpty() ? null : p.getImages().get(0));
            item.setPrice(p.getPrice());
            item.setSize(c.getSize());
            item.setQuantity(c.getQuantity());
            order.addItem(item);
            itemsAmount = itemsAmount.add(p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        order.setItemsAmount(itemsAmount);
        order.setDeliveryFee(props.shop().deliveryFee());
        order.setTotal(itemsAmount.add(props.shop().deliveryFee()));

        Order saved = orders.save(order);
        cartItems.deleteAllByUser(userId);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> mine(Long userId, int page, int size) {
        return PageResponse.of(orders.findByUserId(userId, pageable(page, size)), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(Long userId, Long id) {
        return mapper.toResponse(orders.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Order not found")));
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long id) {
        Order o = orders.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!CANCELLABLE.contains(o.getStatus())) {
            throw ApiException.conflict("ORDER_NOT_CANCELLABLE", "This order can no longer be cancelled");
        }
        o.setStatus(OrderStatus.CANCELLED);
        return mapper.toResponse(o);
    }

    // ---- admin ----

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> all(OrderStatus status, int page, int size) {
        var pageable = pageable(page, size);
        return PageResponse.of(status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable),
                mapper::toResponse);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.DELIVERED) {
            throw ApiException.conflict("ORDER_CLOSED", "A " + o.getStatus() + " order cannot change status");
        }
        o.setStatus(status);
        if (status == OrderStatus.DELIVERED && o.getPaymentMethod() == PaymentMethod.COD) {
            o.setPaid(true); // cash collected on delivery
        }
        return mapper.toResponse(o);
    }

    private static org.springframework.data.domain.Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), NEWEST);
    }
}

package com.staynest.fb.serviceimpl;

import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.dto.FBOrderRequest;
import com.staynest.fb.dto.FBOrderResponse;
import com.staynest.fb.entity.FBOrder;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.FBOrderRepository;
import com.staynest.fb.service.FBOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FBOrderServiceImpl implements FBOrderService {

    private final FBOrderRepository orderRepository;
    private final FrontDeskServiceClient frontDeskClient;

    @Override
    @Transactional
    public FBOrderResponse placeOrder(FBOrderRequest request) {
        // Calculate total from itemsJSON (simplified - parse JSON in real scenario)
        BigDecimal totalAmount = calculateTotal(request.getItemsJson());

        FBOrder order = FBOrder.builder()
                .stayId(request.getStayId())
                .tableNumber(request.getTableNumber())
                .orderType(request.getOrderType())
                .itemsJson(request.getItemsJson())
                .totalAmount(totalAmount)
                .build();

        FBOrder saved = orderRepository.save(order);
        log.info("Order placed: {}", saved.getOrderId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public FBOrderResponse updateOrderStatus(Integer orderId, OrderStatus status) {
        FBOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Validate status transition
        if (!isValidTransition(order.getStatus(), status)) {
            throw new BadRequestException("Invalid status transition from " + order.getStatus() + " to " + status);
        }

        order.setStatus(status);
        FBOrder updated = orderRepository.save(order);

        // If BILLED, post to folio
        if (status == OrderStatus.BILLED) {
            try {
                Map<String, Object> folioItem = new HashMap<>();
                folioItem.put("chargeType", "FBCHARGE");
                folioItem.put("description", "F&B Order: " + order.getOrderId());
                folioItem.put("amount", order.getTotalAmount());
                folioItem.put("postedBy", 1); // Should come from auth context
                frontDeskClient.postFolioItem(order.getStayId(), folioItem);
            } catch (Exception e) {
                log.warn("Failed to post to folio: {}", e.getMessage());
            }
        }

        log.info("Order {} status updated to {}", orderId, status);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public FBOrderResponse cancelOrder(Integer orderId) {
        FBOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Can only cancel orders with status PLACED. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        FBOrder updated = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
        return mapToResponse(updated);
    }

    @Override
    public FBOrderResponse getOrderById(Integer orderId) {
        FBOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    @Override
    public List<FBOrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<FBOrderResponse> getOrdersByStayId(Integer stayId) {
        return orderRepository.findByStayId(stayId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<FBOrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(String itemsJson) {
        // Simplified: In real scenario, parse JSON and lookup menu item prices
        return new BigDecimal("500.00");
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PLACED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.SERVED;
            case SERVED -> next == OrderStatus.BILLED;
            default -> false;
        };
    }

    private FBOrderResponse mapToResponse(FBOrder order) {
        return FBOrderResponse.builder()
                .orderId(order.getOrderId())
                .stayId(order.getStayId())
                .tableNumber(order.getTableNumber())
                .orderType(order.getOrderType())
                .itemsJson(order.getItemsJson())
                .totalAmount(order.getTotalAmount())
                .orderTime(order.getOrderTime())
                .status(order.getStatus())
                .build();
    }
}
package com.staynest.fb.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.dto.FBOrderItemResponse;
import com.staynest.fb.dto.FBOrderRequest;
import com.staynest.fb.dto.FBOrderResponse;
import com.staynest.fb.entity.FBOrder;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.FBOrderRepository;
import com.staynest.fb.repository.MenuItemRepository;
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
    private final MenuItemRepository menuItemRepository;
    private final FrontDeskServiceClient frontDeskClient;
    private final com.staynest.fb.client.NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(java.util.Map.of(
                    "userId", userId, "category", "FB", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send FB notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public FBOrderResponse placeOrder(FBOrderRequest request) {
        // An order must be attached to a real stay — reject unknown/invalid stayIds instead of
        // silently creating orphaned orders.
        validateStay(request.getStayId());

        // Calculate total from itemsJSON (simplified - parse JSON in real scenario)
        BigDecimal totalAmount = calculateTotal(request.getItemsJson());

        FBOrder order = FBOrder.builder()
                .stayId(request.getStayId())
                .orderType(request.getOrderType())
                .itemsJson(request.getItemsJson())
                .totalAmount(totalAmount)
                .build();

        FBOrder saved = orderRepository.save(order);
        log.info("Order placed: {}", saved.getOrderId());
        notify(request.getPlacedBy(), "Your F&B order #" + saved.getOrderId()
                + " has been placed. Total: " + totalAmount + ".");
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

    /**
     * Computes the order total from the cart JSON produced by the frontend, which is a JSON array
     * of {@code {"itemId": <menuItemId>, "qty": <quantity>}} entries, by summing
     * {@code menuItem.price * qty} for each line.
     */
    /**
     * Confirms the order's stayId maps to a real stay in frontdesk-service and that the stay is
     * still open. Rejects unknown stays (404) and closed/checked-out stays so orders can't be
     * allocated to non-existent or departed stays.
     */
    private void validateStay(Integer stayId) {
        if (stayId == null) {
            throw new BadRequestException("StayId is required");
        }
        Map<String, Object> stay;
        try {
            var resp = frontDeskClient.getStayById(stayId);
            stay = resp != null ? resp.getData() : null;
        } catch (Exception e) {
            // The circuit breaker wraps the downstream 404, so the cause chain has to be walked
            // to tell "no such stay" apart from a genuine frontdesk-service outage.
            if (com.staynest.fb.client.FeignErrors.isNotFound(e)) {
                throw new BadRequestException("Invalid StayId: " + stayId + " (no such stay)");
            }
            log.error("frontdesk-service call failed while validating StayId {}", stayId, e);
            throw new BadRequestException("Unable to validate StayId " + stayId
                    + " (frontdesk-service error: " + e.getMessage() + ")");
        }
        if (stay == null) {
            throw new BadRequestException("Invalid StayId: " + stayId + " (no such stay)");
        }
        Object status = stay.get("status");
        if (status != null && "CHECKEDOUT".equalsIgnoreCase(status.toString())) {
            throw new BadRequestException("Cannot place an F&B order for stay " + stayId
                    + " — the guest has already checked out");
        }
    }

    private BigDecimal calculateTotal(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            throw new BadRequestException("Order must contain at least one item");
        }
        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(itemsJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BadRequestException("Invalid items payload: " + e.getMessage());
        }
        if (items.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> line : items) {
            Object rawId = line.get("itemId");
            Object rawQty = line.get("qty");
            if (rawId == null || rawQty == null) {
                throw new BadRequestException("Each order line must include itemId and qty");
            }
            Integer itemId = ((Number) rawId).intValue();
            int qty = ((Number) rawQty).intValue();
            if (qty <= 0) {
                throw new BadRequestException("Quantity must be positive for item " + itemId);
            }
            MenuItem menuItem = menuItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        return total;
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
                .orderType(order.getOrderType())
                .itemsJson(order.getItemsJson())
                .items(buildItemResponses(order.getItemsJson()))
                .totalAmount(order.getTotalAmount())
                .orderTime(order.getOrderTime())
                .status(order.getStatus())
                .build();
    }

    /**
     * Resolves the stored cart JSON into a structured, human-readable line list
     * (item name, quantity, unit price, line total) so clients render items instead
     * of a raw JSON blob. Returns an empty list if the payload is missing or unparseable.
     */
    private List<FBOrderItemResponse> buildItemResponses(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> lines;
        try {
            lines = objectMapper.readValue(itemsJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Could not parse itemsJson for order response: {}", e.getMessage());
            return List.of();
        }

        List<FBOrderItemResponse> result = new java.util.ArrayList<>();
        for (Map<String, Object> line : lines) {
            Object rawId = line.get("itemId");
            Object rawQty = line.get("qty");
            if (rawId == null || rawQty == null) {
                continue;
            }
            Integer itemId = ((Number) rawId).intValue();
            int qty = ((Number) rawQty).intValue();
            MenuItem menuItem = menuItemRepository.findById(itemId).orElse(null);
            String name = menuItem != null ? menuItem.getName() : "Item #" + itemId;
            BigDecimal unitPrice = menuItem != null ? menuItem.getPrice() : BigDecimal.ZERO;
            result.add(FBOrderItemResponse.builder()
                    .menuItemId(itemId)
                    .name(name)
                    .qty(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                    .build());
        }
        return result;
    }
}
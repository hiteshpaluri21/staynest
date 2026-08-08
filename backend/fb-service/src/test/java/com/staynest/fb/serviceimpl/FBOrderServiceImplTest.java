package com.staynest.fb.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.client.NotificationServiceClient;
import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.FBOrderRequest;
import com.staynest.fb.dto.FBOrderResponse;
import com.staynest.fb.entity.FBOrder;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.enums.OrderType;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.repository.FBOrderRepository;
import com.staynest.fb.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An order's money and its lifecycle.
 *
 * The total is computed from the menu rather than trusted from the client, the charge reaches
 * the folio the moment the order is placed, and cancelling has to take that charge back off —
 * which is only allowed while the kitchen has not started.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FBOrderServiceImplTest {

    private static final int ORDER_ID = 21;
    private static final int STAY_ID = 1;
    private static final int PLACED_BY = 7;
    private static final int BIRYANI = 3;
    private static final int LASSI = 4;

    @Mock private AuditRecorder auditRecorder;
    @Mock private FBOrderRepository orderRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private FrontDeskServiceClient frontDeskClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    /** Real Jackson: the cart JSON parsing is part of what these tests exercise. */
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private FBOrderServiceImpl service;

    private static MenuItem menuItem(int id, String name, String price) {
        MenuItem item = new MenuItem();
        item.setMenuItemId(id);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private static FBOrderRequest request(String itemsJson) {
        FBOrderRequest req = new FBOrderRequest();
        req.setStayId(STAY_ID);
        req.setOrderType(OrderType.INROOMDINING);
        req.setItemsJson(itemsJson);
        req.setPlacedBy(PLACED_BY);
        return req;
    }

    private static FBOrder order(OrderStatus status, String total) {
        return FBOrder.builder()
                .orderId(ORDER_ID)
                .stayId(STAY_ID)
                .orderType(OrderType.INROOMDINING)
                .itemsJson("[]")
                .totalAmount(new BigDecimal(total))
                .placedBy(PLACED_BY)
                .status(status)
                .build();
    }

    /** frontdesk-service confirms the stay exists and is still open. */
    private void stayIsOpen() {
        doReturn(ApiResponse.success(Map.of("stayId", STAY_ID, "status", "ACTIVE")))
                .when(frontDeskClient).getStayById(STAY_ID);
    }

    private void menuHasBiryaniAndLassi() {
        when(menuItemRepository.findById(BIRYANI)).thenReturn(Optional.of(menuItem(BIRYANI, "Biryani", "320.00")));
        when(menuItemRepository.findById(LASSI)).thenReturn(Optional.of(menuItem(LASSI, "Lassi", "90.00")));
    }

    @Test
    void placeOrder_calculatesCorrectTotal() {
        stayIsOpen();
        menuHasBiryaniAndLassi();
        when(orderRepository.save(any(FBOrder.class))).thenAnswer(inv -> {
            FBOrder o = inv.getArgument(0);
            o.setOrderId(ORDER_ID);
            return o;
        });

        // Two biryanis at 320 plus three lassis at 90 = 640 + 270.
        FBOrderResponse placed = service.placeOrder(request(
                "[{\"itemId\":3,\"qty\":2},{\"itemId\":4,\"qty\":3}]"));

        assertThat(placed.getTotalAmount()).isEqualByComparingTo(new BigDecimal("910.00"));
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.getItems()).hasSize(2);

        // The charge lands on the folio at placement, not at billing.
        verify(frontDeskClient).postFolioItem(eq(STAY_ID), any(Map.class));
        verify(auditRecorder).record("CREATE", "FBORDER", ORDER_ID);
    }

    @Test
    void cancelOrder_placed_becomesCancelled() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PLACED, "910.00")));
        when(orderRepository.save(any(FBOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        FBOrderResponse cancelled = service.cancelOrder(ORDER_ID);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // Folio lines are never deleted, so the reversal is a DISCOUNT for the same amount.
        verify(frontDeskClient).postFolioItem(eq(STAY_ID), any(Map.class));
        verify(auditRecorder).record("CANCEL", "FBORDER", ORDER_ID);
    }

    /** Once the kitchen has served it the guest is committed, so it can no longer be cancelled. */
    @Test
    void cancelOrder_served_throwsException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.SERVED, "910.00")));

        assertThatThrownBy(() -> service.cancelOrder(ORDER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Can only cancel orders with status PLACED");

        verify(orderRepository, never()).save(any());
        // No reversal posted, so the guest is still charged for what they ate.
        verify(frontDeskClient, never()).postFolioItem(any(), any());
    }

    @Test
    void updateOrderStatus_changes() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PLACED, "910.00")));
        when(orderRepository.save(any(FBOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        FBOrderResponse updated = service.updateOrderStatus(ORDER_ID, OrderStatus.PREPARING);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PREPARING);
        // The charge went on at placement, so moving the status must not post a second one.
        verify(frontDeskClient, never()).postFolioItem(any(), any());
        verify(auditRecorder).record("UPDATE_STATUS", "FBORDER", ORDER_ID);
    }
}

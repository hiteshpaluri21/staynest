package com.staynest.fb.controller;

import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.FBOrderRequest;
import com.staynest.fb.dto.FBOrderResponse;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.service.FBOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fb-orders")
@RequiredArgsConstructor
public class FBOrderController {

    private final FBOrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER', 'FRONTDESK', 'GUEST')")
    public ResponseEntity<ApiResponse<FBOrderResponse>> create(@Valid @RequestBody FBOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed", orderService.placeOrder(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FBOrderResponse>>> getAll(
            @RequestParam(required = false) Integer stayId,
            @RequestParam(required = false) OrderStatus status) {
        if (stayId != null) {
            return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByStayId(stayId)));
        }
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FBOrderResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<FBOrderResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status)));
    }

    // A guest can call off their own order, same as a dining reservation. cancelOrder() only allows
    // this while the order is still PLACED — once the kitchen has started, staff handle it.
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'FBMANAGER', 'FRONTDESK', 'GUEST')")
    public ResponseEntity<ApiResponse<FBOrderResponse>> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(id)));
    }
}
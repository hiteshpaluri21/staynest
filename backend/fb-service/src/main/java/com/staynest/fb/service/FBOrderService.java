package com.staynest.fb.service;

import com.staynest.fb.dto.FBOrderRequest;
import com.staynest.fb.dto.FBOrderResponse;
import com.staynest.fb.enums.OrderStatus;

import java.util.List;

public interface FBOrderService {
    FBOrderResponse placeOrder(FBOrderRequest request);
    FBOrderResponse updateOrderStatus(Integer orderId, OrderStatus status);
    FBOrderResponse cancelOrder(Integer orderId);
    FBOrderResponse getOrderById(Integer orderId);
    List<FBOrderResponse> getAllOrders();
    List<FBOrderResponse> getOrdersByStayId(Integer stayId);
    List<FBOrderResponse> getOrdersByStatus(OrderStatus status);
}
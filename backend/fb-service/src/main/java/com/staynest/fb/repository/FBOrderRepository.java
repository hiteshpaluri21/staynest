package com.staynest.fb.repository;

import com.staynest.fb.entity.FBOrder;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FBOrderRepository extends JpaRepository<FBOrder, Integer> {
    List<FBOrder> findByStayId(Integer stayId);
    List<FBOrder> findByStatus(OrderStatus status);
    List<FBOrder> findByStatusIn(Collection<OrderStatus> statuses);
    List<FBOrder> findByOrderType(OrderType orderType);
}
package com.staynest.frontdesk.repository;

import com.staynest.frontdesk.entity.FolioItem;
import com.staynest.frontdesk.enums.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolioItemRepository extends JpaRepository<FolioItem, Integer> {
    List<FolioItem> findByStayRecord_StayId(Integer stayId);
    List<FolioItem> findByChargeType(ChargeType chargeType);
}
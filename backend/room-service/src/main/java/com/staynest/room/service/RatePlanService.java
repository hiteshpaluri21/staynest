package com.staynest.room.service;

import com.staynest.room.dto.RatePlanRequest;
import com.staynest.room.dto.RatePlanResponse;
import com.staynest.room.enums.RatePlanStatus;

import java.time.LocalDate;
import java.util.List;

public interface RatePlanService {

    RatePlanResponse createRatePlan(RatePlanRequest request);
    List<RatePlanResponse> getAllRatePlans();
    List<RatePlanResponse> getActivePlansForRoomType(Integer roomTypeId, LocalDate date);
    RatePlanResponse updateStatus(Integer id, RatePlanStatus status);
}
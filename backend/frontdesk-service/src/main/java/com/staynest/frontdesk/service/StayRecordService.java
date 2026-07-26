package com.staynest.frontdesk.service;

import com.staynest.frontdesk.dto.CheckInRequest;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.StayRecordResponse;
import com.staynest.frontdesk.entity.StayRecord;

import java.util.List;

public interface StayRecordService {
    StayRecordResponse checkIn(CheckInRequest request);
    StayRecordResponse postFolioItem(Integer stayId, FolioItemRequest request);
    StayRecordResponse checkOut(Integer stayId);
    StayRecordResponse getStayById(Integer stayId);
    List<StayRecordResponse> getAllStays();
    List<StayRecordResponse> getStaysByGuestId(Integer guestId);
    List<StayRecordResponse> getStaysByStatus(String status);
    StayRecord getStayEntityById(Integer stayId);
}
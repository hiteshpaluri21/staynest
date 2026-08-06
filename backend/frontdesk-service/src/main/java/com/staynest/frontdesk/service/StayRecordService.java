package com.staynest.frontdesk.service;

import com.staynest.frontdesk.dto.CheckInRequest;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.StayRecordResponse;
import com.staynest.frontdesk.entity.StayRecord;

import java.util.List;

public interface StayRecordService {
    StayRecordResponse checkIn(CheckInRequest request);
    StayRecordResponse postFolioItem(Integer stayId, FolioItemRequest request);
    /**
     * @param housekeepingStaffId the housekeeping staff member the post-checkout cleaning task is
     *                            assigned to; when null no task is raised, since tasks may not be
     *                            left unassigned.
     */
    StayRecordResponse checkOut(Integer stayId, Integer housekeepingStaffId);
    StayRecordResponse getStayById(Integer stayId);
    List<StayRecordResponse> getAllStays();
    List<StayRecordResponse> getStaysByGuestId(Integer guestId);
    List<StayRecordResponse> getStaysByStatus(String status);
    StayRecord getStayEntityById(Integer stayId);
}
package com.staynest.frontdesk.service;

import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.FolioItemResponse;
import com.staynest.frontdesk.enums.ChargeType;

import java.util.List;

public interface FolioItemService {
    FolioItemResponse addFolioItem(Integer stayId, FolioItemRequest request);
    FolioItemResponse updateFolioItem(Integer folioItemId, FolioItemRequest request);
    List<FolioItemResponse> getFolioItemsByStayId(Integer stayId);
    List<FolioItemResponse> getFolioItemsByChargeType(ChargeType chargeType);
}
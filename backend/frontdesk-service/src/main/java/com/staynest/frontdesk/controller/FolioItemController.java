package com.staynest.frontdesk.controller;

import com.staynest.frontdesk.dto.ApiResponse;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.FolioItemResponse;
import com.staynest.frontdesk.enums.ChargeType;
import com.staynest.frontdesk.service.FolioItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folio-items")
@RequiredArgsConstructor
public class FolioItemController {

    private final FolioItemService folioItemService;

    @PostMapping("/stay/{stayId}")
    public ResponseEntity<ApiResponse<FolioItemResponse>> add(
            @PathVariable Integer stayId,
            @Valid @RequestBody FolioItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folio item added", folioItemService.addFolioItem(stayId, request)));
    }

    @GetMapping("/stay/{stayId}")
    public ResponseEntity<ApiResponse<List<FolioItemResponse>>> getByStayId(@PathVariable Integer stayId) {
        return ResponseEntity.ok(ApiResponse.success(folioItemService.getFolioItemsByStayId(stayId)));
    }

    @GetMapping("/charge-type/{chargeType}")
    public ResponseEntity<ApiResponse<List<FolioItemResponse>>> getByChargeType(@PathVariable ChargeType chargeType) {
        return ResponseEntity.ok(ApiResponse.success(folioItemService.getFolioItemsByChargeType(chargeType)));
    }
}
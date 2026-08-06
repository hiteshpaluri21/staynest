package com.staynest.frontdesk.controller;

import com.staynest.frontdesk.dto.ApiResponse;
import com.staynest.frontdesk.dto.CheckInRequest;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.StayRecordResponse;
import com.staynest.frontdesk.service.StayRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.staynest.frontdesk.dto.FolioItemResponse;
import com.staynest.frontdesk.service.FolioItemService;

@Slf4j
@RestController
@RequestMapping("/api/stay-records")
@RequiredArgsConstructor
public class StayRecordController {

    private final StayRecordService stayRecordService;
    private final FolioItemService folioItemService;

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK')")
    public ResponseEntity<ApiResponse<StayRecordResponse>> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Check-in successful", stayRecordService.checkIn(request)));
    }

    // F&B needs the list of open stays to attach orders and dining reservations to a real stay.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'FBMANAGER', 'GUEST')")
    public ResponseEntity<ApiResponse<List<StayRecordResponse>>> getAll(
            @RequestParam(required = false) Integer guestId,
            @RequestParam(required = false) String status) {
        if (guestId != null) {
            return ResponseEntity.ok(ApiResponse.success(stayRecordService.getStaysByGuestId(guestId)));
        }
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(stayRecordService.getStaysByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(stayRecordService.getAllStays()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StayRecordResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(stayRecordService.getStayById(id)));
    }

    @PostMapping("/{id}/folio-items")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'FBMANAGER', 'GUEST')")
    public ResponseEntity<ApiResponse<StayRecordResponse>> addFolioItem(
            @PathVariable Integer id,
            @Valid @RequestBody FolioItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folio item added", stayRecordService.postFolioItem(id, request)));
    }

    @GetMapping("/{id}/folio-items")
    public ResponseEntity<ApiResponse<List<FolioItemResponse>>> getFolioItems(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(folioItemService.getFolioItemsByStayId(id)));
    }

    @PutMapping("/{id}/folio-items/{folioItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK', 'FBMANAGER')")
    public ResponseEntity<ApiResponse<FolioItemResponse>> updateFolioItem(
            @PathVariable Integer id,
            @PathVariable Integer folioItemId,
            @Valid @RequestBody FolioItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Folio item updated",
                folioItemService.updateFolioItem(folioItemId, request)));
    }

    // housekeepingStaffId is who the automatic post-checkout cleaning task goes to — front desk
    // picks them in the checkout dialog so the task never lands on the board unassigned.
    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONTDESK')")
    public ResponseEntity<ApiResponse<StayRecordResponse>> checkOut(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer housekeepingStaffId) {
        return ResponseEntity.ok(ApiResponse.success("Check-out successful",
                stayRecordService.checkOut(id, housekeepingStaffId)));
    }
}
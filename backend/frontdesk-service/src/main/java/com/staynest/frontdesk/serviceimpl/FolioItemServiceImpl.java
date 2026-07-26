package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.FolioItemResponse;
import com.staynest.frontdesk.entity.FolioItem;
import com.staynest.frontdesk.entity.StayRecord;
import com.staynest.frontdesk.enums.ChargeType;
import com.staynest.frontdesk.enums.StayStatus;
import com.staynest.frontdesk.exception.BadRequestException;
import com.staynest.frontdesk.exception.ResourceNotFoundException;
import com.staynest.frontdesk.repository.FolioItemRepository;
import com.staynest.frontdesk.repository.StayRecordRepository;
import com.staynest.frontdesk.service.FolioItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolioItemServiceImpl implements FolioItemService {

    private final FolioItemRepository folioItemRepository;
    private final StayRecordRepository stayRecordRepository;

    @Override
    public FolioItemResponse addFolioItem(Integer stayId, FolioItemRequest request) {
        StayRecord stay = stayRecordRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.getStatus() == StayStatus.CHECKEDOUT) {
            throw new BadRequestException("Cannot add charges to checked-out stay");
        }

        FolioItem item = FolioItem.builder()
                .stayRecord(stay)
                .chargeType(request.getChargeType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .postedBy(request.getPostedBy())
                .build();

        FolioItem saved = folioItemRepository.save(item);
        log.info("Folio item added: {}", saved.getFolioItemId());
        return mapToResponse(saved);
    }

    @Override
    public List<FolioItemResponse> getFolioItemsByStayId(Integer stayId) {
        return folioItemRepository.findByStayRecord_StayId(stayId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<FolioItemResponse> getFolioItemsByChargeType(ChargeType chargeType) {
        return folioItemRepository.findByChargeType(chargeType).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private FolioItemResponse mapToResponse(FolioItem item) {
        return FolioItemResponse.builder()
                .folioItemId(item.getFolioItemId())
                .stayId(item.getStayRecord().getStayId())
                .chargeType(item.getChargeType())
                .description(item.getDescription())
                .amount(item.getAmount())
                .postedDate(item.getPostedDate())
                .postedBy(item.getPostedBy())
                .build();
    }
}
package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.audit.AuditRecorder;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolioItemServiceImpl implements FolioItemService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "FOLIOITEM";

    private final AuditRecorder auditRecorder;
    private final FolioItemRepository folioItemRepository;
    private final StayRecordRepository stayRecordRepository;

    @Override
    @Transactional
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

        // Keep the running folio balance in sync (a DISCOUNT reduces the balance).
        BigDecimal current = stay.getFolioBalance() == null ? BigDecimal.ZERO : stay.getFolioBalance();
        stay.setFolioBalance(current.add(signedAmount(request.getChargeType(), request.getAmount())));
        stayRecordRepository.save(stay);

        log.info("Folio item added: {}", saved.getFolioItemId());
        auditRecorder.record("CREATE", ENTITY, saved.getFolioItemId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public FolioItemResponse updateFolioItem(Integer folioItemId, FolioItemRequest request) {
        FolioItem item = folioItemRepository.findById(folioItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio item not found: " + folioItemId));
        StayRecord stay = item.getStayRecord();

        if (stay.getStatus() == StayStatus.CHECKEDOUT) {
            throw new BadRequestException("Cannot edit charges on a checked-out stay");
        }

        // Re-balance the folio by the delta between the old and new signed amounts so the
        // running total stays correct after an edit.
        BigDecimal oldSigned = signedAmount(item.getChargeType(), item.getAmount());
        BigDecimal newSigned = signedAmount(request.getChargeType(), request.getAmount());
        BigDecimal current = stay.getFolioBalance() == null ? BigDecimal.ZERO : stay.getFolioBalance();
        stay.setFolioBalance(current.subtract(oldSigned).add(newSigned));

        item.setChargeType(request.getChargeType());
        item.setDescription(request.getDescription());
        item.setAmount(request.getAmount());
        if (request.getPostedBy() != null) {
            item.setPostedBy(request.getPostedBy());
        }

        FolioItem saved = folioItemRepository.save(item);
        stayRecordRepository.save(stay);
        log.info("Folio item updated: {}", saved.getFolioItemId());
        auditRecorder.record("UPDATE", ENTITY, saved.getFolioItemId());
        return mapToResponse(saved);
    }

    /** A DISCOUNT charge reduces the folio balance; every other charge type adds to it. */
    static BigDecimal signedAmount(ChargeType chargeType, BigDecimal amount) {
        return chargeType == ChargeType.DISCOUNT ? amount.negate() : amount;
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
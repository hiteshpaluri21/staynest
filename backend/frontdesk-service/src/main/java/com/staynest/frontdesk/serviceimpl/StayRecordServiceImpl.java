package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.client.ReservationServiceClient;
import com.staynest.frontdesk.client.RoomServiceClient;
import com.staynest.frontdesk.dto.CheckInRequest;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.StayRecordResponse;
import com.staynest.frontdesk.entity.FolioItem;
import com.staynest.frontdesk.entity.StayRecord;
import com.staynest.frontdesk.enums.StayStatus;
import com.staynest.frontdesk.exception.BadRequestException;
import com.staynest.frontdesk.exception.ResourceNotFoundException;
import com.staynest.frontdesk.repository.FolioItemRepository;
import com.staynest.frontdesk.repository.StayRecordRepository;
import com.staynest.frontdesk.service.StayRecordService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayRecordServiceImpl implements StayRecordService {

    private final StayRecordRepository stayRecordRepository;
    private final FolioItemRepository folioItemRepository;
    private final RoomServiceClient roomServiceClient;
    private final ReservationServiceClient reservationServiceClient;

    @Override
    @Transactional
    public StayRecordResponse checkIn(CheckInRequest request) {
        // Verify reservation exists
        try {
            reservationServiceClient.getReservationById(request.getReservationId());
        } catch (FeignException.NotFound e) {
            throw new BadRequestException("Invalid ReservationId: " + request.getReservationId());
        } catch (Exception e) {
            log.error("reservation-service call failed while validating ReservationId {}", request.getReservationId(), e);
            throw new BadRequestException("Unable to validate ReservationId " + request.getReservationId()
                    + " (reservation-service error: " + e.getMessage() + ")");
        }

        // Check if stay already exists for this reservation
        if (stayRecordRepository.findByReservationId(request.getReservationId()).isPresent()) {
            throw new BadRequestException("Stay already exists for reservation: " + request.getReservationId());
        }

        StayRecord stay = StayRecord.builder()
                .reservationId(request.getReservationId())
                .guestId(request.getRoomId()) // Will be fetched from reservation in real scenario
                .assignedRoomId(request.getRoomId())
                .actualCheckIn(LocalDateTime.now())
                .folioBalance(BigDecimal.ZERO)
                .status(StayStatus.ACTIVE)
                .build();

        StayRecord saved = stayRecordRepository.save(stay);

        // Update room status to OCCUPIED
        try {
            roomServiceClient.updateRoomStatus(request.getRoomId(), "OCCUPIED");
        } catch (Exception e) {
            log.warn("Failed to update room status: {}", e.getMessage());
        }

        // Update reservation status to CHECKEDIN
        try {
            reservationServiceClient.updateReservationStatus(request.getReservationId(), "CHECKEDIN");
        } catch (Exception e) {
            log.warn("Failed to update reservation status: {}", e.getMessage());
        }

        log.info("Check-in completed for reservation: {}", request.getReservationId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public StayRecordResponse postFolioItem(Integer stayId, FolioItemRequest request) {
        StayRecord stay = stayRecordRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.getStatus() == StayStatus.CHECKEDOUT) {
            throw new BadRequestException("Cannot post charges to a checked-out stay");
        }

        FolioItem item = FolioItem.builder()
                .stayRecord(stay)
                .chargeType(request.getChargeType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .postedBy(request.getPostedBy())
                .build();

        folioItemRepository.save(item);

        // Update folio balance
        BigDecimal newBalance = stay.getFolioBalance().add(request.getAmount());
        stay.setFolioBalance(newBalance);
        StayRecord updated = stayRecordRepository.save(stay);

        log.info("Folio item posted for stay: {}, amount: {}", stayId, request.getAmount());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public StayRecordResponse checkOut(Integer stayId) {
        StayRecord stay = stayRecordRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.getStatus() == StayStatus.CHECKEDOUT) {
            throw new BadRequestException("Stay is already checked out");
        }

        // Sum all folio items
        List<FolioItem> items = folioItemRepository.findByStayRecord_StayId(stayId);
        BigDecimal total = items.stream()
                .map(FolioItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stay.setFolioBalance(total);
        stay.setActualCheckOut(LocalDateTime.now());
        stay.setStatus(StayStatus.CHECKEDOUT);
        StayRecord updated = stayRecordRepository.save(stay);

        // Update room status to AVAILABLE
        try {
            roomServiceClient.updateRoomStatus(stay.getAssignedRoomId(), "AVAILABLE");
        } catch (Exception e) {
            log.warn("Failed to update room status: {}", e.getMessage());
        }

        // Update reservation status to CHECKEDOUT
        try {
            reservationServiceClient.updateReservationStatus(stay.getReservationId(), "CHECKEDOUT");
        } catch (Exception e) {
            log.warn("Failed to update reservation status: {}", e.getMessage());
        }

        log.info("Check-out completed for stay: {}, total folio: {}", stayId, total);
        return mapToResponse(updated);
    }

    @Override
    public StayRecordResponse getStayById(Integer stayId) {
        StayRecord stay = stayRecordRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
        return mapToResponse(stay);
    }

    @Override
    public List<StayRecordResponse> getAllStays() {
        return stayRecordRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StayRecordResponse> getStaysByGuestId(Integer guestId) {
        return stayRecordRepository.findByGuestId(guestId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StayRecordResponse> getStaysByStatus(String status) {
        StayStatus stayStatus = StayStatus.valueOf(status.toUpperCase());
        return stayRecordRepository.findByStatus(stayStatus).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public StayRecord getStayEntityById(Integer stayId) {
        return stayRecordRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
    }

    private StayRecordResponse mapToResponse(StayRecord stay) {
        return StayRecordResponse.builder()
                .stayId(stay.getStayId())
                .reservationId(stay.getReservationId())
                .guestId(stay.getGuestId())
                .assignedRoomId(stay.getAssignedRoomId())
                .actualCheckIn(stay.getActualCheckIn())
                .actualCheckOut(stay.getActualCheckOut())
                .folioBalance(stay.getFolioBalance())
                .status(stay.getStatus())
                .build();
    }
}
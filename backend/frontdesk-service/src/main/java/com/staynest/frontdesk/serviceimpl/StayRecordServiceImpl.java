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
    private final com.staynest.frontdesk.client.NotificationServiceClient notificationServiceClient;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(java.util.Map.of(
                    "userId", userId, "category", "FRONTDESK", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send FRONTDESK notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public StayRecordResponse checkIn(CheckInRequest request) {
        Integer guestId = 1;
        try {
            var res = reservationServiceClient.getReservationById(request.getReservationId());
            if (res != null && res.getData() instanceof java.util.Map<?, ?> map) {
                if (map.get("guestId") != null) {
                    guestId = Integer.parseInt(map.get("guestId").toString());
                }
            }
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

        // Check if room is already assigned to an active stay
        if (stayRecordRepository.findByAssignedRoomIdAndStatus(request.getRoomId(), StayStatus.ACTIVE).isPresent()) {
            throw new BadRequestException("Room #" + request.getRoomId() + " is currently occupied by an active stay");
        }

        StayRecord stay = StayRecord.builder()
                .reservationId(request.getReservationId())
                .guestId(guestId)
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
            log.error("Failed to update room {} status to OCCUPIED on check-in", request.getRoomId(), e);
        }

        // Update reservation status to CHECKEDIN
        try {
            reservationServiceClient.updateReservationStatus(request.getReservationId(), "CHECKEDIN");
        } catch (Exception e) {
            log.warn("Failed to update reservation status: {}", e.getMessage());
        }

        notify(guestId, "Welcome! You are checked in to room #" + request.getRoomId() + ".");

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

        // Update folio balance (a DISCOUNT reduces the balance).
        BigDecimal current = stay.getFolioBalance() == null ? BigDecimal.ZERO : stay.getFolioBalance();
        stay.setFolioBalance(current.add(FolioItemServiceImpl.signedAmount(request.getChargeType(), request.getAmount())));
        StayRecord updated = stayRecordRepository.save(stay);

        notify(stay.getGuestId(), "A charge of " + request.getAmount() + " (" + request.getChargeType()
                + ") was posted to your folio.");

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

        // Sum all folio items (a DISCOUNT reduces the total).
        List<FolioItem> items = folioItemRepository.findByStayRecord_StayId(stayId);
        BigDecimal total = items.stream()
                .map(fi -> FolioItemServiceImpl.signedAmount(fi.getChargeType(), fi.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stay.setFolioBalance(total);
        stay.setActualCheckOut(LocalDateTime.now());
        stay.setStatus(StayStatus.CHECKEDOUT);
        StayRecord updated = stayRecordRepository.save(stay);

        // Update room status to AVAILABLE
        try {
            roomServiceClient.updateRoomStatus(stay.getAssignedRoomId(), "AVAILABLE");
        } catch (Exception e) {
            log.error("Failed to update room {} status to AVAILABLE on check-out", stay.getAssignedRoomId(), e);
        }

        // Update reservation status to CHECKEDOUT
        try {
            reservationServiceClient.updateReservationStatus(stay.getReservationId(), "CHECKEDOUT");
        } catch (Exception e) {
            log.warn("Failed to update reservation status: {}", e.getMessage());
        }

        notify(stay.getGuestId(), "You have been checked out. Final folio total: " + total + ".");

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
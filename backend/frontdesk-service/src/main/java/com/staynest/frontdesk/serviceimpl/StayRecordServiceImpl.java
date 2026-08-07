package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.audit.AuditRecorder;
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
import com.staynest.frontdesk.client.HousekeepingServiceClient;
import com.staynest.frontdesk.client.NotificationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayRecordServiceImpl implements StayRecordService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "STAY";

    private final AuditRecorder auditRecorder;
    private final StayRecordRepository stayRecordRepository;
    private final FolioItemRepository folioItemRepository;
    private final RoomServiceClient roomServiceClient;
    private final ReservationServiceClient reservationServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    /** Optional so check-out still works if housekeeping-service is unreachable. */
    @Autowired(required = false)
    private HousekeepingServiceClient housekeepingServiceClient;

    /** Fire-and-forget notification; a failure here must never fail the primary action. */
    private void notify(Integer userId, String message) {
        if (userId == null) return;
        try {
            notificationServiceClient.create(Map.of(
                    "userId", userId, "category", "FRONTDESK", "message", message));
        } catch (Exception e) {
            log.warn("Failed to send FRONTDESK notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Notifies the guest behind a stay.
     *
     * A stay carries a guestId, which is a reservation-service profile id — not the IAM userId
     * notifications are addressed by. These two were being used interchangeably, so check-in,
     * folio and check-out messages went to whichever account happened to share the number and
     * the guest saw none of them. Resolved through the guest profile, which now records it.
     */
    private void notifyGuest(Integer guestId, String message) {
        if (guestId == null) return;
        Integer userId = null;
        try {
            var resp = reservationServiceClient.getGuestById(guestId);
            Object id = resp != null && resp.getData() != null ? resp.getData().get("userId") : null;
            if (id instanceof Number n) {
                userId = n.intValue();
            }
        } catch (Exception e) {
            log.warn("Could not resolve the account for guest {}, so no notification was sent: {}",
                    guestId, e.getMessage());
            return;
        }
        if (userId == null) {
            // A walk-in profile with no login. Nothing to deliver to, and guessing is what
            // caused the mis-delivery in the first place.
            log.info("Guest {} has no linked account; skipping notification", guestId);
            return;
        }
        notify(userId, message);
    }

    @Override
    @Transactional
    public StayRecordResponse checkIn(CheckInRequest request) {
        // Must be the reservation's own guestId. This used to default to 1, which filed the stay
        // under an unrelated guest — the real guest then saw "no active stay" after check-in.
        Integer guestId = null;
        try {
            var res = reservationServiceClient.getReservationById(request.getReservationId());
            if (res != null && res.getData() instanceof Map<?, ?> map) {
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

        if (guestId == null) {
            throw new BadRequestException("Reservation " + request.getReservationId()
                    + " has no guest attached, so the stay cannot be filed against a guest.");
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

        notifyGuest(guestId, "Welcome! You are checked in to room #" + request.getRoomId() + ".");

        log.info("Check-in completed for reservation: {}", request.getReservationId());
        auditRecorder.record("CHECKIN", ENTITY, saved.getStayId());
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

        notifyGuest(stay.getGuestId(), "A charge of " + request.getAmount() + " (" + request.getChargeType()
                + ") was posted to your folio.");

        log.info("Folio item posted for stay: {}, amount: {}", stayId, request.getAmount());
        auditRecorder.record("POST_CHARGE", ENTITY, stayId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public StayRecordResponse checkOut(Integer stayId, Integer housekeepingStaffId) {
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

        // Raise the post-checkout cleaning task BEFORE the room is released. A CHECKOUT task only
        // applies to a room a guest was in, and housekeeping-service enforces that by reading the
        // room's status — so this has to happen while the room is still OCCUPIED.
        // Fire-and-forget — a housekeeping outage must not block a guest from checking out.
        try {
            if (housekeepingServiceClient == null) {
                log.warn("housekeeping-service client unavailable; no CHECKOUT task raised for room {}",
                        stay.getAssignedRoomId());
            } else if (housekeepingStaffId == null) {
                log.warn("No housekeeping assignee supplied at check-out of stay {}; no CHECKOUT task "
                        + "raised for room {} (tasks may not be left unassigned)", stayId, stay.getAssignedRoomId());
            } else {
                housekeepingServiceClient.createTask(Map.of(
                        "roomId", stay.getAssignedRoomId(),
                        "taskType", "CHECKOUT",
                        "assignedToId", housekeepingStaffId));
                log.info("CHECKOUT housekeeping task raised for room {}, assigned to staff {}",
                        stay.getAssignedRoomId(), housekeepingStaffId);
            }
        } catch (Exception e) {
            log.error("Failed to raise CHECKOUT housekeeping task for room {}", stay.getAssignedRoomId(), e);
        }

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

        notifyGuest(stay.getGuestId(), "You have been checked out. Final folio total: " + total + ".");

        log.info("Check-out completed for stay: {}, total folio: {}", stayId, total);
        auditRecorder.record("CHECKOUT", ENTITY, stayId);
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
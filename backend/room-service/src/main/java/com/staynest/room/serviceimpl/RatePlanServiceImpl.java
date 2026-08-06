package com.staynest.room.serviceimpl;

import com.staynest.room.audit.AuditRecorder;
import com.staynest.room.dto.RatePlanRequest;
import com.staynest.room.dto.RatePlanResponse;
import com.staynest.room.entity.RatePlan;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.RatePlanRepository;
import com.staynest.room.repository.RoomTypeRepository;
import com.staynest.room.service.RatePlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatePlanServiceImpl implements RatePlanService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "RATEPLAN";

    private final AuditRecorder auditRecorder;

    private final RatePlanRepository ratePlanRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public RatePlanResponse createRatePlan(RatePlanRequest request) {
        RoomType roomType = requireRoomType(request.getRoomTypeId());
        validateRange(request);
        rejectOverlap(request, null);

        RatePlan ratePlan = new RatePlan();
        applyRequest(ratePlan, request, roomType);
        ratePlan.setStatus(RatePlanStatus.ACTIVE);

        RatePlan saved = ratePlanRepository.save(ratePlan);
        log.info("RatePlan created: {}", saved.getRatePlanId());
        auditRecorder.record("CREATE", ENTITY, saved.getRatePlanId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RatePlanResponse updateRatePlan(Integer id, RatePlanRequest request) {
        RatePlan ratePlan = requirePlan(id);
        RoomType roomType = requireRoomType(request.getRoomTypeId());
        validateRange(request);
        // Excluding itself, or every edit that leaves the dates alone would self-clash.
        rejectOverlap(request, id);

        applyRequest(ratePlan, request, roomType);

        RatePlan updated = ratePlanRepository.save(ratePlan);
        log.info("RatePlan {} updated", id);
        auditRecorder.record("UPDATE", ENTITY, id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRatePlan(Integer id) {
        RatePlan ratePlan = requirePlan(id);
        ratePlanRepository.delete(ratePlan);
        // Reservations hold ratePlanId as a plain column, with no foreign key back to here, so
        // this cannot be blocked on "is it in use" without asking reservation-service.
        // Deactivating is the safer option for a plan already sold; deleting is for one created
        // by mistake.
        log.warn("RatePlan {} deleted — existing reservations referencing it will keep the id", id);
        auditRecorder.record("DELETE", ENTITY, id);
    }

    @Override
    public RatePlanResponse getRatePlanById(Integer id) {
        return mapToResponse(requirePlan(id));
    }

    @Override
    public List<RatePlanResponse> getAllRatePlans() {
        return ratePlanRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RatePlanResponse> getActivePlansForRoomType(Integer roomTypeId, LocalDate date) {
        return ratePlanRepository
                .findByRoomType_RoomTypeIdAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
                        roomTypeId, RatePlanStatus.ACTIVE, date, date)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RatePlanResponse updateStatus(Integer id, RatePlanStatus status) {
        RatePlan ratePlan = requirePlan(id);
        ratePlan.setStatus(status);
        RatePlan updated = ratePlanRepository.save(ratePlan);
        log.info("RatePlan {} status updated to {}", id, status);
        auditRecorder.record("UPDATE_STATUS", ENTITY, id);
        return mapToResponse(updated);
    }

    // ------------------------------------------------------------------ internals --

    private RatePlan requirePlan(Integer id) {
        return ratePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RatePlan not found: " + id));
    }

    private RoomType requireRoomType(Integer roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + roomTypeId));
    }

    /** The fields create and update both write, kept in one place so they cannot drift apart. */
    private void applyRequest(RatePlan ratePlan, RatePlanRequest request, RoomType roomType) {
        ratePlan.setRoomType(roomType);
        ratePlan.setName(request.getName());
        ratePlan.setPricePerNight(request.getPricePerNight());
        ratePlan.setValidFrom(request.getValidFrom());
        ratePlan.setValidTo(request.getValidTo());
        // Absent means "no meals", not null — the column is a plain Boolean.
        ratePlan.setMealPlanIncluded(Boolean.TRUE.equals(request.getMealPlanIncluded()));
    }

    private void validateRange(RatePlanRequest request) {
        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new BadRequestException("validTo must be on or after validFrom");
        }
    }

    /**
     * Two ACTIVE plans of the same name for the same room type must not cover any of the same
     * days — otherwise availability search has two prices for one night and picks arbitrarily.
     *
     * @param exceptId the plan being edited, which must not clash with itself
     */
    private void rejectOverlap(RatePlanRequest request, Integer exceptId) {
        ratePlanRepository.findActiveOverlapping(
                        request.getRoomTypeId(), request.getName(),
                        request.getValidFrom(), request.getValidTo())
                .stream()
                .filter(rp -> !rp.getRatePlanId().equals(exceptId))
                .findFirst()
                .ifPresent(clash -> {
                    throw new BadRequestException("A " + request.getName()
                            + " rate plan for this room type already covers "
                            + clash.getValidFrom() + " to " + clash.getValidTo()
                            + ". Change the dates, or edit that plan instead.");
                });
    }

    private RatePlanResponse mapToResponse(RatePlan rp) {
        RatePlanResponse response = new RatePlanResponse();
        response.setRatePlanId(rp.getRatePlanId());
        response.setRoomTypeId(rp.getRoomType().getRoomTypeId());
        response.setRoomTypeName(rp.getRoomType().getName().name());
        response.setName(rp.getName());
        response.setPricePerNight(rp.getPricePerNight());
        response.setValidFrom(rp.getValidFrom());
        response.setValidTo(rp.getValidTo());
        response.setMealPlanIncluded(rp.getMealPlanIncluded());
        response.setStatus(rp.getStatus());
        return response;
    }
}

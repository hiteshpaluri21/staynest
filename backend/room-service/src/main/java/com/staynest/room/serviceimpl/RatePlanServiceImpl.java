package com.staynest.room.serviceimpl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RatePlanServiceImpl implements RatePlanService {

	private static final Logger log = LoggerFactory.getLogger(RatePlanServiceImpl.class);

	@Autowired
	private RatePlanRepository ratePlanRepository;

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Override
	public RatePlanResponse createRatePlan(RatePlanRequest request) {
		RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
				.orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId()));

		validateRange(request);
		rejectOverlap(request, null);

		RatePlan ratePlan = new RatePlan();
		ratePlan.setRoomType(roomType);
		ratePlan.setName(request.getName());
		ratePlan.setPricePerNight(request.getPricePerNight());
		ratePlan.setValidFrom(request.getValidFrom());
		ratePlan.setValidTo(request.getValidTo());
		ratePlan.setMealPlanIncluded(Boolean.TRUE.equals(request.getMealPlanIncluded()));
		ratePlan.setStatus(RatePlanStatus.ACTIVE);

		RatePlan saved = ratePlanRepository.save(ratePlan);
		log.info("RatePlan created: {}", saved.getRatePlanId());
		return mapToResponse(saved);
	}

	@Override
	public RatePlanResponse updateRatePlan(Integer id, RatePlanRequest request) {
		RatePlan ratePlan = ratePlanRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RatePlan not found: " + id));
		RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
				.orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId()));

		validateRange(request);
		// Excluding itself, or every edit that leaves the dates alone would self-clash.
		rejectOverlap(request, id);

		ratePlan.setRoomType(roomType);
		ratePlan.setName(request.getName());
		ratePlan.setPricePerNight(request.getPricePerNight());
		ratePlan.setValidFrom(request.getValidFrom());
		ratePlan.setValidTo(request.getValidTo());
		ratePlan.setMealPlanIncluded(Boolean.TRUE.equals(request.getMealPlanIncluded()));

		RatePlan updated = ratePlanRepository.save(ratePlan);
		log.info("RatePlan {} updated", id);
		return mapToResponse(updated);
	}

	@Override
	public void deleteRatePlan(Integer id) {
		RatePlan ratePlan = ratePlanRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RatePlan not found: " + id));
		ratePlanRepository.delete(ratePlan);
		// Reservations hold ratePlanId as a plain column, with no foreign key back to
		// here, so this cannot be blocked on "is it in use" without asking
		// reservation-service. Deactivating is the safer option for a plan already sold;
		// deleting is for one created by mistake.
		log.warn("RatePlan {} deleted — existing reservations referencing it will keep the id", id);
	}

	private void validateRange(RatePlanRequest request) {
		if (request.getValidTo().isBefore(request.getValidFrom())) {
			throw new BadRequestException("validTo must be on or after validFrom");
		}
	}

	/**
	 * Two ACTIVE plans of the same name for the same room type must not cover any of the
	 * same days — otherwise availability search has two prices for one night and picks
	 * arbitrarily.
	 *
	 * @param exceptId the plan being edited, which must not clash with itself
	 */
	private void rejectOverlap(RatePlanRequest request, Integer exceptId) {
		List<RatePlan> clashes = ratePlanRepository.findActiveOverlapping(
				request.getRoomTypeId(), request.getName(),
				request.getValidFrom(), request.getValidTo());

		clashes.stream()
				.filter(rp -> !rp.getRatePlanId().equals(exceptId))
				.findFirst()
				.ifPresent(clash -> {
					throw new BadRequestException("A " + request.getName()
							+ " rate plan for this room type already covers "
							+ clash.getValidFrom() + " to " + clash.getValidTo()
							+ ". Change the dates, or edit that plan instead.");
				});
	}

	@Override
	public RatePlanResponse getRatePlanById(Integer id) {
		RatePlan ratePlan = ratePlanRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RatePlan not found: " + id));
		return mapToResponse(ratePlan);
	}

	@Override
	public List<RatePlanResponse> getAllRatePlans() {
		return ratePlanRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public List<RatePlanResponse> getActivePlansForRoomType(Integer roomTypeId, LocalDate date) {
		List<RatePlan> plans = ratePlanRepository
				.findByRoomType_RoomTypeIdAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(roomTypeId,
						RatePlanStatus.ACTIVE, date, date);
		return plans.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public RatePlanResponse updateStatus(Integer id, RatePlanStatus status) {
		RatePlan ratePlan = ratePlanRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RatePlan not found: " + id));
		ratePlan.setStatus(status);
		RatePlan updated = ratePlanRepository.save(ratePlan);
		log.info("RatePlan {} status updated to {}", id, status);
		return mapToResponse(updated);
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
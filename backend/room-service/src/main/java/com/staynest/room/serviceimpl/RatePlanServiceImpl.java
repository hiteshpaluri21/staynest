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

		RatePlan ratePlan = new RatePlan();
		ratePlan.setRoomType(roomType);
		ratePlan.setName(request.getName());
		ratePlan.setPricePerNight(request.getPricePerNight());
		ratePlan.setValidFrom(request.getValidFrom());
		ratePlan.setValidTo(request.getValidTo());
		ratePlan.setMealPlanIncluded(request.getMealPlanIncluded());
		ratePlan.setStatus(RatePlanStatus.ACTIVE);

		RatePlan saved = ratePlanRepository.save(ratePlan);
		log.info("RatePlan created: {}", saved.getRatePlanId());
		return mapToResponse(saved);
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
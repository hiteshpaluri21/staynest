package com.staynest.room.serviceimpl;

import com.staynest.room.audit.AuditRecorder;
import com.staynest.room.dto.RoomTypeRequest;
import com.staynest.room.dto.RoomTypeResponse;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.RoomTypeRepository;
import com.staynest.room.service.RoomTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {

    /** entityType recorded in audit_logs for everything in this service. */
    private static final String ENTITY = "ROOMTYPE";

    @Autowired
    private AuditRecorder auditRecorder;

	private static final Logger log = LoggerFactory.getLogger(RoomTypeServiceImpl.class);

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Override
	@Transactional
	public RoomTypeResponse createRoomType(RoomTypeRequest request) {
		checkDuplicate(request, null);

		RoomType roomType = new RoomType();
		roomType.setName(request.getName());
		roomType.setBedConfiguration(request.getBedConfiguration());
		roomType.setMaxOccupancy(request.getMaxOccupancy());
		roomType.setBaseRate(request.getBaseRate());
		roomType.setAmenitiesList(request.getAmenitiesList());
		roomType.setStatus(RatePlanStatus.ACTIVE);

		RoomType saved = roomTypeRepository.save(roomType);
		log.info("RoomType created: {}", saved.getRoomTypeId());
		auditRecorder.record("CREATE", ENTITY, saved.getRoomTypeId());
		return mapToResponse(saved);
	}

	@Override
	public List<RoomTypeResponse> getAllRoomTypes() {
		return roomTypeRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public RoomTypeResponse getRoomTypeById(Integer id) {
		RoomType roomType = roomTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RoomType not found: " + id));
		return mapToResponse(roomType);
	}

	@Override
	@Transactional
	public RoomTypeResponse updateRoomType(Integer id, RoomTypeRequest request) {
		RoomType roomType = roomTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RoomType not found: " + id));
		checkDuplicate(request, id);

		roomType.setName(request.getName());
		roomType.setBedConfiguration(request.getBedConfiguration());
		roomType.setMaxOccupancy(request.getMaxOccupancy());
		roomType.setBaseRate(request.getBaseRate());
		roomType.setAmenitiesList(request.getAmenitiesList());

		RoomType updated = roomTypeRepository.save(roomType);
		log.info("RoomType updated: {}", id);
		auditRecorder.record("UPDATE", ENTITY, id);
		return mapToResponse(updated);
	}

	@Override
	@Transactional
	public RoomTypeResponse updateStatus(Integer id, RatePlanStatus status) {
		RoomType roomType = roomTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("RoomType not found: " + id));
		roomType.setStatus(status);
		RoomType updated = roomTypeRepository.save(roomType);
		log.info("RoomType {} status updated to {}", id, status);
		auditRecorder.record("UPDATE_STATUS", ENTITY, id);
		return mapToResponse(updated);
	}

	/**
	 * Rejects a room type whose name and amenities duplicate an existing one. When updating,
	 * {@code excludeId} is the row being edited so it does not count as its own duplicate.
	 */
	private void checkDuplicate(RoomTypeRequest request, Integer excludeId) {
		String normalizedAmenities = normalizeAmenities(request.getAmenitiesList());
		boolean duplicate = roomTypeRepository.findByName(request.getName()).stream()
				.filter(rt -> excludeId == null || !excludeId.equals(rt.getRoomTypeId()))
				.anyMatch(rt -> normalizeAmenities(rt.getAmenitiesList()).equals(normalizedAmenities));
		if (duplicate) {
			throw new BadRequestException(
					"A room type '" + request.getName() + "' with the same amenities already exists");
		}
	}

	/**
	 * Normalizes a free-text, comma-separated amenities string so that ordering, casing and
	 * surrounding whitespace do not affect equality (e.g. "WiFi, AC" == "ac,wifi").
	 */
	private String normalizeAmenities(String amenities) {
		if (amenities == null || amenities.isBlank()) {
			return "";
		}
		return Arrays.stream(amenities.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(s -> s.toLowerCase())
				.collect(Collectors.toCollection(TreeSet::new))
				.stream()
				.collect(Collectors.joining(","));
	}

	private RoomTypeResponse mapToResponse(RoomType rt) {
		RoomTypeResponse response = new RoomTypeResponse();
		response.setRoomTypeId(rt.getRoomTypeId());
		response.setName(rt.getName());
		response.setBedConfiguration(rt.getBedConfiguration());
		response.setMaxOccupancy(rt.getMaxOccupancy());
		response.setBaseRate(rt.getBaseRate());
		response.setAmenitiesList(rt.getAmenitiesList());
		response.setStatus(rt.getStatus());
		return response;
	}
}
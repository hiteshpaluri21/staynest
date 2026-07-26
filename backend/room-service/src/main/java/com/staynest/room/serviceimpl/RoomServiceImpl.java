package com.staynest.room.serviceimpl;

import com.staynest.room.dto.RoomRequest;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.entity.Room;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RoomStatus;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.RoomRepository;
import com.staynest.room.repository.RoomTypeRepository;
import com.staynest.room.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Override
    public RoomResponse addRoom(RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new BadRequestException("Invalid RoomTypeId: " + request.getRoomTypeId()));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setRoomType(roomType);

        Room saved = roomRepository.save(room);
        log.info("Room created: {}", saved.getRoomId());
        return mapToResponse(saved);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoomById(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        return mapToResponse(room);
    }

    @Override
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getRoomsByType(Integer roomTypeId) {
        return roomRepository.findByRoomType_RoomTypeId(roomTypeId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public RoomResponse updateRoomStatus(Integer id, RoomStatus status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setStatus(status);
        Room updated = roomRepository.save(room);
        log.info("Room {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getRoomId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloor(room.getFloor());
        response.setRoomTypeId(room.getRoomType().getRoomTypeId());
        response.setRoomTypeName(room.getRoomType().getName().name());
        response.setStatus(room.getStatus());
        return response;
    }
}
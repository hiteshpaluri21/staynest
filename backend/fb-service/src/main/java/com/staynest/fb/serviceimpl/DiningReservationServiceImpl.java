package com.staynest.fb.serviceimpl;

import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.dto.DiningReservationResponse;
import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import com.staynest.fb.exception.ResourceNotFoundException;
import com.staynest.fb.repository.DiningReservationRepository;
import com.staynest.fb.service.DiningReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningReservationServiceImpl implements DiningReservationService {

    private final DiningReservationRepository reservationRepository;

    @Override
    public DiningReservationResponse createReservation(DiningReservationRequest request) {
        DiningReservation reservation = DiningReservation.builder()
                .guestId(request.getGuestId())
                .stayId(request.getStayId())
                .restaurantOutlet(request.getRestaurantOutlet())
                .date(request.getDate())
                .time(request.getTime())
                .covers(request.getCovers())
                .build();

        DiningReservation saved = reservationRepository.save(reservation);
        log.info("Dining reservation created: {}", saved.getDiningResId());
        return mapToResponse(saved);
    }

    @Override
    public DiningReservationResponse updateReservationStatus(Integer id, DiningResStatus status) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));
        reservation.setStatus(status);
        DiningReservation updated = reservationRepository.save(reservation);
        log.info("Dining reservation {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    @Override
    public DiningReservationResponse getReservationById(Integer id) {
        DiningReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining reservation not found: " + id));
        return mapToResponse(reservation);
    }

    @Override
    public List<DiningReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiningReservationResponse> getReservationsByGuestId(Integer guestId) {
        return reservationRepository.findByGuestId(guestId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiningReservationResponse> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByDate(date).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    private DiningReservationResponse mapToResponse(DiningReservation dr) {
        return DiningReservationResponse.builder()
                .diningResId(dr.getDiningResId())
                .guestId(dr.getGuestId())
                .stayId(dr.getStayId())
                .restaurantOutlet(dr.getRestaurantOutlet())
                .date(dr.getDate())
                .time(dr.getTime())
                .covers(dr.getCovers())
                .status(dr.getStatus())
                .build();
    }
}
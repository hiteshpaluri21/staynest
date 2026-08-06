package com.staynest.room.serviceimpl;

import com.staynest.room.audit.AuditRecorder;
import com.staynest.room.dto.RatePlanRequest;
import com.staynest.room.entity.RatePlan;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RatePlanName;
import com.staynest.room.enums.RatePlanStatus;
import com.staynest.room.enums.RoomTypeName;
import com.staynest.room.exception.BadRequestException;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.RatePlanRepository;
import com.staynest.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Two ACTIVE plans of the same name for one room type must not share any days, or
 * availability search has two prices for a night and picks arbitrarily.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RatePlanServiceImplTest {

    @Mock private RatePlanRepository ratePlanRepository;
    @Mock private AuditRecorder auditRecorder;
    @Mock private RoomTypeRepository roomTypeRepository;
    @InjectMocks private RatePlanServiceImpl service;

    private static RoomType deluxe() {
        RoomType t = new RoomType();
        t.setRoomTypeId(2);
        t.setName(RoomTypeName.DELUXE);
        return t;
    }

    private static RatePlanRequest request(String from, String to) {
        RatePlanRequest req = new RatePlanRequest();
        req.setRoomTypeId(2);
        req.setName(RatePlanName.SEASONAL);
        req.setPricePerNight(new BigDecimal("5000.00"));
        req.setValidFrom(LocalDate.parse(from));
        req.setValidTo(LocalDate.parse(to));
        req.setMealPlanIncluded(true);
        return req;
    }

    private static RatePlan plan(int id, String from, String to) {
        RatePlan rp = new RatePlan();
        rp.setRatePlanId(id);
        rp.setRoomType(deluxe());
        rp.setName(RatePlanName.SEASONAL);
        rp.setPricePerNight(new BigDecimal("4000.00"));
        rp.setValidFrom(LocalDate.parse(from));
        rp.setValidTo(LocalDate.parse(to));
        rp.setStatus(RatePlanStatus.ACTIVE);
        return rp;
    }

    private void roomTypeExists() {
        when(roomTypeRepository.findById(2)).thenReturn(Optional.of(deluxe()));
    }

    private void overlapsFound(List<RatePlan> clashes) {
        when(ratePlanRepository.findActiveOverlapping(anyInt(), any(RatePlanName.class),
                any(LocalDate.class), any(LocalDate.class))).thenReturn(clashes);
    }

    // ------------------------------------------------------------------- creating --

    @Test
    void aNonOverlappingPlanIsCreated() {
        roomTypeExists();
        overlapsFound(List.of());
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.createRatePlan(request("2026-06-01", "2026-06-30"));

        assertThat(saved.getValidFrom()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(saved.getStatus()).isEqualTo(RatePlanStatus.ACTIVE);
    }

    @Test
    void anOverlappingPlanIsRejected() {
        roomTypeExists();
        overlapsFound(List.of(plan(9, "2026-06-15", "2026-07-15")));

        assertThatThrownBy(() -> service.createRatePlan(request("2026-06-01", "2026-06-30")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already covers");

        verify(ratePlanRepository, never()).save(any());
    }

    @Test
    void validToBeforeValidFromIsRejected() {
        roomTypeExists();

        assertThatThrownBy(() -> service.createRatePlan(request("2026-06-30", "2026-06-01")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("validTo must be on or after validFrom");

        verify(ratePlanRepository, never()).save(any());
    }

    @Test
    void aSingleDayPlanIsAllowed() {
        roomTypeExists();
        overlapsFound(List.of());
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        // validFrom == validTo is a one-night plan, not an inverted range.
        var saved = service.createRatePlan(request("2026-06-01", "2026-06-01"));

        assertThat(saved.getValidTo()).isEqualTo(LocalDate.parse("2026-06-01"));
    }

    @Test
    void anUnknownRoomTypeIsRejected() {
        when(roomTypeRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRatePlan(request("2026-06-01", "2026-06-30")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid RoomTypeId");
    }

    @Test
    void aMissingMealPlanFlagBecomesFalseRatherThanNull() {
        roomTypeExists();
        overlapsFound(List.of());
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        RatePlanRequest req = request("2026-06-01", "2026-06-30");
        req.setMealPlanIncluded(null);

        assertThat(service.createRatePlan(req).getMealPlanIncluded()).isFalse();
    }

    // -------------------------------------------------------------------- editing --

    @Test
    void aPlanCanBeEditedWithoutClashingWithItself() {
        roomTypeExists();
        when(ratePlanRepository.findById(9)).thenReturn(Optional.of(plan(9, "2026-06-01", "2026-06-30")));
        // The only overlap is the plan being edited, which must be ignored.
        overlapsFound(List.of(plan(9, "2026-06-01", "2026-06-30")));
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.updateRatePlan(9, request("2026-06-01", "2026-07-05"));

        assertThat(updated.getValidTo()).isEqualTo(LocalDate.parse("2026-07-05"));
    }

    @Test
    void editingOntoAnotherPlansDatesIsRejected() {
        roomTypeExists();
        when(ratePlanRepository.findById(9)).thenReturn(Optional.of(plan(9, "2026-06-01", "2026-06-30")));
        overlapsFound(List.of(plan(9, "2026-06-01", "2026-06-30"), plan(11, "2026-08-01", "2026-08-31")));

        assertThatThrownBy(() -> service.updateRatePlan(9, request("2026-07-15", "2026-08-15")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already covers");
    }

    @Test
    void editingAnUnknownPlanIsRejected() {
        when(ratePlanRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRatePlan(404, request("2026-06-01", "2026-06-30")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------- deleting --

    @Test
    void deletingRemovesThePlan() {
        RatePlan existing = plan(9, "2026-06-01", "2026-06-30");
        when(ratePlanRepository.findById(9)).thenReturn(Optional.of(existing));

        service.deleteRatePlan(9);

        verify(ratePlanRepository).delete(existing);
    }

    @Test
    void deletingAnUnknownPlanIsRejected() {
        when(ratePlanRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRatePlan(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

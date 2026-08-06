package com.staynest.fb.serviceimpl;

import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.client.ReservationServiceClient;
import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.repository.DiningReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A guest must not hold two tables at the same outlet for the same slot, and a cancelled
 * booking must not block rebooking that slot.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiningReservationServiceImplTest {

    private static final String OUTLET = "Sky Lounge";
    private static final LocalTime SEVEN_PM = LocalTime.of(19, 0);

    @Mock private DiningReservationRepository reservationRepository;
    @Mock private AuditRecorder auditRecorder;
    @Mock private ReservationServiceClient reservationServiceClient;
    @Mock private FrontDeskServiceClient frontDeskServiceClient;
    @InjectMocks private DiningReservationServiceImpl service;

    private DiningReservationRequest request(LocalDate date) {
        DiningReservationRequest req = new DiningReservationRequest();
        req.setGuestId(4);
        req.setRestaurantOutlet(OUTLET);
        req.setDate(date);
        req.setTime(SEVEN_PM);
        req.setCovers(2);
        return req;
    }

    /** Guest validation goes out over Feign; a valid guest is the default for these tests. */
    private void guestExists() {
        when(reservationServiceClient.getGuestById(anyInt()))
                .thenReturn(ApiResponse.success(Map.of("guestId", 4)));
    }

    private void slotAlreadyHeld(boolean held) {
        when(reservationRepository.existsByGuestIdAndRestaurantOutletIgnoreCaseAndDateAndTimeAndStatusIn(
                anyInt(), anyString(), any(LocalDate.class), any(LocalTime.class), any(Collection.class)))
                .thenReturn(held);
    }

    @Test
    void aFreeSlotIsBooked() {
        guestExists();
        slotAlreadyHeld(false);
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.createReservation(request(LocalDate.now().plusDays(1)));

        assertThat(saved.getRestaurantOutlet()).isEqualTo(OUTLET);
        assertThat(saved.getCovers()).isEqualTo(2);
    }

    @Test
    void aSecondBookingForTheSameOutletAndSlotIsRejected() {
        guestExists();
        slotAlreadyHeld(true);

        assertThatThrownBy(() -> service.createReservation(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already have a table");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void onlyLiveBookingsBlockTheSlot() {
        guestExists();
        slotAlreadyHeld(false);
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        // CANCELLED / COMPLETED / NOSHOW are excluded, so cancelling frees the slot again.
        verify(reservationRepository).existsByGuestIdAndRestaurantOutletIgnoreCaseAndDateAndTimeAndStatusIn(
                eq(4), eq(OUTLET), any(LocalDate.class), eq(SEVEN_PM),
                eq(java.util.List.of(DiningResStatus.CONFIRMED, DiningResStatus.SEATED)));
    }

    @Test
    void aDateInThePastIsRejectedBeforeAnythingElseHappens() {
        assertThatThrownBy(() -> service.createReservation(request(LocalDate.now().minusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");

        verify(reservationRepository, never()).save(any());
    }

    // ------------------------------------------------------------------- cancelling --

    @Test
    void aConfirmedBookingCanBeCancelled() {
        DiningReservation booking = DiningReservation.builder()
                .diningResId(3).guestId(4).restaurantOutlet(OUTLET)
                .date(LocalDate.now().plusDays(1)).time(SEVEN_PM).covers(2)
                .status(DiningResStatus.CONFIRMED)
                .build();
        when(reservationRepository.findById(3)).thenReturn(Optional.of(booking));
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.cancelReservation(3).getStatus()).isEqualTo(DiningResStatus.CANCELLED);
    }

    @Test
    void aSeatedBookingCannotBeCancelled() {
        DiningReservation booking = DiningReservation.builder()
                .diningResId(3).guestId(4).restaurantOutlet(OUTLET)
                .date(LocalDate.now()).time(SEVEN_PM).covers(2)
                .status(DiningResStatus.SEATED)
                .build();
        when(reservationRepository.findById(3)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelReservation(3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void cancellingTwiceIsRejected() {
        DiningReservation booking = DiningReservation.builder()
                .diningResId(3).guestId(4).restaurantOutlet(OUTLET)
                .date(LocalDate.now()).time(SEVEN_PM).covers(2)
                .status(DiningResStatus.CANCELLED)
                .build();
        when(reservationRepository.findById(3)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelReservation(3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }
}

package com.staynest.reservation.serviceimpl;

import com.staynest.reservation.audit.AuditRecorder;
import com.staynest.reservation.client.IamServiceClient;
import com.staynest.reservation.client.NotificationServiceClient;
import com.staynest.reservation.client.RoomServiceClient;
import com.staynest.reservation.dto.ApiResponse;
import com.staynest.reservation.dto.ReservationRequest;
import com.staynest.reservation.dto.ReservationResponse;
import com.staynest.reservation.entity.GuestProfile;
import com.staynest.reservation.entity.Reservation;
import com.staynest.reservation.enums.BookingChannel;
import com.staynest.reservation.enums.GuestStatus;
import com.staynest.reservation.enums.LoyaltyTier;
import com.staynest.reservation.enums.ReservationStatus;
import com.staynest.reservation.exception.BadRequestException;
import com.staynest.reservation.repository.GuestProfileRepository;
import com.staynest.reservation.repository.ReservationRepository;
import com.staynest.reservation.service.GuestUserResolver;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Booking and cancelling.
 *
 * Two rules are load-bearing. Nights are derived from the dates rather than taken from the
 * request, so a client cannot under-report the length of stay. And a guest who has already
 * arrived cannot have their booking cancelled out from under them — front desk checks them out
 * instead.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceImplTest {

    private static final int RESERVATION_ID = 55;
    private static final int GUEST_ID = 4;
    private static final int USER_ID = 7;
    private static final int ROOM_TYPE_ID = 2;
    private static final int RATE_PLAN_ID = 9;

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(7);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Mock private AuditRecorder auditRecorder;
    @Mock private ReservationRepository reservationRepository;
    @Mock private GuestProfileRepository guestProfileRepository;
    @Mock private RoomServiceClient roomServiceClient;
    @Mock private IamServiceClient iamServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private GuestUserResolver guestUserResolver;
    @InjectMocks private ReservationServiceImpl service;

    private static GuestProfile guest() {
        GuestProfile gp = new GuestProfile();
        gp.setGuestId(GUEST_ID);
        gp.setName("Asha Menon");
        gp.setEmail("asha@staynest.example");
        gp.setStatus(GuestStatus.ACTIVE);
        gp.setLoyaltyTier(LoyaltyTier.NONE);
        return gp;
    }

    private static ReservationRequest request() {
        ReservationRequest req = new ReservationRequest();
        req.setGuestId(GUEST_ID);
        req.setRoomTypeId(ROOM_TYPE_ID);
        req.setRatePlanId(RATE_PLAN_ID);
        req.setCheckInDate(CHECK_IN);
        req.setCheckOutDate(CHECK_OUT);
        // Deliberately wrong: the service must derive 3 nights from the dates instead.
        req.setNights(1);
        req.setAdults(2);
        req.setChildren(0);
        req.setTotalAmount(new BigDecimal("12000.00"));
        req.setBookingChannel(BookingChannel.DIRECT);
        return req;
    }

    private static Reservation reservation(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setReservationId(RESERVATION_ID);
        r.setGuest(guest());
        r.setRoomTypeId(ROOM_TYPE_ID);
        r.setRatePlanId(RATE_PLAN_ID);
        r.setCheckInDate(CHECK_IN);
        r.setCheckOutDate(CHECK_OUT);
        r.setNights(3);
        r.setAdults(2);
        r.setChildren(0);
        r.setTotalAmount(new BigDecimal("12000.00"));
        r.setBookingChannel(BookingChannel.DIRECT);
        r.setStatus(status);
        return r;
    }

    /**
     * room-service confirms the type and the rate plan, and reports enough physical rooms that
     * the availability check passes. Stubbed with doReturn because both calls are declared as a
     * wildcard ApiResponse&lt;?&gt;.
     */
    private void roomServiceAgrees() {
        doReturn(ApiResponse.success(Map.of("roomTypeId", ROOM_TYPE_ID, "maxOccupancy", 2)))
                .when(roomServiceClient).getRoomTypeById(ROOM_TYPE_ID);
        doReturn(ApiResponse.success(Map.of("ratePlanId", RATE_PLAN_ID)))
                .when(roomServiceClient).getRatePlanById(RATE_PLAN_ID);
        when(roomServiceClient.getAllRooms()).thenReturn(ApiResponse.success(List.of(
                Map.of("roomId", 1, "roomTypeId", ROOM_TYPE_ID, "status", "AVAILABLE"),
                Map.of("roomId", 2, "roomTypeId", ROOM_TYPE_ID, "status", "AVAILABLE"))));
        when(reservationRepository.findOverlappingReservations(anyInt(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    @Test
    void createReservation_valid() {
        when(guestProfileRepository.findById(GUEST_ID)).thenReturn(Optional.of(guest()));
        roomServiceAgrees();
        when(guestUserResolver.userIdFor(any(GuestProfile.class))).thenReturn(USER_ID);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(RESERVATION_ID);
            return r;
        });

        ReservationResponse created = service.createReservation(request());

        assertThat(created.getReservationId()).isEqualTo(RESERVATION_ID);
        assertThat(created.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        // Derived from the dates, not the 1 the request claimed.
        assertThat(created.getNights()).isEqualTo(3);
        verify(auditRecorder).record("CREATE", "RESERVATION", RESERVATION_ID);
    }

    @Test
    void cancelReservation_confirmed_becomesCancelled() {
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.CONFIRMED)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestUserResolver.userIdFor(any(GuestProfile.class))).thenReturn(USER_ID);

        ReservationResponse cancelled = service.cancelReservation(RESERVATION_ID);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(auditRecorder).record("CANCEL", "RESERVATION", RESERVATION_ID);
    }

    /** The guest is already in the room, so front desk has to check them out, not cancel. */
    @Test
    void cancelReservation_checkedIn_throwsException() {
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.CHECKEDIN)));

        assertThatThrownBy(() -> service.cancelReservation(RESERVATION_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel a checked-in reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservationStatus_changes() {
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.CONFIRMED)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse updated =
                service.updateReservationStatus(RESERVATION_ID, ReservationStatus.CHECKEDIN);

        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CHECKEDIN);
        verify(auditRecorder).record("UPDATE_STATUS", "RESERVATION", RESERVATION_ID);
    }
}

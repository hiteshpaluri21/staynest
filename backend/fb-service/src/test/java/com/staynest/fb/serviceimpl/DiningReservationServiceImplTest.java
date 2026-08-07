package com.staynest.fb.serviceimpl;

import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.client.FrontDeskServiceClient;
import com.staynest.fb.client.IamServiceClient;
import com.staynest.fb.client.NotificationServiceClient;
import com.staynest.fb.client.ReservationServiceClient;
import com.staynest.fb.dto.ApiResponse;
import com.staynest.fb.dto.DiningReservationRequest;
import com.staynest.fb.entity.DiningReservation;
import com.staynest.fb.enums.DiningResStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.repository.DiningReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An outlet is held exclusively for the length of a sitting: no second party may book an
 * overlapping window there, the same guest may not double-book, and a cancelled booking
 * must free the slot again.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiningReservationServiceImplTest {

    private static final String OUTLET = "Sky Lounge";
    private static final LocalTime SEVEN_PM = LocalTime.of(19, 0);

    /** Guest profile 4 belongs to IAM account 77 — deliberately different numbers. */
    private static final int GUEST_ID = 4;
    private static final int GUEST_ACCOUNT = 77;

    @Mock private DiningReservationRepository reservationRepository;
    @Mock private AuditRecorder auditRecorder;
    @Mock private ReservationServiceClient reservationServiceClient;
    @Mock private FrontDeskServiceClient frontDeskServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private IamServiceClient iamServiceClient;
    @InjectMocks private DiningReservationServiceImpl service;

    private DiningReservationRequest request(LocalDate date) {
        DiningReservationRequest req = new DiningReservationRequest();
        req.setGuestId(GUEST_ID);
        req.setRestaurantOutlet(OUTLET);
        req.setDate(date);
        req.setTime(SEVEN_PM);
        req.setCovers(2);
        return req;
    }

    /** Guest validation goes out over Feign; a valid guest is the default for these tests. */
    private void guestExists() {
        when(reservationServiceClient.getGuestById(anyInt()))
                .thenReturn(ApiResponse.success(Map.of("guestId", GUEST_ID, "userId", GUEST_ACCOUNT)));
    }

    /** A guest profile with no login, e.g. one staff typed in for a walk-in. */
    private void guestHasNoAccount() {
        when(reservationServiceClient.getGuestById(anyInt()))
                .thenReturn(ApiResponse.success(Map.of("guestId", GUEST_ID)));
    }

    /** The payload sent to notification-service for the given recipient. */
    private static Map<String, Object> notificationTo(int userId, String message) {
        return Map.of("userId", userId, "category", "FB", "message", message);
    }

    /** What the outlet already holds that day. An empty list means the outlet is free. */
    private void outletHolds(DiningReservation... existing) {
        when(reservationRepository.findByRestaurantOutletIgnoreCaseAndDateAndStatusIn(
                anyString(), any(LocalDate.class), any(Collection.class)))
                .thenReturn(List.of(existing));
    }

    /** A live booking by someone else, over the given window. */
    private DiningReservation booking(int guestId, LocalTime from, LocalTime to) {
        return DiningReservation.builder()
                .diningResId(9).guestId(guestId).restaurantOutlet(OUTLET)
                .date(LocalDate.now().plusDays(1)).time(from).endTime(to).covers(2)
                .status(DiningResStatus.CONFIRMED)
                .build();
    }

    @Test
    void aFreeSlotIsBooked() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.createReservation(request(LocalDate.now().plusDays(1)));

        assertThat(saved.getRestaurantOutlet()).isEqualTo(OUTLET);
        assertThat(saved.getCovers()).isEqualTo(2);
    }

    /** No end time supplied, so the sitting runs for the default 90 minutes. */
    @Test
    void anOmittedEndTimeDefaultsToAStandardSitting() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.createReservation(request(LocalDate.now().plusDays(1)));

        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(20, 30));
    }

    @Test
    void anEndTimeAtOrBeforeTheStartIsRejected() {
        guestExists();
        DiningReservationRequest req = request(LocalDate.now().plusDays(1));
        req.setEndTime(LocalTime.of(18, 0));

        assertThatThrownBy(() -> service.createReservation(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be after the start time");

        verify(reservationRepository, never()).save(any());
    }

    /** The point of the whole rule: one party holds the outlet, so another guest is turned away. */
    @Test
    void anotherGuestCannotBookAnOverlappingWindow() {
        guestExists();
        outletHolds(booking(11, LocalTime.of(18, 30), LocalTime.of(20, 0)));

        assertThatThrownBy(() -> service.createReservation(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is already booked");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void theSameGuestCannotDoubleBookTheSameWindow() {
        guestExists();
        outletHolds(booking(4, SEVEN_PM, LocalTime.of(20, 30)));

        assertThatThrownBy(() -> service.createReservation(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already have a table");

        verify(reservationRepository, never()).save(any());
    }

    /** Half-open: a sitting that ends exactly as the next begins is not a clash. */
    @Test
    void abuttingSittingsDoNotClash() {
        guestExists();
        outletHolds(booking(11, LocalTime.of(17, 30), SEVEN_PM));
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.createReservation(request(LocalDate.now().plusDays(1))).getTime())
                .isEqualTo(SEVEN_PM);
    }

    @Test
    void onlyLiveBookingsBlockTheSlot() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        // CANCELLED / COMPLETED / NOSHOW are excluded, so cancelling frees the slot again.
        verify(reservationRepository).findByRestaurantOutletIgnoreCaseAndDateAndStatusIn(
                eq(OUTLET), any(LocalDate.class),
                eq(List.of(DiningResStatus.CONFIRMED, DiningResStatus.SEATED)));
    }

    // ----------------------------------------------------------------- notifications --

    /**
     * The regression: notifications are addressed by IAM userId, and the guestId is a different
     * key space. Sending the guestId delivered the confirmation to whichever unrelated account
     * shared that number, and the guest got nothing.
     */
    @Test
    void theGuestIsNotifiedByAccountIdNotGuestId() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        ArgumentCaptor<Map<String, Object>> sent = ArgumentCaptor.forClass(Map.class);
        verify(notificationServiceClient, atLeastOnce()).create(sent.capture());
        assertThat(sent.getAllValues())
                .as("the guest's account is notified")
                .anyMatch(n -> GUEST_ACCOUNT == (Integer) n.get("userId"))
                .as("the guest profile id is never used as a recipient")
                .noneMatch(n -> GUEST_ID == (Integer) n.get("userId"));
    }

    @Test
    void bookingATableConfirmsItToTheGuest() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        verify(notificationServiceClient).create(notificationTo(GUEST_ACCOUNT,
                "Your table for 2 at " + OUTLET + " is confirmed for "
                        + LocalDate.now().plusDays(1) + ", 19:00–20:30."));
    }

    /** F&B staff work the queue, so a new booking has to reach them too. */
    @Test
    void bookingATableAlertsFbStaff() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(iamServiceClient.getUsersByRole("FBMANAGER"))
                .thenReturn(ApiResponse.success(List.of(Map.of("userId", 12))));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        ArgumentCaptor<Map<String, Object>> sent = ArgumentCaptor.forClass(Map.class);
        verify(notificationServiceClient, atLeastOnce()).create(sent.capture());
        assertThat(sent.getAllValues()).anyMatch(n -> Integer.valueOf(12).equals(n.get("userId")));
    }

    /** Nobody to deliver to, and guessing is what caused the mis-delivery. */
    @Test
    void aGuestWithNoAccountIsNotNotifiedAtAll() {
        guestHasNoAccount();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReservation(request(LocalDate.now().plusDays(1)));

        verify(notificationServiceClient, never()).create(any());
    }

    /** A notification-service outage must not cost the guest their table. */
    @Test
    void aFailedNotificationDoesNotFailTheBooking() {
        guestExists();
        outletHolds();
        when(reservationRepository.save(any(DiningReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationServiceClient.create(any())).thenThrow(new RuntimeException("notification-service down"));

        assertThat(service.createReservation(request(LocalDate.now().plusDays(1))).getRestaurantOutlet())
                .isEqualTo(OUTLET);
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

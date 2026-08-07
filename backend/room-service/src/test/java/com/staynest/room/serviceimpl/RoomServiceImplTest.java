package com.staynest.room.serviceimpl;

import com.staynest.room.client.ReservationServiceClient;
import com.staynest.room.dto.ApiResponse;
import com.staynest.room.dto.RoomResponse;
import com.staynest.room.entity.Room;
import com.staynest.room.entity.RoomType;
import com.staynest.room.enums.RoomStatus;
import com.staynest.room.enums.RoomTypeName;
import com.staynest.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The advertised count of free rooms must fall by exactly one per booking, and stay there as
 * that booking moves from CONFIRMED to CHECKEDIN.
 *
 * It used to fall twice: the room left the AVAILABLE pool when check-in turned it OCCUPIED,
 * while its reservation still counted against the same room type.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceImplTest {

    private static final int DELUXE = 1;
    private static final String CHECK_IN = "2026-09-10";
    private static final String CHECK_OUT = "2026-09-12";

    @Mock private RoomRepository roomRepository;
    @Mock private ReservationServiceClient reservationServiceClient;
    @InjectMocks private RoomServiceImpl service;

    private static RoomType deluxe() {
        return RoomType.builder()
                .roomTypeId(DELUXE).name(RoomTypeName.DELUXE)
                .maxOccupancy(2).baseRate(new BigDecimal("4000.00"))
                .build();
    }

    private static Room room(int id, RoomStatus status) {
        return Room.builder()
                .roomId(id).roomNumber("10" + id).floor(1)
                .roomType(deluxe()).status(status)
                .build();
    }

    /** The hotel's rooms, whatever their current status. */
    private void hotelHas(Room... rooms) {
        when(roomRepository.findAll()).thenReturn(List.of(rooms));
    }

    /** A reservation of the deluxe type over the given dates. */
    private static Map<String, Object> reservation(String status, String from, String to) {
        Map<String, Object> res = new HashMap<>();
        res.put("roomTypeId", DELUXE);
        res.put("status", status);
        res.put("checkInDate", from);
        res.put("checkOutDate", to);
        return res;
    }

    private void reservationsAre(Map<String, Object>... reservations) {
        when(reservationServiceClient.getAllReservations(null))
                .thenReturn(ApiResponse.success(new ArrayList<>(List.of(reservations))));
    }

    private int freeCount() {
        List<RoomResponse> free = service.getAvailableRooms(CHECK_IN, CHECK_OUT);
        return free.size();
    }

    @Test
    void withNothingBookedEveryRoomIsFree() {
        hotelHas(room(1, RoomStatus.AVAILABLE), room(2, RoomStatus.AVAILABLE), room(3, RoomStatus.AVAILABLE));
        reservationsAre();

        assertThat(freeCount()).isEqualTo(3);
    }

    @Test
    void aConfirmedBookingTakesOneRoom() {
        hotelHas(room(1, RoomStatus.AVAILABLE), room(2, RoomStatus.AVAILABLE), room(3, RoomStatus.AVAILABLE));
        reservationsAre(reservation("CONFIRMED", CHECK_IN, CHECK_OUT));

        assertThat(freeCount()).isEqualTo(2);
    }

    /**
     * The regression. Checking that booking in turns its room OCCUPIED and its reservation
     * CHECKEDIN — one room gone, not two.
     */
    @Test
    void checkingInDoesNotTakeASecondRoom() {
        hotelHas(room(1, RoomStatus.OCCUPIED), room(2, RoomStatus.AVAILABLE), room(3, RoomStatus.AVAILABLE));
        reservationsAre(reservation("CHECKEDIN", CHECK_IN, CHECK_OUT));

        assertThat(freeCount()).isEqualTo(2);
    }

    @Test
    void aCancelledBookingFreesItsRoomAgain() {
        hotelHas(room(1, RoomStatus.AVAILABLE), room(2, RoomStatus.AVAILABLE));
        reservationsAre(reservation("CANCELLED", CHECK_IN, CHECK_OUT));

        assertThat(freeCount()).isEqualTo(2);
    }

    /** A room occupied today is still sellable for a window after that guest leaves. */
    @Test
    void aRoomOccupiedTodayIsFreeForALaterWindow() {
        hotelHas(room(1, RoomStatus.OCCUPIED), room(2, RoomStatus.AVAILABLE));
        reservationsAre(reservation("CHECKEDIN", "2026-08-01", "2026-08-03"));

        assertThat(freeCount()).isEqualTo(2);
    }

    /** A room being turned over between stays can still be let for the requested dates. */
    @Test
    void aRoomBeingCleanedCountsAsInventory() {
        hotelHas(room(1, RoomStatus.CLEANING), room(2, RoomStatus.AVAILABLE));
        reservationsAre();

        assertThat(freeCount()).isEqualTo(2);
    }

    @Test
    void roomsOutOfServiceAreNotSold() {
        hotelHas(room(1, RoomStatus.MAINTENANCE), room(2, RoomStatus.BLOCKED), room(3, RoomStatus.AVAILABLE));
        reservationsAre();

        assertThat(freeCount()).isEqualTo(1);
    }

    /** Half-open: a booking ending the morning this one starts does not clash. */
    @Test
    void aBookingEndingOnTheArrivalDayDoesNotClash() {
        hotelHas(room(1, RoomStatus.AVAILABLE));
        reservationsAre(reservation("CONFIRMED", "2026-09-08", CHECK_IN));

        assertThat(freeCount()).isEqualTo(1);
    }

    @Test
    void moreBookingsThanRoomsLeavesNoneRatherThanANegativeCount() {
        hotelHas(room(1, RoomStatus.AVAILABLE));
        reservationsAre(
                reservation("CONFIRMED", CHECK_IN, CHECK_OUT),
                reservation("CONFIRMED", CHECK_IN, CHECK_OUT));

        assertThat(freeCount()).isZero();
    }

    /** With no dates the question is what is free right now, which is the AVAILABLE pool. */
    @Test
    void withoutDatesOnlyCurrentlyAvailableRoomsAreReturned() {
        when(roomRepository.findByStatus(RoomStatus.AVAILABLE))
                .thenReturn(List.of(room(2, RoomStatus.AVAILABLE)));

        assertThat(service.getAvailableRooms(null, null)).hasSize(1);
    }
}

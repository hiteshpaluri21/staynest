package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.audit.AuditRecorder;
import com.staynest.frontdesk.client.HousekeepingServiceClient;
import com.staynest.frontdesk.client.NotificationServiceClient;
import com.staynest.frontdesk.client.ReservationServiceClient;
import com.staynest.frontdesk.client.RoomServiceClient;
import com.staynest.frontdesk.dto.ApiResponse;
import com.staynest.frontdesk.dto.CheckInRequest;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.dto.StayRecordResponse;
import com.staynest.frontdesk.entity.FolioItem;
import com.staynest.frontdesk.entity.StayRecord;
import com.staynest.frontdesk.enums.ChargeType;
import com.staynest.frontdesk.enums.StayStatus;
import com.staynest.frontdesk.repository.FolioItemRepository;
import com.staynest.frontdesk.repository.StayRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The arrival-to-departure path: a stay opens against the reservation's own guest and takes
 * its room out of the pool, charges accumulate on the folio while the guest is in-house, and
 * check-out closes the stay and releases the room.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StayRecordServiceImplTest {

    private static final int RESERVATION_ID = 55;
    private static final int ROOM_ID = 101;
    private static final int GUEST_ID = 7;
    private static final int STAY_ID = 1;
    private static final int HOUSEKEEPER_ID = 12;

    @Mock private AuditRecorder auditRecorder;
    @Mock private StayRecordRepository stayRecordRepository;
    @Mock private FolioItemRepository folioItemRepository;
    @Mock private RoomServiceClient roomServiceClient;
    @Mock private ReservationServiceClient reservationServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private HousekeepingServiceClient housekeepingServiceClient;
    @InjectMocks private StayRecordServiceImpl service;

    /*
     * housekeepingServiceClient is @Autowired(required = false) rather than a constructor
     * argument, so that check-out survives a housekeeping outage. @InjectMocks satisfies the
     * generated constructor and then stops, leaving that field null — it has to be set by hand.
     */
    @BeforeEach
    void wireOptionalClient() {
        ReflectionTestUtils.setField(service, "housekeepingServiceClient", housekeepingServiceClient);
    }

    private static CheckInRequest checkInRequest() {
        CheckInRequest req = new CheckInRequest();
        req.setReservationId(RESERVATION_ID);
        req.setRoomId(ROOM_ID);
        return req;
    }

    private static StayRecord stay(StayStatus status, String balance) {
        return StayRecord.builder()
                .stayId(STAY_ID)
                .reservationId(RESERVATION_ID)
                .guestId(GUEST_ID)
                .assignedRoomId(ROOM_ID)
                .folioBalance(new BigDecimal(balance))
                .status(status)
                .build();
    }

    private static FolioItem folioItem(ChargeType type, String amount) {
        return FolioItem.builder()
                .chargeType(type)
                .description("test charge")
                .amount(new BigDecimal(amount))
                .build();
    }

    /**
     * reservation-service answers with the reservation, carrying the guest it belongs to.
     * Stubbed with doReturn because the client declares a wildcard ApiResponse&lt;?&gt;.
     */
    private void reservationBelongsToGuest() {
        doReturn(ApiResponse.success(Map.of("guestId", GUEST_ID, "status", "CONFIRMED")))
                .when(reservationServiceClient).getReservationById(RESERVATION_ID);
    }

    @Test
    void checkIn_valid_createsStayAndUpdatesRoom() {
        reservationBelongsToGuest();
        when(stayRecordRepository.findByReservationId(RESERVATION_ID)).thenReturn(Optional.empty());
        when(stayRecordRepository.findByAssignedRoomIdAndStatus(ROOM_ID, StayStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(stayRecordRepository.save(any(StayRecord.class))).thenAnswer(inv -> {
            StayRecord s = inv.getArgument(0);
            s.setStayId(STAY_ID);
            return s;
        });

        StayRecordResponse opened = service.checkIn(checkInRequest());

        // Filed against the reservation's own guest, not a default.
        assertThat(opened.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(opened.getAssignedRoomId()).isEqualTo(ROOM_ID);
        assertThat(opened.getStatus()).isEqualTo(StayStatus.ACTIVE);
        assertThat(opened.getActualCheckIn()).isNotNull();
        assertThat(opened.getFolioBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // The room leaves the sellable pool and the booking is marked as arrived.
        verify(roomServiceClient).updateRoomStatus(ROOM_ID, "OCCUPIED");
        verify(reservationServiceClient).updateReservationStatus(RESERVATION_ID, "CHECKEDIN");
        verify(auditRecorder).record("CHECKIN", "STAY", STAY_ID);
    }

    @Test
    void postFolioItem_addsToFolio() {
        when(stayRecordRepository.findById(STAY_ID)).thenReturn(Optional.of(stay(StayStatus.ACTIVE, "500.00")));
        when(stayRecordRepository.save(any(StayRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        FolioItemRequest charge = new FolioItemRequest();
        charge.setChargeType(ChargeType.FBCHARGE);
        charge.setDescription("Room service");
        charge.setAmount(new BigDecimal("250.00"));

        StayRecordResponse updated = service.postFolioItem(STAY_ID, charge);

        verify(folioItemRepository).save(any(FolioItem.class));
        assertThat(updated.getFolioBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        verify(auditRecorder).record("POST_CHARGE", "STAY", STAY_ID);
    }

    @Test
    void checkOut_setsStatusAndFreesRoom() {
        when(stayRecordRepository.findById(STAY_ID)).thenReturn(Optional.of(stay(StayStatus.ACTIVE, "750.00")));
        when(stayRecordRepository.save(any(StayRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        // The closing total is recomputed from the folio lines, not read off the running balance.
        when(folioItemRepository.findByStayRecord_StayId(STAY_ID)).thenReturn(List.of(
                folioItem(ChargeType.ROOMRENT, "4000.00"),
                folioItem(ChargeType.FBCHARGE, "250.00"),
                folioItem(ChargeType.DISCOUNT, "250.00")));

        StayRecordResponse closed = service.checkOut(STAY_ID, HOUSEKEEPER_ID);

        assertThat(closed.getStatus()).isEqualTo(StayStatus.CHECKEDOUT);
        assertThat(closed.getActualCheckOut()).isNotNull();
        // A DISCOUNT line comes off the total rather than adding to it.
        assertThat(closed.getFolioBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));

        verify(roomServiceClient).updateRoomStatus(ROOM_ID, "AVAILABLE");
        verify(reservationServiceClient).updateReservationStatus(RESERVATION_ID, "CHECKEDOUT");
        // The cleaning task is raised while the room is still OCCUPIED, before it is released.
        verify(housekeepingServiceClient).createTask(Map.of(
                "roomId", ROOM_ID, "taskType", "CHECKOUT", "assignedToId", HOUSEKEEPER_ID));
        verify(auditRecorder).record("CHECKOUT", "STAY", STAY_ID);
    }
}

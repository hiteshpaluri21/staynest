package com.staynest.frontdesk.serviceimpl;

import com.staynest.frontdesk.audit.AuditRecorder;
import com.staynest.frontdesk.dto.FolioItemRequest;
import com.staynest.frontdesk.entity.FolioItem;
import com.staynest.frontdesk.entity.StayRecord;
import com.staynest.frontdesk.enums.ChargeType;
import com.staynest.frontdesk.enums.StayStatus;
import com.staynest.frontdesk.exception.BadRequestException;
import com.staynest.frontdesk.exception.ResourceNotFoundException;
import com.staynest.frontdesk.repository.FolioItemRepository;
import com.staynest.frontdesk.repository.StayRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the folio money path: the sign a charge carries, and the running balance kept on
 * the stay. These are the numbers a guest is billed from, so they are worth pinning down.
 */
@ExtendWith(MockitoExtension.class)
class FolioItemServiceImplTest {

    @Mock private FolioItemRepository folioItemRepository;
    @Mock private AuditRecorder auditRecorder;
    @Mock private StayRecordRepository stayRecordRepository;
    @InjectMocks private FolioItemServiceImpl service;

    private static StayRecord activeStay(BigDecimal balance) {
        StayRecord stay = new StayRecord();
        stay.setStayId(1);
        stay.setGuestId(7);
        stay.setStatus(StayStatus.ACTIVE);
        stay.setFolioBalance(balance);
        return stay;
    }

    private static FolioItemRequest request(ChargeType type, String amount) {
        FolioItemRequest req = new FolioItemRequest();
        req.setChargeType(type);
        req.setDescription("test charge");
        req.setAmount(new BigDecimal(amount));
        req.setPostedBy(42);
        return req;
    }

    // ---------------------------------------------------------------- signedAmount --

    @Test
    void discountIsNegated() {
        assertThat(FolioItemServiceImpl.signedAmount(ChargeType.DISCOUNT, new BigDecimal("250.00")))
                .isEqualByComparingTo("-250.00");
    }

    @Test
    void everyOtherChargeTypeAddsToTheBalance() {
        for (ChargeType type : ChargeType.values()) {
            if (type == ChargeType.DISCOUNT) continue;
            assertThat(FolioItemServiceImpl.signedAmount(type, new BigDecimal("100.00")))
                    .as("charge type %s should be positive", type)
                    .isEqualByComparingTo("100.00");
        }
    }

    // --------------------------------------------------------------- addFolioItem --

    @Test
    void addingAChargeIncreasesTheStayBalance() {
        StayRecord stay = activeStay(new BigDecimal("1000.00"));
        when(stayRecordRepository.findById(1)).thenReturn(Optional.of(stay));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addFolioItem(1, request(ChargeType.FBCHARGE, "450.50"));

        ArgumentCaptor<StayRecord> saved = ArgumentCaptor.forClass(StayRecord.class);
        verify(stayRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getFolioBalance()).isEqualByComparingTo("1450.50");
    }

    @Test
    void aDiscountReducesTheStayBalance() {
        StayRecord stay = activeStay(new BigDecimal("1000.00"));
        when(stayRecordRepository.findById(1)).thenReturn(Optional.of(stay));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addFolioItem(1, request(ChargeType.DISCOUNT, "200.00"));

        ArgumentCaptor<StayRecord> saved = ArgumentCaptor.forClass(StayRecord.class);
        verify(stayRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getFolioBalance()).isEqualByComparingTo("800.00");
    }

    @Test
    void aNullStartingBalanceIsTreatedAsZero() {
        StayRecord stay = activeStay(null);
        when(stayRecordRepository.findById(1)).thenReturn(Optional.of(stay));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addFolioItem(1, request(ChargeType.LAUNDRY, "75.00"));

        ArgumentCaptor<StayRecord> saved = ArgumentCaptor.forClass(StayRecord.class);
        verify(stayRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getFolioBalance()).isEqualByComparingTo("75.00");
    }

    @Test
    void chargesCannotBeAddedToACheckedOutStay() {
        StayRecord stay = activeStay(new BigDecimal("500.00"));
        stay.setStatus(StayStatus.CHECKEDOUT);
        when(stayRecordRepository.findById(1)).thenReturn(Optional.of(stay));

        assertThatThrownBy(() -> service.addFolioItem(1, request(ChargeType.FBCHARGE, "100.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("checked-out");

        verify(folioItemRepository, never()).save(any());
    }

    @Test
    void anUnknownStayIsRejected() {
        when(stayRecordRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFolioItem(99, request(ChargeType.SPA, "10.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------ updateFolioItem --

    @Test
    void editingAChargeRebalancesByTheDeltaOnly() {
        StayRecord stay = activeStay(new BigDecimal("1000.00"));
        FolioItem existing = FolioItem.builder()
                .folioItemId(5)
                .stayRecord(stay)
                .chargeType(ChargeType.FBCHARGE)
                .amount(new BigDecimal("200.00"))
                .build();
        when(folioItemRepository.findById(5)).thenReturn(Optional.of(existing));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // 200 becomes 350, so the balance should move by +150, not by +350.
        service.updateFolioItem(5, request(ChargeType.FBCHARGE, "350.00"));

        ArgumentCaptor<StayRecord> saved = ArgumentCaptor.forClass(StayRecord.class);
        verify(stayRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getFolioBalance()).isEqualByComparingTo("1150.00");
    }

    @Test
    void switchingAChargeToADiscountFlipsItsContribution() {
        StayRecord stay = activeStay(new BigDecimal("1000.00"));
        FolioItem existing = FolioItem.builder()
                .folioItemId(5)
                .stayRecord(stay)
                .chargeType(ChargeType.FBCHARGE)
                .amount(new BigDecimal("100.00"))
                .build();
        when(folioItemRepository.findById(5)).thenReturn(Optional.of(existing));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Removing a +100 charge and applying a -100 discount is a 200 swing.
        service.updateFolioItem(5, request(ChargeType.DISCOUNT, "100.00"));

        ArgumentCaptor<StayRecord> saved = ArgumentCaptor.forClass(StayRecord.class);
        verify(stayRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getFolioBalance()).isEqualByComparingTo("800.00");
    }
}

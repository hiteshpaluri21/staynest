package com.staynest.fb.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.fb.audit.AuditRecorder;
import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.entity.FBOrder;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.FoodType;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.enums.OrderStatus;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.repository.FBOrderRepository;
import com.staynest.fb.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The menu must not show the same dish twice (see rejectDuplicateName), and deleting a dish
 * must not pull it out from under an order the kitchen is still working.
 */
@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock private MenuItemRepository menuItemRepository;
    @Mock private FBOrderRepository orderRepository;
    @Mock private AuditRecorder auditRecorder;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private MenuItemServiceImpl service;

    private static MenuItemRequest request(String name) {
        MenuItemRequest req = new MenuItemRequest();
        req.setName(name);
        req.setCategory(MenuCategory.MAINCOURSE);
        req.setPrice(new BigDecimal("250.00"));
        req.setFoodType(FoodType.VEG);
        return req;
    }

    private static MenuItem existing(int id, String name) {
        return MenuItem.builder()
                .menuItemId(id)
                .name(name)
                .category(MenuCategory.BREAKFAST)
                .price(new BigDecimal("100.00"))
                .foodType(FoodType.VEG)
                .build();
    }

    /** An in-flight order whose cart holds the given menu item ids. */
    private static FBOrder orderFor(int... menuItemIds) {
        String lines = Arrays.stream(menuItemIds)
                .mapToObj(id -> "{\"itemId\":" + id + ",\"qty\":1}")
                .collect(Collectors.joining(",", "[", "]"));
        return FBOrder.builder().orderId(1).stayId(5).itemsJson(lines)
                .status(OrderStatus.PREPARING).build();
    }

    @Test
    void aNewNameIsAccepted() {
        when(menuItemRepository.findByNameIgnoreCase("Masala Dosa")).thenReturn(Optional.empty());
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse saved = service.addMenuItem(request("Masala Dosa"));

        assertThat(saved.getName()).isEqualTo("Masala Dosa");
    }

    @Test
    void aDuplicateNameIsRejected() {
        when(menuItemRepository.findByNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(existing(1, "Masala Dosa")));

        assertThatThrownBy(() -> service.addMenuItem(request("Masala Dosa")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void duplicateDetectionIgnoresCase() {
        // The lookup is case-insensitive, so a differently-cased name finds the same row.
        when(menuItemRepository.findByNameIgnoreCase("masala dosa"))
                .thenReturn(Optional.of(existing(1, "Masala Dosa")));

        assertThatThrownBy(() -> service.addMenuItem(request("masala dosa")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void surroundingAndRepeatedWhitespaceIsNormalisedBeforeSaving() {
        when(menuItemRepository.findByNameIgnoreCase("Masala Dosa")).thenReturn(Optional.empty());
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse saved = service.addMenuItem(request("  Masala   Dosa  "));

        // Both that it is stored tidy, and that the duplicate check used the tidy form.
        assertThat(saved.getName()).isEqualTo("Masala Dosa");
        verify(menuItemRepository).findByNameIgnoreCase("Masala Dosa");
    }

    @Test
    void anItemCanBeUpdatedWithoutRenamingIt() {
        MenuItem item = existing(7, "Club Sandwich");
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(item));
        // The only match is the item itself, which must not count as a clash.
        when(menuItemRepository.findByNameIgnoreCase("Club Sandwich")).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse updated = service.updateMenuItem(7, request("Club Sandwich"));

        assertThat(updated.getName()).isEqualTo("Club Sandwich");
    }

    @Test
    void renamingOntoAnotherItemsNameIsRejected() {
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(existing(7, "Club Sandwich")));
        when(menuItemRepository.findByNameIgnoreCase("Masala Dosa"))
                .thenReturn(Optional.of(existing(1, "Masala Dosa")));

        assertThatThrownBy(() -> service.updateMenuItem(7, request("Masala Dosa")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(menuItemRepository, never()).save(any());
    }

    // --------------------------------------------------------------------- deleting --

    @Test
    void anUnorderedItemIsDeleted() {
        MenuItem item = existing(7, "Club Sandwich");
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(item));
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());

        service.deleteMenuItem(7);

        verify(menuItemRepository).delete(item);
    }

    @Test
    void anItemOnAnUnfinishedOrderIsNotDeleted() {
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(existing(7, "Club Sandwich")));
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(orderFor(3, 7)));

        assertThatThrownBy(() -> service.deleteMenuItem(7))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mark it unavailable instead");

        verify(menuItemRepository, never()).delete(any(MenuItem.class));
    }

    /** An open order for other dishes must not block this one. */
    @Test
    void anOpenOrderForOtherItemsDoesNotBlockTheDelete() {
        MenuItem item = existing(7, "Club Sandwich");
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(item));
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(orderFor(3, 4)));

        service.deleteMenuItem(7);

        verify(menuItemRepository).delete(item);
    }

    /** Only in-flight orders are consulted — a billed or cancelled one keeps its stored total. */
    @Test
    void onlyUnfinishedOrdersAreChecked() {
        MenuItem item = existing(7, "Club Sandwich");
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(item));
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of());

        service.deleteMenuItem(7);

        verify(orderRepository).findByStatusIn(
                List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.SERVED));
    }

    /** A cart we cannot read might hold the item, so the delete is refused rather than risked. */
    @Test
    void anUnreadableCartBlocksTheDelete() {
        when(menuItemRepository.findById(7)).thenReturn(Optional.of(existing(7, "Club Sandwich")));
        FBOrder broken = FBOrder.builder().orderId(1).stayId(5).itemsJson("not json")
                .status(OrderStatus.PLACED).build();
        when(orderRepository.findByStatusIn(any())).thenReturn(List.of(broken));

        assertThatThrownBy(() -> service.deleteMenuItem(7))
                .isInstanceOf(BadRequestException.class);

        verify(menuItemRepository, never()).delete(any(MenuItem.class));
    }
}

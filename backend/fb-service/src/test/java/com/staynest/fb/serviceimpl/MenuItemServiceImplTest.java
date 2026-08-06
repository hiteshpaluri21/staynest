package com.staynest.fb.serviceimpl;

import com.staynest.fb.dto.MenuItemRequest;
import com.staynest.fb.dto.MenuItemResponse;
import com.staynest.fb.entity.MenuItem;
import com.staynest.fb.enums.MenuCategory;
import com.staynest.fb.exception.BadRequestException;
import com.staynest.fb.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The menu must not show the same dish twice — see rejectDuplicateName. */
@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock private MenuItemRepository menuItemRepository;
    @InjectMocks private MenuItemServiceImpl service;

    private static MenuItemRequest request(String name) {
        MenuItemRequest req = new MenuItemRequest();
        req.setName(name);
        req.setCategory(MenuCategory.MAINCOURSE);
        req.setPrice(new BigDecimal("250.00"));
        return req;
    }

    private static MenuItem existing(int id, String name) {
        return MenuItem.builder()
                .menuItemId(id)
                .name(name)
                .category(MenuCategory.BREAKFAST)
                .price(new BigDecimal("100.00"))
                .build();
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
}

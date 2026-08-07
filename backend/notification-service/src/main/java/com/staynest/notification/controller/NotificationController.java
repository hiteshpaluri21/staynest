package com.staynest.notification.controller;

import com.staynest.notification.audit.CurrentUser;
import com.staynest.notification.dto.ApiResponse;
import com.staynest.notification.dto.NotificationRequest;
import com.staynest.notification.dto.NotificationResponse;
import com.staynest.notification.dto.UnreadCountResponse;
import com.staynest.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * A notification is private to the user it was sent to.
     *
     * Every read endpoint below takes the recipient from the URL, and being signed in as anyone
     * was enough to pass — so changing the number in /user/{userId} handed you somebody else's
     * inbox, and /{id}/read let you mark their messages read. The acting user comes from the
     * JWT instead, with ADMIN allowed through for support.
     */
    private void requireOwner(Integer userId) {
        if (CurrentUser.hasRole("ADMIN")) {
            return;
        }
        Integer actor = CurrentUser.id();
        if (actor == null || !actor.equals(userId)) {
            throw new AccessDeniedException("You can only access your own notifications.");
        }
    }

    // Deliberately not owner-checked: services post to a guest or staff member on behalf of
    // whoever triggered the event, so the recipient is almost never the caller.
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent", notificationService.sendNotification(request)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByUser(@PathVariable Integer userId) {
        requireOwner(userId);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByUser(userId)));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadByUser(@PathVariable Integer userId) {
        requireOwner(userId);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationsByUser(userId)));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@PathVariable Integer userId) {
        requireOwner(userId);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Integer id) {
        // Addressed by notification id, so the owner has to be looked up to check it.
        requireOwner(notificationService.getNotificationById(id).getUserId());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", notificationService.markAsRead(id)));
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@PathVariable Integer userId) {
        requireOwner(userId);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " notifications as read", count));
    }
}
package com.staynest.reservation.audit;

import com.staynest.reservation.client.AuditServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Records an activity in the shared audit_logs table.
 *
 * Deliberately fire-and-forget, like the notification helpers: an audit write must never fail
 * the business action that produced it, and iam-service being briefly unreachable should not
 * stop a guest checking in. A failure is logged locally so the gap is visible.
 *
 * The trade-off is explicit — this is an activity trail for support and demos, not a
 * tamper-evident compliance log. A guaranteed trail would need the write in the same
 * transaction as the change, which means an outbox table rather than a Feign call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRecorder {

    private final AuditServiceClient auditServiceClient;

    /**
     * @param action     what happened, e.g. CREATE / UPDATE / CANCEL / DELETE
     * @param entityType which kind of record, e.g. RESERVATION
     * @param entityId   the affected record's id, null if it has none yet
     */
    public void record(String action, String entityType, Integer entityId) {
        Integer userId = CurrentUser.id();
        try {
            auditServiceClient.logAction(userId, action, entityType, entityId);
        } catch (Exception e) {
            log.warn("Audit write failed for {} {} {} (user {}): {}",
                    action, entityType, entityId, userId, e.getMessage());
        }
    }
}

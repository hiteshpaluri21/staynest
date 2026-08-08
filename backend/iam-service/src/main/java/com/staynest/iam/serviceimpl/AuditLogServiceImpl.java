package com.staynest.iam.serviceimpl;

import com.staynest.iam.entity.AuditLog;
import com.staynest.iam.repository.AuditLogRepository;
import com.staynest.iam.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public void logAction(Integer userId, String action, String entityType, Integer entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);

        auditLogRepository.save(auditLog);
        log.info("Audit logged: user={}, action={}, entity={}", userId, action, entityType);
    }

    @Override
    public List<AuditLog> getByUserId(Integer userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> getByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> getByTimestampRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    @Override
    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
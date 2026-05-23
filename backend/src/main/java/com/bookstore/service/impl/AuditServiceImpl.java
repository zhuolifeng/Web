package com.bookstore.service.impl;

import com.bookstore.entity.AuditLog;
import com.bookstore.repository.AuditLogRepository;
import com.bookstore.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void log(Long userId, String username, String action,
                    String detail, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setDetail(detail);
        log.setIpAddress(getClientIp(request));
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

package com.bookstore.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {

    void log(Long userId, String username, String action,
             String detail, HttpServletRequest request);
}

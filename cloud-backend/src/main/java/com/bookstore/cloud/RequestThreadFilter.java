package com.bookstore.cloud;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Evidence is captured on the actual Tomcat request thread, not in a separate task. */
public class RequestThreadFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Request-Thread-Virtual", Boolean.toString(Thread.currentThread().isVirtual()));
        response.setHeader("X-Request-Thread-Name", Thread.currentThread().getName());
        response.setHeader("X-Java-Version", System.getProperty("java.version"));
        chain.doFilter(request, response);
    }
}

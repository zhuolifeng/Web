package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.OrderCreateRequest;
import com.bookstore.dto.OrderEvent;
import com.bookstore.entity.CartItem;
import com.bookstore.exception.BusinessException;
import com.bookstore.messaging.OrderEventProducer;
import com.bookstore.messaging.OrderSseHub;
import com.bookstore.repository.CartItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * hw5：促销场景下的异步下单接口（Kafka + SSE）。
 * <ul>
 *   <li>POST /api/orders：把订单 JSON 投递到 Kafka order-events，立即返回 202 Accepted 与排队 ID；</li>
 *   <li>GET /api/orders/{id}/stream：SSE 长连接，消费者处理完成后实时推送成功/失败结果。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/orders")
public class AsyncOrderController {

    private final CartItemRepository cartItemRepository;
    private final OrderEventProducer producer;
    private final OrderSseHub sseHub;

    public AsyncOrderController(CartItemRepository cartItemRepository,
                                OrderEventProducer producer,
                                OrderSseHub sseHub) {
        this.cartItemRepository = cartItemRepository;
        this.producer = producer;
        this.sseHub = sseHub;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(@Valid @RequestBody OrderCreateRequest body,
                                                                   HttpServletRequest request) {
        Long userId = requireUserId(request);
        List<CartItem> cart = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cart.isEmpty()) {
            throw new BusinessException(400, "购物车为空，无法下单");
        }

        OrderEvent event = new OrderEvent();
        event.setQueueId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setUsername(currentUsername());
        event.setShipping(body);
        event.setItems(cart.stream()
                .map(ci -> new OrderEvent.Item(ci.getBookId(), ci.getQuantity()))
                .toList());
        event.setSubmittedAt(LocalDateTime.now());

        // 先登记再投递，避免消费者处理过快导致状态丢失
        sseHub.register(event.getQueueId(), userId);
        try {
            producer.send(event);
        } catch (RuntimeException e) {
            sseHub.unregister(event.getQueueId());
            throw e;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("queueId", event.getQueueId());
        data.put("status", "QUEUED");
        data.put("streamUrl", "/api/orders/" + event.getQueueId() + "/stream");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("订单已受理，正在排队处理", data));
    }

    /**
     * SSE 订阅订单处理结果。浏览器原生 EventSource 无法携带请求头，
     * 因此也支持通过 {@code ?token=<JWT>} 传递登录凭证。
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable("id") String queueId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (!sseHub.isOwnedBy(queueId, userId)) {
            throw new BusinessException(404, "排队单不存在或已过期");
        }
        return sseHub.subscribe(queueId);
    }

    private Long requireUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Long)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return (Long) attr;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : String.valueOf(auth.getPrincipal());
    }
}

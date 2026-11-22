package com.bookstore.order.service;

import com.bookstore.order.client.InventoryClient;
import com.bookstore.order.dto.DeductRequest;
import com.bookstore.order.dto.DeductResult;
import com.bookstore.order.dto.OrderResult;
import com.bookstore.order.dto.PlaceOrderRequest;
import com.bookstore.order.repository.OrderRepository;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;

    public OrderService(InventoryClient inventoryClient, OrderRepository orderRepository) {
        this.inventoryClient = inventoryClient;
        this.orderRepository = orderRepository;
    }

    /**
     * 统一下单（Seata AT 全局事务）。
     * <ol>
     *   <li>TM 向 TC 申请全局事务，得到 XID；</li>
     *   <li>OpenFeign 调用库存中心扣减库存（分支 1，bookstore_inventory，一阶段本地提交）；</li>
     *   <li>在订单库写订单（分支 2，bookstore_order）；</li>
     *   <li>任一步抛出异常 → TM 通知 TC 全局回滚 → TC 驱动各 RM 依据 undo_log 补偿。</li>
     * </ol>
     * 这里不叠加本地 @Transactional：订单 INSERT 在 DataSourceProxy 下以分支事务提交并写 undo_log，
     * 抛异常后同样由 Seata 依据 undo_log 删除该订单，避免 RPC 期间长时间占用数据库连接。
     */
    @GlobalTransactional(name = "bookstore-place-order", rollbackFor = Exception.class, timeoutMills = 60000)
    public OrderResult placeOrder(PlaceOrderRequest req) {
        return doPlaceOrder(req, "Seata 全局事务");
    }

    /**
     * 对照组：只有本地 @Transactional，没有全局事务。
     * 订单写入失败只能回滚订单库，已经在库存库提交的扣减不会被撤销 → 数据不一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResult placeOrderWithoutGlobalTx(PlaceOrderRequest req) {
        return doPlaceOrder(req, "仅本地事务（对照组）");
    }

    private OrderResult doPlaceOrder(PlaceOrderRequest req, String mode) {
        validate(req);
        String xid = RootContext.getXID();
        log.info("[订单中心] ===== 开始下单 [{}] XID={} userId={} bookId={} quantity={} simulateError={}",
                mode, xid, req.userId(), req.bookId(), req.quantity(), req.shouldFail());

        // 第一步：RPC 扣减库存
        DeductResult deducted = inventoryClient.deduct(new DeductRequest(req.bookId(), req.quantity()));
        log.info("[订单中心] 第一步完成：库存中心已扣减 bookId={} 剩余库存={}",
                deducted.bookId(), deducted.remainingStock());

        // 第二步：订单入库（可模拟异常）
        BigDecimal total = deducted.price().multiply(BigDecimal.valueOf(req.quantity()));
        String orderNo = LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        long orderId = orderRepository.insert(orderNo, req.userId(), req.bookId(), req.quantity(),
                deducted.price(), total, req.shouldFail());
        log.info("[订单中心] 第二步完成：订单已创建 orderId={} orderNo={} XID={}", orderId, orderNo, xid);

        return new OrderResult(xid, orderId, orderNo, req.bookId(), deducted.title(), req.quantity(),
                total, deducted.remainingStock());
    }

    private static void validate(PlaceOrderRequest req) {
        if (req == null || req.userId() == null || req.bookId() == null
                || req.quantity() == null || req.quantity() <= 0) {
            throw new IllegalArgumentException("userId/bookId/quantity 不合法");
        }
    }
}

package com.bookstore.inventory.controller;

import com.bookstore.inventory.dto.DeductRequest;
import com.bookstore.inventory.dto.DeductResult;
import com.bookstore.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 库存中心对外（供订单中心 RPC 调用）的接口。 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/deduct")
    public DeductResult deduct(@RequestBody DeductRequest req) {
        return inventoryService.deduct(req.bookId(), req.quantity() == null ? 0 : req.quantity());
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return inventoryService.listAll();
    }
}

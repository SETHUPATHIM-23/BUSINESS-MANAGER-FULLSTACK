package com.businessmanager.backend.purchasing.controller;

import com.businessmanager.backend.purchasing.dto.*;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.mapper.PurchaseOrderMapper;
import com.businessmanager.backend.purchasing.service.GoodsReceiptService;
import com.businessmanager.backend.purchasing.service.PurchaseOrderService;
import com.businessmanager.backend.purchasing.service.ThreeWayMatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;
    private final ThreeWayMatchService threeWayMatchService;
    private final PurchaseOrderMapper purchaseOrderMapper;


    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<PurchaseOrderSummaryDto>> listPurchaseOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PurchaseOrder> purchaseOrders = purchaseOrderService.searchPurchaseOrders(
                search, supplierId, status, dateFrom, dateTo, pageable);

        Page<PurchaseOrderSummaryDto> dtos = purchaseOrders.map(purchaseOrderMapper::toSummaryDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrder(@PathVariable Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(purchaseOrderMapper.toResponseDto(purchaseOrder));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        PurchaseOrder entity = purchaseOrderMapper.toEntity(request);
        PurchaseOrder saved = purchaseOrderService.createPurchaseOrder(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderMapper.toResponseDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<PurchaseOrderResponseDto> updatePurchaseOrder(
            @PathVariable Long id, 
            @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        PurchaseOrder entity = purchaseOrderMapper.toEntity(request);
        PurchaseOrder updated = purchaseOrderService.updatePurchaseOrder(id, entity);
        return ResponseEntity.ok(purchaseOrderMapper.toResponseDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<Void> cancelPurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<Void> receiveGoods(
            @PathVariable Long id,
            @Valid @RequestBody GoodsReceiptRequest request) {
        request.setPoId(id);
        goodsReceiptService.receiveGoods(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<Void> processSupplierInvoice(
            @PathVariable Long id,
            @Valid @RequestBody SupplierInvoiceRequest request) {
        request.setPurchaseOrderId(id);
        threeWayMatchService.processSupplierInvoice(request);
        return ResponseEntity.ok().build();
    }

}

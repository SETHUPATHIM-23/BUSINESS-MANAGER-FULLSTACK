package com.businessmanager.backend.purchasing.mapper;

import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.purchasing.dto.*;
import com.businessmanager.backend.purchasing.entity.GoodsReceipt;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.supplier.entity.Supplier;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseOrderMapper {

    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "idToSupplier")
    PurchaseOrder toEntity(PurchaseOrderCreateRequest request);

    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "idToSupplier")
    PurchaseOrder toEntity(PurchaseOrderUpdateRequest request);

    @Mapping(target = "product", source = "productId", qualifiedByName = "idToProduct")
    PurchaseLine toEntity(PurchaseLineRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    PurchaseOrderResponseDto toResponseDto(PurchaseOrder entity);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    PurchaseOrderSummaryDto toSummaryDto(PurchaseOrder entity);

    List<PurchaseOrderSummaryDto> toSummaryDtoList(List<PurchaseOrder> entities);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    PurchaseLineResponseDto toLineResponseDto(PurchaseLine line);

    @Mapping(target = "purchaseLineId", source = "purchaseLine.id")
    GoodsReceiptResponseDto toReceiptResponseDto(GoodsReceipt receipt);

    @Named("idToSupplier")
    default Supplier idToSupplier(Long id) {
        if (id == null) return null;
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }

    @Named("idToProduct")
    default Product idToProduct(Long id) {
        if (id == null) return null;
        Product product = new Product();
        product.setId(id);
        return product;
    }

    @AfterMapping
    default void computeTotals(PurchaseOrder source, @MappingTarget PurchaseOrderResponseDto target) {
        if (source.getLines() != null) {
            BigDecimal total = source.getLines().stream()
                    .map(PurchaseLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            target.setTotalAmount(total);
        } else {
            target.setTotalAmount(BigDecimal.ZERO);
        }
    }

    @AfterMapping
    default void computeTotalsSummary(PurchaseOrder source, @MappingTarget PurchaseOrderSummaryDto target) {
        if (source.getLines() != null) {
            BigDecimal total = source.getLines().stream()
                    .map(PurchaseLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            target.setTotalAmount(total);
        } else {
            target.setTotalAmount(BigDecimal.ZERO);
        }
    }
}

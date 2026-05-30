package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.ProductTypeDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.ProductType;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.ProductTypeRepository;
import yt.wer.efms.repository.UnitRepository;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductTypeController {
    private final ProductTypeRepository productTypeRepository;
    private final UnitRepository unitRepository;
    private final FarmRepository farmRepository;
    private final PermissionService permissionService;

    public ProductTypeController(ProductTypeRepository productTypeRepository,
                                 UnitRepository unitRepository,
                                 FarmRepository farmRepository,
                                 PermissionService permissionService) {
        this.productTypeRepository = productTypeRepository;
        this.unitRepository = unitRepository;
        this.farmRepository = farmRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/product-types")
    public ResponseEntity<List<ProductTypeDto>> listProductTypes(
            @RequestParam(required = false) Long farmId) {
        List<ProductTypeDto> types = (farmId != null
                ? productTypeRepository.findByFarmIsNullOrFarmIdOrderByIdAsc(farmId)
                : productTypeRepository.findByFarmIsNullOrderByIdAsc())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @PostMapping("/product-types")
    public ResponseEntity<ProductTypeDto> createProductType(@RequestBody ProductTypeDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProductType type = new ProductType();
        type.setName(input.getName().trim());
        type.setSeedType(input.isSeedType());
        type.setCreatedAt(LocalDateTime.now());
        type.setModifiedAt(LocalDateTime.now());
        if (input.getUnitId() != null) {
            unitRepository.findById(input.getUnitId()).ifPresent(type::setUnit);
        }
        return ResponseEntity.ok(toDto(productTypeRepository.save(type)));
    }

    @PutMapping("/product-types/{id}")
    public ResponseEntity<ProductTypeDto> updateProductType(@PathVariable Long id,
                                                            @RequestBody ProductTypeDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return productTypeRepository.findById(id).map(type -> {
            if (input.getName() != null && !input.getName().trim().isEmpty()) {
                type.setName(input.getName().trim());
            }
            type.setSeedType(input.isSeedType());
            if (input.getUnitId() != null) {
                unitRepository.findById(input.getUnitId()).ifPresent(type::setUnit);
            } else {
                type.setUnit(null);
            }
            type.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(productTypeRepository.save(type)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/product-types/{id}")
    public ResponseEntity<Void> deleteProductType(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (!productTypeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/product-types/{id}/promote")
    public ResponseEntity<ProductTypeDto> promoteProductType(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return productTypeRepository.findById(id).map(type -> {
            type.setFarm(null);
            type.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(productTypeRepository.save(type)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ProductTypeDto toDto(ProductType t) {
        ProductTypeDto dto = new ProductTypeDto(t.getId(), t.getName(),
                t.getUnit() != null ? t.getUnit().getId() : null,
                t.getFarm() != null ? t.getFarm().getId() : null);
        dto.setSeedType(t.isSeedType());
        return dto;
    }
}

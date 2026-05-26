package yt.wer.efms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.dto.*;
import yt.wer.efms.model.*;
import yt.wer.efms.repository.*;
import yt.wer.efms.service.FarmAssetService;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/farm/{farmId}")
public class FarmAssetController {
    private final FarmAssetService farmAssetService;
    private final FarmRepository farmRepository;
    private final UnitRepository unitRepository;
    private final ProductTypeRepository productTypeRepository;
    private final OperationTypeRepository operationTypeRepository;
    private final ToolRepository toolRepository;
    private final FarmOperationTypeDefaultToolRepository farmOpTypeDefaultRepository;
    private final PermissionService permissionService;

    public FarmAssetController(FarmAssetService farmAssetService,
                               FarmRepository farmRepository,
                               UnitRepository unitRepository,
                               ProductTypeRepository productTypeRepository,
                               OperationTypeRepository operationTypeRepository,
                               ToolRepository toolRepository,
                               FarmOperationTypeDefaultToolRepository farmOpTypeDefaultRepository,
                               PermissionService permissionService) {
        this.farmAssetService = farmAssetService;
        this.farmRepository = farmRepository;
        this.unitRepository = unitRepository;
        this.productTypeRepository = productTypeRepository;
        this.operationTypeRepository = operationTypeRepository;
        this.toolRepository = toolRepository;
        this.farmOpTypeDefaultRepository = farmOpTypeDefaultRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(
            @PathVariable Long farmId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long productTypeId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long defaultOpTypeId,
            @RequestParam(required = false, defaultValue = "false") boolean includeOfficial) {
        try {
            if (page != null) {
                int pageSize = size != null ? size : 10;
                var pageable = org.springframework.data.domain.PageRequest.of(page, pageSize);
                if (includeOfficial) {
                    return ResponseEntity.ok(farmAssetService.searchProductsWithOfficial(farmId, query, pageable));
                }
                if (query != null || productTypeId != null || unitId != null || defaultOpTypeId != null) {
                    return ResponseEntity.ok(farmAssetService.searchProducts(farmId, query, productTypeId, unitId, defaultOpTypeId, pageable));
                }
                return ResponseEntity.ok(farmAssetService.listProducts(farmId, pageable));
            }
            if (includeOfficial) {
                return ResponseEntity.ok(farmAssetService.listProductsWithOfficial(farmId));
            }
            return ResponseEntity.ok(farmAssetService.listProducts(farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@PathVariable Long farmId, @RequestBody ProductInput input) {
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("name_required");
        }
        try {
            return farmAssetService.createProduct(farmId, input)
                .map(dto -> ResponseEntity.created(URI.create("/farm/" + farmId + "/products/" + dto.getId())).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long farmId, @PathVariable Long productId, @RequestBody ProductInput input) {
        try {
            return farmAssetService.updateProduct(farmId, productId, input)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping(value = "/products/{productId}/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductPicture(@PathVariable Long farmId, @PathVariable Long productId,
                                                  @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("missing_file");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) return ResponseEntity.badRequest().body("invalid_file_type");
        try {
            String url = farmAssetService.uploadProductPicture(farmId, productId, file);
            return ResponseEntity.ok(Map.of("pictureUrl", url));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long farmId, @PathVariable Long productId) {
        try {
            farmAssetService.deleteProduct(farmId, productId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/tools")
    public ResponseEntity<?> listTools(
            @PathVariable Long farmId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId) {
        try {
            if (page != null) {
                int pageSize = size != null ? size : 10;
                var pageable = org.springframework.data.domain.PageRequest.of(page, pageSize);
                if (query != null || categoryId != null) {
                    return ResponseEntity.ok(farmAssetService.searchTools(farmId, query, categoryId, pageable));
                }
                return ResponseEntity.ok(farmAssetService.listTools(farmId, pageable));
            }
            return ResponseEntity.ok(farmAssetService.listTools(farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/tools")
    public ResponseEntity<?> createTool(@PathVariable Long farmId, @RequestBody ToolInput input) {
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("name_required");
        }
        try {
            return farmAssetService.createTool(farmId, input)
                .map(dto -> ResponseEntity.created(URI.create("/farm/" + farmId + "/tools/" + dto.getId())).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/tools/{toolId}")
    public ResponseEntity<ToolDto> updateTool(@PathVariable Long farmId, @PathVariable Long toolId, @RequestBody ToolInput input) {
        try {
            return farmAssetService.updateTool(farmId, toolId, input)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping(value = "/tools/{toolId}/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadToolPicture(@PathVariable Long farmId, @PathVariable Long toolId,
                                               @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("missing_file");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) return ResponseEntity.badRequest().body("invalid_file_type");
        try {
            String url = farmAssetService.uploadToolPicture(farmId, toolId, file);
            return ResponseEntity.ok(Map.of("pictureUrl", url));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/tools/{toolId}")
    public ResponseEntity<Void> deleteTool(@PathVariable Long farmId, @PathVariable Long toolId) {
        try {
            farmAssetService.deleteTool(farmId, toolId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    private Farm requireFarmManage(Long farmId) {
        Long userId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        if (!permissionService.canManageFarm(farmId, userId, isAdmin)) {
            throw new RuntimeException("Not authorised to manage this farm");
        }
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    @PostMapping("/reference/units")
    public ResponseEntity<UnitDto> createFarmUnit(@PathVariable Long farmId,
                                                  @RequestBody UnitDto input) {
        try {
            Farm farm = requireFarmManage(farmId);
            if (input.getValue() == null || input.getValue().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            Unit unit = new Unit();
            unit.setValue(input.getValue().trim());
            unit.setFarm(farm);
            unit.setCreatedAt(LocalDateTime.now());
            unit.setModifiedAt(LocalDateTime.now());
            Unit saved = unitRepository.save(unit);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/reference/units/" + saved.getId()))
                    .body(new UnitDto(saved.getId(), saved.getValue(), farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/reference/units/{unitId}")
    public ResponseEntity<Void> deleteFarmUnit(@PathVariable Long farmId,
                                               @PathVariable Long unitId) {
        try {
            requireFarmManage(farmId);
            Unit unit = unitRepository.findById(unitId).orElse(null);
            if (unit == null) return ResponseEntity.notFound().build();
            if (unit.getFarm() == null || !unit.getFarm().getId().equals(farmId)) {
                return ResponseEntity.status(403).build();
            }
            unitRepository.delete(unit);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/reference/product-types")
    public ResponseEntity<ProductTypeDto> createFarmProductType(@PathVariable Long farmId,
                                                                @RequestBody ProductTypeDto input) {
        try {
            Farm farm = requireFarmManage(farmId);
            if (input.getName() == null || input.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            ProductType type = new ProductType();
            type.setName(input.getName().trim());
            type.setFarm(farm);
            type.setCreatedAt(LocalDateTime.now());
            type.setModifiedAt(LocalDateTime.now());
            if (input.getUnitId() != null) {
                unitRepository.findById(input.getUnitId()).ifPresent(type::setUnit);
            }
            ProductType saved = productTypeRepository.save(type);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/reference/product-types/" + saved.getId()))
                    .body(new ProductTypeDto(saved.getId(), saved.getName(),
                            saved.getUnit() != null ? saved.getUnit().getId() : null, farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/reference/product-types/{typeId}")
    public ResponseEntity<ProductTypeDto> updateFarmProductType(@PathVariable Long farmId,
                                                                @PathVariable Long typeId,
                                                                @RequestBody ProductTypeDto input) {
        try {
            requireFarmManage(farmId);
            ProductType type = productTypeRepository.findById(typeId).orElse(null);
            if (type == null) return ResponseEntity.notFound().build();
            if (type.getFarm() == null || !type.getFarm().getId().equals(farmId)) {
                return ResponseEntity.status(403).build();
            }
            if (input.getName() != null && !input.getName().trim().isEmpty()) {
                type.setName(input.getName().trim());
            }
            if (input.getUnitId() != null) {
                unitRepository.findById(input.getUnitId()).ifPresent(type::setUnit);
            } else {
                type.setUnit(null);
            }
            type.setModifiedAt(LocalDateTime.now());
            ProductType saved = productTypeRepository.save(type);
            return ResponseEntity.ok(new ProductTypeDto(saved.getId(), saved.getName(),
                    saved.getUnit() != null ? saved.getUnit().getId() : null, farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/reference/product-types/{typeId}")
    public ResponseEntity<Void> deleteFarmProductType(@PathVariable Long farmId,
                                                      @PathVariable Long typeId) {
        try {
            requireFarmManage(farmId);
            ProductType type = productTypeRepository.findById(typeId).orElse(null);
            if (type == null) return ResponseEntity.notFound().build();
            if (type.getFarm() == null || !type.getFarm().getId().equals(farmId)) {
                return ResponseEntity.status(403).build();
            }
            productTypeRepository.delete(type);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/reference/operation-types")
    public ResponseEntity<OperationTypeDto> createFarmOperationType(@PathVariable Long farmId,
                                                                    @RequestBody OperationTypeDto input) {
        try {
            Farm farm = requireFarmManage(farmId);
            if (input.getName() == null || input.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            OperationType type = new OperationType();
            type.setName(input.getName().trim());
            type.setFarm(farm);
            type.setCreatedAt(LocalDateTime.now());
            type.setModifiedAt(LocalDateTime.now());
            OperationType saved = operationTypeRepository.save(type);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/reference/operation-types/" + saved.getId()))
                    .body(new OperationTypeDto(saved.getId(), saved.getName(), farmId, null, null));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/reference/operation-types/{typeId}")
    public ResponseEntity<Void> deleteFarmOperationType(@PathVariable Long farmId,
                                                        @PathVariable Long typeId) {
        try {
            requireFarmManage(farmId);
            OperationType type = operationTypeRepository.findById(typeId).orElse(null);
            if (type == null) return ResponseEntity.notFound().build();
            if (type.getFarm() == null || !type.getFarm().getId().equals(farmId)) {
                return ResponseEntity.status(403).build();
            }
            operationTypeRepository.delete(type);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/operation-type-defaults")
    public ResponseEntity<List<FarmOperationTypeDefaultDto>> listDefaultTools(
            @PathVariable Long farmId) {
        try {
            Long userId = permissionService.currentUserId();
            boolean isAdmin = permissionService.isCurrentUserAdmin();
            if (!permissionService.canViewFarm(farmId, userId, isAdmin)) {
                return ResponseEntity.status(403).build();
            }
            List<FarmOperationTypeDefaultDto> result = farmOpTypeDefaultRepository.findByFarmId(farmId)
                    .stream()
                    .map(d -> new FarmOperationTypeDefaultDto(
                            d.getOperationType().getId(),
                            d.getOperationType().getName(),
                            d.getTool().getId(),
                            d.getTool().getName()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/operation-type-defaults/{operationTypeId}")
    public ResponseEntity<FarmOperationTypeDefaultDto> setDefaultTool(
            @PathVariable Long farmId,
            @PathVariable Long operationTypeId,
            @RequestBody Map<String, Long> body) {
        try {
            Farm farm = requireFarmManage(farmId);
            Long toolId = body.get("toolId");
            if (toolId == null) {
                return ResponseEntity.badRequest().build();
            }
            Tool tool = toolRepository.findById(toolId)
                    .orElseThrow(() -> new RuntimeException("Tool not found"));
            if (tool.getFarm() != null && !tool.getFarm().getId().equals(farmId)) {
                return ResponseEntity.status(403).build();
            }
            OperationType opType = operationTypeRepository.findById(operationTypeId)
                    .orElseThrow(() -> new RuntimeException("Operation type not found"));

            FarmOperationTypeDefaultTool entry = farmOpTypeDefaultRepository
                    .findByFarmIdAndOperationTypeId(farmId, operationTypeId)
                    .orElseGet(() -> {
                        FarmOperationTypeDefaultTool e = new FarmOperationTypeDefaultTool();
                        e.setFarm(farm);
                        e.setOperationType(opType);
                        e.setCreatedAt(LocalDateTime.now());
                        return e;
                    });
            entry.setTool(tool);
            entry.setModifiedAt(LocalDateTime.now());
            farmOpTypeDefaultRepository.save(entry);

            return ResponseEntity.ok(new FarmOperationTypeDefaultDto(
                    operationTypeId, opType.getName(), toolId, tool.getName()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/operation-type-defaults/{operationTypeId}")
    public ResponseEntity<Void> clearDefaultTool(@PathVariable Long farmId,
                                                 @PathVariable Long operationTypeId) {
        try {
            requireFarmManage(farmId);
            farmOpTypeDefaultRepository.deleteByFarmIdAndOperationTypeId(farmId, operationTypeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }
}

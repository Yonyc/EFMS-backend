package yt.wer.efms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import yt.wer.efms.dto.AdminProductInput;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.dto.ProductInput;
import yt.wer.efms.dto.ToolDto;
import yt.wer.efms.dto.ToolInput;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.ProductType;
import yt.wer.efms.model.Tool;
import yt.wer.efms.model.ToolCategory;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.AttachmentRepository;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.repository.ProductRepository;
import yt.wer.efms.repository.ProductTypeRepository;
import yt.wer.efms.repository.ToolRepository;
import yt.wer.efms.repository.ToolCategoryRepository;
import yt.wer.efms.repository.UnitRepository;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmAssetService {
    private final FarmRepository farmRepository;
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UnitRepository unitRepository;
    private final ToolRepository toolRepository;
    private final ToolCategoryRepository toolCategoryRepository;
    private final OperationTypeRepository operationTypeRepository;
    private final AttachmentRepository attachmentRepository;
    private final PermissionService permissionService;

    public FarmAssetService(FarmRepository farmRepository,
                            ProductRepository productRepository,
                            ProductTypeRepository productTypeRepository,
                            UnitRepository unitRepository,
                            ToolRepository toolRepository,
                            ToolCategoryRepository toolCategoryRepository,
                            OperationTypeRepository operationTypeRepository,
                            AttachmentRepository attachmentRepository,
                            PermissionService permissionService) {
        this.farmRepository = farmRepository;
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.unitRepository = unitRepository;
        this.toolRepository = toolRepository;
        this.toolCategoryRepository = toolCategoryRepository;
        this.operationTypeRepository = operationTypeRepository;
        this.attachmentRepository = attachmentRepository;
        this.permissionService = permissionService;
    }

    public List<ProductDto> listProducts(Long farmId) {
        requireFarmView(farmId);
        return productRepository.findByFarmId(farmId).stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> listProductsWithOfficial(Long farmId) {
        requireFarmView(farmId);
        List<ProductDto> result = new java.util.ArrayList<>(
            productRepository.findByFarmId(farmId).stream()
                .map(this::toProductDto)
                .collect(Collectors.toList())
        );
        productRepository.findByOfficialTrueAndOfficialCurrentTrue().stream()
                .map(this::toProductDto)
                .forEach(result::add);
        return result;
    }

    public Page<ProductDto> listProducts(Long farmId, Pageable pageable) {
        requireFarmView(farmId);
        return productRepository.findByFarmId(farmId, pageable)
                .map(this::toProductDto);
    }

    public Page<ProductDto> searchProductsWithOfficial(Long farmId, String query, Pageable pageable) {
        requireFarmView(farmId);
        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        return productRepository.searchFarmAndOfficialProducts(farmId, q, pageable)
                .map(this::toProductDto);
    }

    public Page<ProductDto> searchProducts(Long farmId, String query, Long productTypeId, Long unitId, Long defaultOpTypeId, Pageable pageable) {
        requireFarmView(farmId);
        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        return productRepository.searchByFarm(farmId, q, productTypeId, unitId, defaultOpTypeId, pageable)
                .map(this::toProductDto);
    }

    public Optional<ProductDto> createProduct(Long farmId, ProductInput input) {
        Farm farm = requireFarmEdit(farmId);
        Product p = new Product();
        p.setName(input.getName());
        p.setFarm(farm);
        p.setCreatedAt(LocalDateTime.now());
        p.setModifiedAt(LocalDateTime.now());
        if (input.getProductTypeId() != null) {
            ProductType type = productTypeRepository.findById(input.getProductTypeId())
                    .orElseThrow(() -> new RuntimeException("Product type not found"));
            p.setProductType(type);
        }
        if (input.getUnitId() != null) {
            Unit unit = unitRepository.findById(input.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            p.setUnit(unit);
        }
        if (input.getDefaultOperationTypeId() != null) {
            OperationType opType = operationTypeRepository.findById(input.getDefaultOperationTypeId())
                    .orElseThrow(() -> new RuntimeException("Operation type not found"));
            p.setDefaultOperationType(opType);
        }
        if (input.getOverrideToolId() != null) {
            Tool tool = toolRepository.findById(input.getOverrideToolId())
                    .orElseThrow(() -> new RuntimeException("Tool not found"));
            p.setOverrideTool(tool);
        }
        if (input.getDescription() != null) p.setDescription(input.getDescription());
        if (input.getPictureUrl() != null) p.setPictureUrl(input.getPictureUrl());
        Product saved = productRepository.save(p);
        return Optional.of(toProductDto(saved));
    }

    public Optional<ProductDto> updateProduct(Long farmId, Long productId, ProductInput input) {
        requireFarmEdit(farmId);
        return productRepository.findById(productId).map(existing -> {
            if (existing.getFarm() == null || !existing.getFarm().getId().equals(farmId)) {
                throw new RuntimeException("Product does not belong to this farm");
            }
            if (input.getName() != null) existing.setName(input.getName());
            if (input.getProductTypeId() != null) {
                ProductType type = productTypeRepository.findById(input.getProductTypeId())
                        .orElseThrow(() -> new RuntimeException("Product type not found"));
                existing.setProductType(type);
            }
            if (input.getUnitId() != null) {
                Unit unit = unitRepository.findById(input.getUnitId())
                        .orElseThrow(() -> new RuntimeException("Unit not found"));
                existing.setUnit(unit);
            }
            // passing null explicitly clears the association rather than leaving the old value
            if (input.getDefaultOperationTypeId() != null) {
                OperationType opType = operationTypeRepository.findById(input.getDefaultOperationTypeId())
                        .orElseThrow(() -> new RuntimeException("Operation type not found"));
                existing.setDefaultOperationType(opType);
            } else {
                existing.setDefaultOperationType(null);
            }
            if (input.getOverrideToolId() != null) {
                Tool tool = toolRepository.findById(input.getOverrideToolId())
                        .orElseThrow(() -> new RuntimeException("Tool not found"));
                existing.setOverrideTool(tool);
            } else {
                existing.setOverrideTool(null);
            }
            if (input.getDescription() != null) existing.setDescription(input.getDescription());
            if (input.getPictureUrl() != null) existing.setPictureUrl(input.getPictureUrl());
            existing.setModifiedAt(LocalDateTime.now());
            return toProductDto(productRepository.save(existing));
        });
    }

    public void deleteProduct(Long farmId, Long productId) {
        requireFarmEdit(farmId);
        productRepository.findById(productId).ifPresent(p -> {
            if (p.getFarm() != null && p.getFarm().getId().equals(farmId)) {
                productRepository.delete(p);
            }
        });
    }

    public List<ToolDto> listTools(Long farmId) {
        requireFarmView(farmId);
        return toolRepository.findByFarmId(farmId).stream()
                .map(this::toToolDto)
                .collect(Collectors.toList());
    }

    public Page<ToolDto> listTools(Long farmId, Pageable pageable) {
        requireFarmView(farmId);
        return toolRepository.findByFarmId(farmId, pageable)
                .map(this::toToolDto);
    }

    public Page<ToolDto> searchTools(Long farmId, String query, Long categoryId, Pageable pageable) {
        requireFarmView(farmId);
        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        return toolRepository.searchByFarm(farmId, q, categoryId, pageable)
                .map(this::toToolDto);
    }

    public Optional<ToolDto> createTool(Long farmId, ToolInput input) {
        Farm farm = requireFarmEdit(farmId);
        Tool tool = new Tool();
        tool.setName(input.getName());
        tool.setFarm(farm);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setModifiedAt(LocalDateTime.now());
        if (input.getCategoryId() != null) {
            ToolCategory category = toolCategoryRepository.findById(input.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Tool category not found"));
            tool.setCategory(category);
        }
        if (input.getDescription() != null) tool.setDescription(input.getDescription());
        if (input.getPictureUrl() != null) tool.setPictureUrl(input.getPictureUrl());
        Tool saved = toolRepository.save(tool);
        return Optional.of(toToolDto(saved));
    }

    public Optional<ToolDto> updateTool(Long farmId, Long toolId, ToolInput input) {
        requireFarmEdit(farmId);
        return toolRepository.findById(toolId).map(existing -> {
            if (existing.getFarm() == null || !existing.getFarm().getId().equals(farmId)) {
                throw new RuntimeException("Tool does not belong to this farm");
            }
            if (input.getName() != null) existing.setName(input.getName());
            if (input.getCategoryId() != null) {
                ToolCategory category = toolCategoryRepository.findById(input.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Tool category not found"));
                existing.setCategory(category);
            }
            if (input.getDescription() != null) existing.setDescription(input.getDescription());
            if (input.getPictureUrl() != null) existing.setPictureUrl(input.getPictureUrl());
            existing.setModifiedAt(LocalDateTime.now());
            return toToolDto(toolRepository.save(existing));
        });
    }

    public void deleteTool(Long farmId, Long toolId) {
        requireFarmEdit(farmId);
        toolRepository.findById(toolId).ifPresent(t -> {
            if (t.getFarm() != null && t.getFarm().getId().equals(farmId)) {
                toolRepository.delete(t);
            }
        });
    }

    private ProductDto toProductDto(Product p) {
        ProductDto dto = new ProductDto(
            p.getId(),
            p.getName(),
            p.getProductType() != null ? p.getProductType().getId() : null,
            p.getUnit() != null ? p.getUnit().getId() : null,
            p.getFarm() != null ? p.getFarm().getId() : null,
            p.getCreatedAt(),
            p.getModifiedAt()
        );
        dto.setOfficial(p.getOfficial());
        dto.setOfficialCurrent(p.getOfficialCurrent());
        dto.setOfficialAuthNumber(p.getOfficialAuthNumber());
        dto.setOfficialVersionTag(p.getOfficialVersionTag());
        dto.setOfficialDecisionCode(p.getOfficialDecisionCode());
        dto.setOfficialDecisionCodeEn(p.getOfficialDecisionCodeEn());
        dto.setOfficialDateFirstAuthorization(p.getOfficialDateFirstAuthorization());
        dto.setOfficialDateFrom(p.getOfficialDateFrom());
        dto.setOfficialDateTo(p.getOfficialDateTo());
        dto.setOfficialUserGroupCode(p.getOfficialUserGroupCode());
        dto.setOfficialUserGroupEn(p.getOfficialUserGroupEn());
        dto.setOfficialFormulationTypeCode(p.getOfficialFormulationTypeCode());
        dto.setOfficialFormulationTypeEn(p.getOfficialFormulationTypeEn());
        dto.setOfficialProductTypeCodes(p.getOfficialProductTypeCodes());
        dto.setOfficialProductTypeEn(p.getOfficialProductTypeEn());
        dto.setOfficialImportedAt(p.getOfficialImportedAt());
        dto.setOfficialSaleTo(p.getOfficialSaleTo());
        dto.setOfficialUseToleratedTo(p.getOfficialUseToleratedTo());
        if (p.getDefaultOperationType() != null) {
            dto.setDefaultOperationTypeId(p.getDefaultOperationType().getId());
            dto.setDefaultOperationTypeName(p.getDefaultOperationType().getName());
        }
        if (p.getOverrideTool() != null) {
            dto.setOverrideToolId(p.getOverrideTool().getId());
            dto.setOverrideToolName(p.getOverrideTool().getName());
        }
        dto.setDescription(p.getDescription());
        dto.setPictureUrl(p.getPictureUrl());
        dto.setAttachments(attachmentRepository.findByProductId(p.getId()).stream()
                .map(a -> new yt.wer.efms.dto.AttachmentDto(a.getId(), a.getOriginalFilename(),
                        a.getUrl(), a.getMimeType(), a.getFileSize(), a.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList()));
        return dto;
    }

    private ToolDto toToolDto(Tool t) {
        ToolDto dto = new ToolDto(
                t.getId(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getFarm() != null ? t.getFarm().getId() : null,
                t.getCreatedAt(),
                t.getModifiedAt()
        );
        if (t.getCategory() != null) {
            dto.setCategoryName(t.getCategory().getName());
        }
        dto.setDescription(t.getDescription());
        dto.setPictureUrl(t.getPictureUrl());
        dto.setAttachments(attachmentRepository.findByToolId(t.getId()).stream()
                .map(a -> new yt.wer.efms.dto.AttachmentDto(a.getId(), a.getOriginalFilename(),
                        a.getUrl(), a.getMimeType(), a.getFileSize(), a.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList()));
        return dto;
    }

    public String uploadProductPicture(Long farmId, Long productId, MultipartFile file) {
        requireFarmEdit(farmId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getFarm() == null || !product.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Product does not belong to this farm");
        }
        try {
            Path uploadDir = Paths.get("uploads", "pictures");
            Files.createDirectories(uploadDir);
            String ct = file.getContentType();
            String ext = (ct != null && ct.equals("image/png")) ? ".png" : ".jpg";
            String filename = "product-" + productId + ext;
            Files.write(uploadDir.resolve(filename), file.getBytes());
            String url = "/uploads/pictures/" + filename;
            product.setPictureUrl(url);
            product.setModifiedAt(LocalDateTime.now());
            productRepository.save(product);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("picture_upload_failed", e);
        }
    }

    public String uploadToolPicture(Long farmId, Long toolId, MultipartFile file) {
        requireFarmEdit(farmId);
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
        if (tool.getFarm() == null || !tool.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Tool does not belong to this farm");
        }
        try {
            Path uploadDir = Paths.get("uploads", "pictures");
            Files.createDirectories(uploadDir);
            String ct = file.getContentType();
            String ext = (ct != null && ct.equals("image/png")) ? ".png" : ".jpg";
            String filename = "tool-" + toolId + ext;
            Files.write(uploadDir.resolve(filename), file.getBytes());
            String url = "/uploads/pictures/" + filename;
            tool.setPictureUrl(url);
            tool.setModifiedAt(LocalDateTime.now());
            toolRepository.save(tool);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("picture_upload_failed", e);
        }
    }

    public Optional<ProductDto> adminUpdateProduct(Long productId, AdminProductInput input) {
        return productRepository.findById(productId).map(existing -> {
            if (input.getName() != null) existing.setName(input.getName());
            if (input.getProductTypeId() != null) {
                existing.setProductType(productTypeRepository.findById(input.getProductTypeId()).orElse(null));
            } else {
                existing.setProductType(null);
            }
            if (input.getUnitId() != null) {
                existing.setUnit(unitRepository.findById(input.getUnitId()).orElse(null));
            } else {
                existing.setUnit(null);
            }
            if (input.getDefaultOperationTypeId() != null) {
                existing.setDefaultOperationType(operationTypeRepository.findById(input.getDefaultOperationTypeId()).orElse(null));
            } else {
                existing.setDefaultOperationType(null);
            }
            if (input.getOverrideToolId() != null) {
                existing.setOverrideTool(toolRepository.findById(input.getOverrideToolId()).orElse(null));
            } else {
                existing.setOverrideTool(null);
            }
            existing.setDescription(input.getDescription());
            existing.setPictureUrl(input.getPictureUrl());
            existing.setOfficialCurrent(input.getOfficialCurrent());
            if (input.getOfficialAuthNumber() != null) existing.setOfficialAuthNumber(input.getOfficialAuthNumber());
            if (input.getOfficialVersionTag() != null) existing.setOfficialVersionTag(input.getOfficialVersionTag());
            if (input.getOfficialDecisionCode() != null) existing.setOfficialDecisionCode(input.getOfficialDecisionCode());
            if (input.getOfficialDecisionCodeEn() != null) existing.setOfficialDecisionCodeEn(input.getOfficialDecisionCodeEn());
            if (input.getOfficialDateFirstAuthorization() != null) existing.setOfficialDateFirstAuthorization(input.getOfficialDateFirstAuthorization());
            if (input.getOfficialDateFrom() != null) existing.setOfficialDateFrom(input.getOfficialDateFrom());
            if (input.getOfficialDateTo() != null) existing.setOfficialDateTo(input.getOfficialDateTo());
            if (input.getOfficialUserGroupCode() != null) existing.setOfficialUserGroupCode(input.getOfficialUserGroupCode());
            if (input.getOfficialUserGroupEn() != null) existing.setOfficialUserGroupEn(input.getOfficialUserGroupEn());
            if (input.getOfficialFormulationTypeCode() != null) existing.setOfficialFormulationTypeCode(input.getOfficialFormulationTypeCode());
            if (input.getOfficialFormulationTypeEn() != null) existing.setOfficialFormulationTypeEn(input.getOfficialFormulationTypeEn());
            if (input.getOfficialProductTypeCodes() != null) existing.setOfficialProductTypeCodes(input.getOfficialProductTypeCodes());
            if (input.getOfficialProductTypeEn() != null) existing.setOfficialProductTypeEn(input.getOfficialProductTypeEn());
            existing.setModifiedAt(LocalDateTime.now());
            return toProductDto(productRepository.save(existing));
        });
    }

    private Farm requireFarmView(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
            throw new RuntimeException("You can only view assets for farms you can access");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    private Farm requireFarmEdit(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, actionUserId, isAdmin)) {
            throw new RuntimeException("You can only manage assets for farms you can edit");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }
}

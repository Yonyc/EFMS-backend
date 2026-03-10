package yt.wer.efms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.dto.ProductInput;
import yt.wer.efms.dto.ToolDto;
import yt.wer.efms.dto.ToolInput;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.Tool;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.ProductRepository;
import yt.wer.efms.repository.ToolRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmAssetService {
    private final FarmRepository farmRepository;
    private final ProductRepository productRepository;
    private final ToolRepository toolRepository;
    private final PermissionService permissionService;

    public FarmAssetService(FarmRepository farmRepository,
                            ProductRepository productRepository,
                            ToolRepository toolRepository,
                            PermissionService permissionService) {
        this.farmRepository = farmRepository;
        this.productRepository = productRepository;
        this.toolRepository = toolRepository;
        this.permissionService = permissionService;
    }

    public List<ProductDto> listProducts(Long farmId) {
        requireFarmView(farmId);
        return productRepository.findByFarmId(farmId).stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    public Optional<ProductDto> createProduct(Long farmId, ProductInput input) {
        Farm farm = requireFarmEdit(farmId);
        Product p = new Product();
        p.setName(input.getName());
        p.setFarm(farm);
        p.setCreatedAt(LocalDateTime.now());
        p.setModifiedAt(LocalDateTime.now());
        // optional relationships are left null for now; extend when lookup tables are exposed
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

    public Optional<ToolDto> createTool(Long farmId, ToolInput input) {
        Farm farm = requireFarmEdit(farmId);
        Tool tool = new Tool();
        tool.setName(input.getName());
        tool.setFarm(farm);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setModifiedAt(LocalDateTime.now());
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
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getProductType() != null ? p.getProductType().getId() : null,
                p.getUnit() != null ? p.getUnit().getId() : null,
                p.getFarm() != null ? p.getFarm().getId() : null,
                p.getCreatedAt(),
                p.getModifiedAt()
        );
    }

    private ToolDto toToolDto(Tool t) {
        return new ToolDto(
                t.getId(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getFarm() != null ? t.getFarm().getId() : null,
                t.getCreatedAt(),
                t.getModifiedAt()
        );
    }

    private Farm requireFarmView(Long farmId) {
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, username)) {
            throw new RuntimeException("You can only view assets for farms you can access");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    private Farm requireFarmEdit(Long farmId) {
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, username)) {
            throw new RuntimeException("You can only manage assets for farms you can edit");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }
}

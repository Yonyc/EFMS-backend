package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.dto.ProductInput;
import yt.wer.efms.dto.ToolDto;
import yt.wer.efms.dto.ToolInput;
import yt.wer.efms.service.FarmAssetService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/farm/{farmId}")
public class FarmAssetController {
    private final FarmAssetService farmAssetService;

    public FarmAssetController(FarmAssetService farmAssetService) {
        this.farmAssetService = farmAssetService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> listProducts(@PathVariable Long farmId) {
        try {
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
    public ResponseEntity<List<ToolDto>> listTools(@PathVariable Long farmId) {
        try {
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

    @DeleteMapping("/tools/{toolId}")
    public ResponseEntity<Void> deleteTool(@PathVariable Long farmId, @PathVariable Long toolId) {
        try {
            farmAssetService.deleteTool(farmId, toolId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).build();
        }
    }
}

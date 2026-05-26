package yt.wer.efms.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.ToolCategoryDto;
import yt.wer.efms.model.ToolCategory;
import yt.wer.efms.repository.ToolCategoryRepository;
import yt.wer.efms.service.PermissionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ToolCategoryController {
    private final ToolCategoryRepository toolCategoryRepository;
    private final PermissionService permissionService;

    public ToolCategoryController(ToolCategoryRepository toolCategoryRepository, PermissionService permissionService) {
        this.toolCategoryRepository = toolCategoryRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/tool-categories")
    public ResponseEntity<List<ToolCategoryDto>> listToolCategories() {
        List<ToolCategoryDto> categories = toolCategoryRepository.findAll(Sort.by("id").ascending()).stream()
                .map(c -> new ToolCategoryDto(c.getId(), c.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/tool-categories")
    public ResponseEntity<ToolCategoryDto> createToolCategory(@RequestBody ToolCategoryDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ToolCategory category = new ToolCategory();
        category.setName(input.getName().trim());
        category.setCreatedAt(LocalDateTime.now());
        category.setModifiedAt(LocalDateTime.now());
        ToolCategory saved = toolCategoryRepository.save(category);
        return ResponseEntity.ok(new ToolCategoryDto(saved.getId(), saved.getName()));
    }

    @PutMapping("/tool-categories/{id}")
    public ResponseEntity<ToolCategoryDto> renameToolCategory(@PathVariable Long id,
                                                              @RequestBody ToolCategoryDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return toolCategoryRepository.findById(id).map(category -> {
            category.setName(input.getName().trim());
            category.setModifiedAt(LocalDateTime.now());
            ToolCategory saved = toolCategoryRepository.save(category);
            return ResponseEntity.ok(new ToolCategoryDto(saved.getId(), saved.getName()));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tool-categories/{id}")
    public ResponseEntity<Void> deleteToolCategory(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (!toolCategoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        toolCategoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.UnitDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.UnitRepository;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UnitController {
    private final UnitRepository unitRepository;
    private final FarmRepository farmRepository;
    private final PermissionService permissionService;

    public UnitController(UnitRepository unitRepository,
                          FarmRepository farmRepository,
                          PermissionService permissionService) {
        this.unitRepository = unitRepository;
        this.farmRepository = farmRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/units")
    public ResponseEntity<List<UnitDto>> listUnits(
            @RequestParam(required = false) Long farmId) {
        List<UnitDto> units = (farmId != null
                ? unitRepository.findByFarmIsNullOrFarmIdOrderByIdAsc(farmId)
                : unitRepository.findByFarmIsNullOrderByIdAsc())
                .stream()
                .map(u -> new UnitDto(u.getId(), u.getValue(),
                        u.getFarm() != null ? u.getFarm().getId() : null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(units);
    }

    @PostMapping("/units")
    public ResponseEntity<UnitDto> createUnit(@RequestBody UnitDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getValue() == null || input.getValue().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Unit unit = new Unit();
        unit.setValue(input.getValue().trim());
        unit.setCreatedAt(LocalDateTime.now());
        unit.setModifiedAt(LocalDateTime.now());
        Unit saved = unitRepository.save(unit);
        return ResponseEntity.created(URI.create("/units/" + saved.getId()))
                .body(new UnitDto(saved.getId(), saved.getValue(), null));
    }

    @PutMapping("/units/{id}")
    public ResponseEntity<UnitDto> updateUnit(@PathVariable Long id, @RequestBody UnitDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getValue() == null || input.getValue().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return unitRepository.findById(id).map(unit -> {
            unit.setValue(input.getValue().trim());
            unit.setModifiedAt(LocalDateTime.now());
            Unit saved = unitRepository.save(unit);
            return ResponseEntity.ok(new UnitDto(saved.getId(), saved.getValue(),
                    saved.getFarm() != null ? saved.getFarm().getId() : null));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/units/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (!unitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        unitRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // clears the farm association so the unit becomes available to all farms
    @PostMapping("/units/{id}/promote")
    public ResponseEntity<UnitDto> promoteUnit(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return unitRepository.findById(id).map(unit -> {
            unit.setFarm(null);
            unit.setModifiedAt(LocalDateTime.now());
            Unit saved = unitRepository.save(unit);
            return ResponseEntity.ok(new UnitDto(saved.getId(), saved.getValue(), null));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

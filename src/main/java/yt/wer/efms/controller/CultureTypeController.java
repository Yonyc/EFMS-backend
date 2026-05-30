package yt.wer.efms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CultureTypeDto;
import yt.wer.efms.model.CultureType;
import yt.wer.efms.model.Farm;
import yt.wer.efms.repository.CultureTypeRepository;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.service.PermissionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/culture-types")
public class CultureTypeController {

    private final CultureTypeRepository cultureTypeRepository;
    private final FarmRepository farmRepository;
    private final PermissionService permissionService;

    public CultureTypeController(CultureTypeRepository cultureTypeRepository,
                                  FarmRepository farmRepository,
                                  PermissionService permissionService) {
        this.cultureTypeRepository = cultureTypeRepository;
        this.farmRepository = farmRepository;
        this.permissionService = permissionService;
    }

    /** GET /culture-types?farmId=X returns global + farm-specific types. */
    @GetMapping
    public ResponseEntity<List<CultureTypeDto>> list(@RequestParam(required = false) Long farmId) {
        List<CultureType> types = farmId != null
                ? cultureTypeRepository.findByFarmIsNullOrFarmIdOrderByCodeAsc(farmId)
                : cultureTypeRepository.findByFarmIsNullOrderByCodeAsc();
        return ResponseEntity.ok(types.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /** POST /culture-types admin creates a global type; farm owner creates a farm-specific type. */
    @PostMapping
    public ResponseEntity<CultureTypeDto> create(
            @RequestBody CultureTypeDto input,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (input.getCode() == null || input.getCode().isBlank()
                || input.getName() == null || input.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        CultureType ct = new CultureType();
        ct.setCode(input.getCode().trim());
        ct.setName(input.getName().trim());
        ct.setNameNl(input.getNameNl() != null ? input.getNameNl().trim() : null);
        ct.setCategory(input.getCategory());
        ct.setColor(input.getColor() != null && !input.getColor().isBlank() ? input.getColor().trim() : null);
        ct.setCreatedAt(LocalDateTime.now());
        ct.setModifiedAt(LocalDateTime.now());

        if (input.getFarmId() != null) {
            Farm farm = farmRepository.findById(input.getFarmId())
                    .orElseThrow(() -> new RuntimeException("Farm not found"));
            if (!farm.getOwner().getUsername().equals(userDetails.getUsername())
                    && !permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            ct.setFarm(farm);
        } else {
            if (!permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(cultureTypeRepository.save(ct)));
    }

    /** PUT /culture-types/{id} update name/nameNl/category. */
    @PutMapping("/{id}")
    public ResponseEntity<CultureTypeDto> update(
            @PathVariable Long id,
            @RequestBody CultureTypeDto input,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cultureTypeRepository.findById(id).map(ct -> {
            if (ct.getFarm() == null && !permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<CultureTypeDto>build();
            }
            if (ct.getFarm() != null
                    && !ct.getFarm().getOwner().getUsername().equals(userDetails.getUsername())
                    && !permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<CultureTypeDto>build();
            }
            if (input.getCode() != null && !input.getCode().isBlank()) ct.setCode(input.getCode().trim());
            if (input.getName() != null && !input.getName().isBlank()) ct.setName(input.getName().trim());
            if (input.getNameNl() != null) ct.setNameNl(input.getNameNl().trim());
            if (input.getCategory() != null) ct.setCategory(input.getCategory());
            if (input.getColor() != null) ct.setColor(input.getColor().isBlank() ? null : input.getColor().trim());
            ct.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(cultureTypeRepository.save(ct)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** DELETE /culture-types/{id} admin or farm owner. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cultureTypeRepository.findById(id).map(ct -> {
            if (ct.getFarm() == null && !permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }
            if (ct.getFarm() != null
                    && !ct.getFarm().getOwner().getUsername().equals(userDetails.getUsername())
                    && !permissionService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }
            cultureTypeRepository.delete(ct);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** POST /culture-types/{id}/promote admin promotes a farm type to global. */
    @PostMapping("/{id}/promote")
    public ResponseEntity<CultureTypeDto> promote(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) return ResponseEntity.status(403).build();
        return cultureTypeRepository.findById(id).map(ct -> {
            ct.setFarm(null);
            ct.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(cultureTypeRepository.save(ct)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CultureTypeDto toDto(CultureType ct) {
        return new CultureTypeDto(
                ct.getId(), ct.getCode(), ct.getName(), ct.getNameNl(),
                ct.getCategory(), ct.getColor(), ct.getFarm() != null ? ct.getFarm().getId() : null
        );
    }
}

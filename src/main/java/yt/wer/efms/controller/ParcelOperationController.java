package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CreateParcelOperationRequest;
import yt.wer.efms.dto.OperationTypeDto;
import yt.wer.efms.dto.ParcelOperationDto;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.service.ParcelOperationService;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class ParcelOperationController {
    private final ParcelOperationService parcelOperationService;
    private final OperationTypeRepository operationTypeRepository;
    private final PermissionService permissionService;

    public ParcelOperationController(ParcelOperationService parcelOperationService,
                                     OperationTypeRepository operationTypeRepository,
                                     PermissionService permissionService) {
        this.parcelOperationService = parcelOperationService;
        this.operationTypeRepository = operationTypeRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("/operations/types")
    public ResponseEntity<?> listOperationTypes(
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && farmId != null) {
            int pageSize = size != null ? size : 20;
            var pageable = org.springframework.data.domain.PageRequest.of(page, pageSize);
            return ResponseEntity.ok(parcelOperationService.listOperationTypes(farmId, pageable));
        }
        return ResponseEntity.ok(parcelOperationService.listOperationTypes(farmId));
    }

    @PostMapping("/operations/types")
    public ResponseEntity<OperationTypeDto> createOperationType(@RequestBody OperationTypeDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        OperationType type = new OperationType();
        type.setName(input.getName().trim());
        type.setCreatedAt(LocalDateTime.now());
        type.setModifiedAt(LocalDateTime.now());
        OperationType saved = operationTypeRepository.save(type);
        return ResponseEntity.created(URI.create("/operations/types/" + saved.getId()))
                .body(toDto(saved));
    }

    @PutMapping("/operations/types/{id}")
    public ResponseEntity<OperationTypeDto> updateOperationType(@PathVariable Long id,
                                                                @RequestBody OperationTypeDto input) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return operationTypeRepository.findById(id).map(type -> {
            if (input.getName() != null && !input.getName().trim().isEmpty()) {
                type.setName(input.getName().trim());
            }
            type.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(operationTypeRepository.save(type)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/operations/types/{id}")
    public ResponseEntity<Void> deleteOperationType(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        if (!operationTypeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        operationTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // clears the farm association so the type becomes available to all farms
    @PostMapping("/operations/types/{id}/promote")
    public ResponseEntity<OperationTypeDto> promoteOperationType(@PathVariable Long id) {
        if (!permissionService.isCurrentUserAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return operationTypeRepository.findById(id).map(type -> {
            type.setFarm(null);
            type.setModifiedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(operationTypeRepository.save(type)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/farm/{farmId}/operations")
    public ResponseEntity<?> listFarmOperations(
            @PathVariable Long farmId,
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime from = dateFrom != null && !dateFrom.isBlank() ? LocalDateTime.parse(dateFrom, fmt) : null;
        LocalDateTime to   = dateTo   != null && !dateTo.isBlank()   ? LocalDateTime.parse(dateTo,   fmt) : null;
        try {
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            return ResponseEntity.ok(parcelOperationService.listOperationsForFarm(farmId, parcelId, typeId, from, to, pageable));
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (msg.contains("access") || msg.contains("authoris") || msg.contains("only view")) {
                return ResponseEntity.status(403).build();
            }
            throw ex;
        }
    }

    @GetMapping("/farm/{farmId}/parcels/{parcelId}/operations")
    public ResponseEntity<?> listOperations(
            @PathVariable Long farmId,
            @PathVariable Long parcelId,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        try {
            if (page != null) {
                int pageSize = size != null ? size : 10;
                return ResponseEntity.ok(parcelOperationService.listOperationsForParcel(farmId, parcelId, shareToken, org.springframework.data.domain.PageRequest.of(page, pageSize)));
            }
            return ResponseEntity.ok(parcelOperationService.listOperationsForParcel(farmId, parcelId, shareToken));
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/farm/{farmId}/parcels/{parcelId}/operations")
    public ResponseEntity<?> createOperation(@PathVariable Long farmId,
                                             @PathVariable Long parcelId,
                                             @RequestBody CreateParcelOperationRequest request) {
        try {
            return parcelOperationService.createOperation(farmId, parcelId, request)
                    .map(dto -> ResponseEntity.created(URI.create("/farm/" + farmId + "/parcels/" + parcelId + "/operations/" + dto.getId()))
                            .body(dto))
                    .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/farm/{farmId}/parcels/{parcelId}/operations/{operationId}")
    public ResponseEntity<?> updateOperation(@PathVariable Long farmId,
                                             @PathVariable Long parcelId,
                                             @PathVariable Long operationId,
                                             @RequestBody CreateParcelOperationRequest request) {
        try {
            return parcelOperationService.updateOperation(farmId, parcelId, operationId, request)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(401).build();
        }
    }

    @DeleteMapping("/farm/{farmId}/parcels/{parcelId}/operations/{operationId}")
    public ResponseEntity<Void> deleteOperation(@PathVariable Long farmId,
                                                @PathVariable Long parcelId,
                                                @PathVariable Long operationId) {
        try {
            boolean deleted = parcelOperationService.deleteOperation(farmId, parcelId, operationId);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(403).build();
        }
    }

    private OperationTypeDto toDto(OperationType t) {
        return new OperationTypeDto(t.getId(), t.getName(),
                t.getFarm() != null ? t.getFarm().getId() : null, null, null);
    }
}

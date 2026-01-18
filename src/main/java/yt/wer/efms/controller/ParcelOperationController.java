package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CreateParcelOperationRequest;
import yt.wer.efms.dto.OperationTypeDto;
import yt.wer.efms.dto.ParcelOperationDto;
import yt.wer.efms.service.ParcelOperationService;

import java.net.URI;
import java.util.List;

@RestController
public class ParcelOperationController {
    private final ParcelOperationService parcelOperationService;

    public ParcelOperationController(ParcelOperationService parcelOperationService) {
        this.parcelOperationService = parcelOperationService;
    }

    @GetMapping("/operations/types")
    public ResponseEntity<List<OperationTypeDto>> listOperationTypes() {
        return ResponseEntity.ok(parcelOperationService.listOperationTypes());
    }

    @GetMapping("/farm/{farmId}/parcels/{parcelId}/operations")
    public ResponseEntity<List<ParcelOperationDto>> listOperations(@PathVariable Long farmId, @PathVariable Long parcelId) {
        try {
            return ResponseEntity.ok(parcelOperationService.listOperationsForParcel(farmId, parcelId));
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
}

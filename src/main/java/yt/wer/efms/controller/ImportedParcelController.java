package yt.wer.efms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.ConvertParcelRequest;
import yt.wer.efms.dto.ImportRecordDto;
import yt.wer.efms.dto.ImportedParcelDto;
import yt.wer.efms.dto.ValidateParcelRequest;
import yt.wer.efms.model.ValidationStatus;
import yt.wer.efms.service.ImportedParcelService;

import java.util.List;

@RestController
@RequestMapping("/imported-parcels")
public class ImportedParcelController {

    @Autowired
    private ImportedParcelService importedParcelService;

    // Get all imports for current user
    @GetMapping("/imports")
    public ResponseEntity<List<ImportRecordDto>> getUserImports(Authentication authentication) {
        String username = authentication.getName();
        List<ImportRecordDto> imports = importedParcelService.getUserImports(username);
        return ResponseEntity.ok(imports);
    }

    // Get all imported parcels from a specific import
    @GetMapping("/imports/{importId}")
    public ResponseEntity<List<ImportedParcelDto>> getImportedParcels(
            @PathVariable Long importId,
            @RequestParam(required = false) ValidationStatus status,
            Authentication authentication) {
        String username = authentication.getName();
        
        List<ImportedParcelDto> parcels;
        if (status != null) {
            parcels = importedParcelService.getImportedParcelsByStatus(importId, status, username);
        } else {
            parcels = importedParcelService.getImportedParcels(importId, username);
        }
        
        return ResponseEntity.ok(parcels);
    }

    // Update validation status of a specific parcel
    @PatchMapping("/{parcelId}/validate")
    public ResponseEntity<ImportedParcelDto> validateParcel(
            @PathVariable Long parcelId,
            @RequestBody ValidateParcelRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ImportedParcelDto updated = importedParcelService.validateParcel(parcelId, request, username);
        return ResponseEntity.ok(updated);
    }

    // Convert approved parcel to actual farm parcel
    @PostMapping("/{parcelId}/convert")
    public ResponseEntity<ImportedParcelDto> convertToParcel(
            @PathVariable Long parcelId,
            @RequestBody ConvertParcelRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ImportedParcelDto converted = importedParcelService.convertToParcel(parcelId, request, username);
        return ResponseEntity.ok(converted);
    }
}

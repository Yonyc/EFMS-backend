package yt.wer.efms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.dto.*;
import yt.wer.efms.model.ImportRecord;
import yt.wer.efms.model.ValidationStatus;
import yt.wer.efms.service.ImportService;
import yt.wer.efms.service.ImportedParcelService;
import yt.wer.efms.dto.UpdateImportRequest;
import yt.wer.efms.dto.ApproveImportRequest;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/imports")
public class ImportController {

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportedParcelService importedParcelService;

    /**
     * POST /imports/upload
     * Upload a shapefile (as .zip) and import parcels into the database.
     * Requires authentication.
     * 
     * Example usage:
     * curl -X POST http://localhost:8080/imports/upload \
     *   -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     *   -F "file=@parcels.zip"
     */
    @PostMapping("/upload")
    public ResponseEntity<ImportResponseDto> uploadShapefile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Import the shapefile
            ImportRecord importRecord = importService.importShapefile(file, userDetails.getUsername());
            
            // Build response
                ImportResponseDto response = new ImportResponseDto(
                    importRecord.getId(),
                    importRecord.getFilename(),
                    importRecord.getName(),
                    importRecord.getImportedParcels() != null ? importRecord.getImportedParcels().size() : 0,
                    importRecord.getCreatedAt(),
                    "Import successful"
                );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ImportRecordDto>> listImports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ImportRecordDto> imports = importedParcelService.getUserImports(userDetails.getUsername());
            return ResponseEntity.ok(imports);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{importId}")
    public ResponseEntity<ImportRecordDto> getImport(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ImportRecordDto importRecord = importedParcelService.getImportRecord(importId, userDetails.getUsername());
            return ResponseEntity.ok(importRecord);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{importId}")
    public ResponseEntity<ImportRecordDto> renameImport(
            @PathVariable Long importId,
            @RequestBody UpdateImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ImportRecordDto updated = importedParcelService.renameImport(importId, userDetails.getUsername(), request.getName());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{importId}")
    public ResponseEntity<Void> deleteImport(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            importedParcelService.deleteImport(importId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{importId}/parcels")
    public ResponseEntity<List<ImportedParcelDto>> listImportedParcels(
            @PathVariable Long importId,
            @RequestParam(required = false) ValidationStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (status != null) {
                List<ImportedParcelDto> parcelsByStatus = importedParcelService.getImportedParcelsByStatus(importId, status, userDetails.getUsername());
                return ResponseEntity.ok(parcelsByStatus);
            }
            List<ImportedParcelDto> parcels = importedParcelService.getImportedParcels(importId, userDetails.getUsername());
            return ResponseEntity.ok(parcels);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }

    @PatchMapping("/parcels/{parcelId}")
    public ResponseEntity<ImportedParcelDto> updateImportedParcel(
            @PathVariable Long parcelId,
            @RequestBody UpdateImportedParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ImportedParcelDto dto = importedParcelService.updateImportedParcel(parcelId, request, userDetails.getUsername());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{importId}/assign")
    public ResponseEntity<AssignImportResponse> assignImport(
            @PathVariable Long importId,
            @RequestBody AssignImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            AssignImportResponse response = importedParcelService.assignImportToFarm(importId, request, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{importId}/approve")
    public ResponseEntity<Void> approveImport(
            @PathVariable Long importId,
            @RequestBody(required = false) ApproveImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long farmId = request != null ? request.getFarmId() : null;
            importedParcelService.approveImport(importId, userDetails.getUsername(), farmId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

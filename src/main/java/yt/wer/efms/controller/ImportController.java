package yt.wer.efms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.dto.ImportResponseDto;
import yt.wer.efms.model.ImportRecord;
import yt.wer.efms.service.ImportService;

@RestController
@RequestMapping("/imports")
public class ImportController {

    @Autowired
    private ImportService importService;

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
    public ResponseEntity<?> uploadShapefile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        try {
            // Import the shapefile
            ImportRecord importRecord = importService.importShapefile(file, userDetails.getUsername());
            
            // Build response
            ImportResponseDto response = new ImportResponseDto(
                    importRecord.getId(),
                    importRecord.getFilename(),
                    importRecord.getImportedParcels() != null ? importRecord.getImportedParcels().size() : 0,
                    importRecord.getCreatedAt(),
                    "Import successful"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import failed: " + e.getMessage());
        }
    }
}

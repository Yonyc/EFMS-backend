package yt.wer.efms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.dto.AttachmentDto;
import yt.wer.efms.service.AttachmentService;

import java.net.URI;

@RestController
@RequestMapping("/farm/{farmId}")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/parcels/{parcelId}/operations/{operationId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadToOperation(
            @PathVariable Long farmId,
            @PathVariable Long parcelId,
            @PathVariable Long operationId,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("missing_file");
        try {
            AttachmentDto dto = attachmentService.uploadToOperation(farmId, operationId, file);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/attachments/" + dto.getId())).body(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PostMapping(value = "/products/{productId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadToProduct(
            @PathVariable Long farmId,
            @PathVariable Long productId,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("missing_file");
        try {
            AttachmentDto dto = attachmentService.uploadToProduct(farmId, productId, file);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/attachments/" + dto.getId())).body(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PostMapping(value = "/tools/{toolId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadToTool(
            @PathVariable Long farmId,
            @PathVariable Long toolId,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("missing_file");
        try {
            AttachmentDto dto = attachmentService.uploadToTool(farmId, toolId, file);
            return ResponseEntity.created(URI.create("/farm/" + farmId + "/attachments/" + dto.getId())).body(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long farmId,
            @PathVariable Long attachmentId) {
        try {
            attachmentService.delete(farmId, attachmentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }
}

package yt.wer.efms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yt.wer.efms.dto.*;
import yt.wer.efms.model.ImportRecord;
import yt.wer.efms.model.ParcelStatus;
import yt.wer.efms.service.ImportService;
import yt.wer.efms.service.StagedParcelService;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/imports")
public class ImportController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportController.class);

    @Autowired private ImportService importService;
    @Autowired private StagedParcelService stagedParcelService;
    @Autowired private ObjectMapper objectMapper;

    @PostMapping("/upload")
    public ResponseEntity<ImportResponseDto> uploadShapefile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        try {
            ImportRecord importRecord = importService.importShapefile(file, userDetails.getUsername());
            ImportResponseDto response = new ImportResponseDto(
                    importRecord.getId(), importRecord.getFilename(), importRecord.getName(),
                    importRecord.getParcels() != null ? importRecord.getParcels().size() : 0,
                    importRecord.getCreatedAt(), "Import successful");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/upload/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadShapefileStream(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            trySend(emitter, "error", "{\"message\":\"No files provided\"}");
            emitter.complete();
            return emitter;
        }

        final List<ImportService.FileEntry> fileEntries = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                fileEntries.add(new ImportService.FileEntry(file.getOriginalFilename(), file.getBytes()));
            } catch (IOException e) {
                trySend(emitter, "error", "{\"message\":\"Failed to read upload: " + file.getOriginalFilename() + "\"}");
                emitter.complete();
                return emitter;
            }
        }

        final String label = fileEntries.size() == 1 ? fileEntries.get(0).name() : fileEntries.size() + " files";
        final String username = userDetails.getUsername();

        Thread importThread = new Thread(() -> {
            try {
                ImportRecord record = importService.detectAndImportMultiple(fileEntries, label, username,
                        (current, total, message) -> {
                            try {
                                Map<String, Object> data = new HashMap<>();
                                data.put("type", "progress");
                                data.put("current", current);
                                data.put("total", total);
                                data.put("message", message);
                                emitter.send(SseEmitter.event().name("progress")
                                        .data(objectMapper.writeValueAsString(data)));
                            } catch (Exception ignored) {}
                        });

                Map<String, Object> done = new HashMap<>();
                done.put("type", "done");
                done.put("importId", record.getId());
                done.put("filename", record.getFilename());
                done.put("count", record.getParcels() != null ? record.getParcels().size() : 0);
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done)));
                emitter.complete();
            } catch (Exception e) {
                trySendError(emitter, e.getMessage() != null ? e.getMessage() : "Import failed");
            }
        });
        importThread.setDaemon(true);
        importThread.setName("import-stream-" + username);
        importThread.start();
        return emitter;
    }


    @GetMapping
    public ResponseEntity<List<ImportRecordDto>> listImports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getUserImports(userDetails.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{importId}")
    public ResponseEntity<ImportRecordDto> getImport(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getImportRecord(importId, userDetails.getUsername()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PatchMapping("/{importId}")
    public ResponseEntity<ImportRecordDto> renameImport(
            @PathVariable Long importId,
            @RequestBody UpdateImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.renameImport(importId, userDetails.getUsername(), request.getName()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{importId}")
    public ResponseEntity<Void> deleteImport(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            stagedParcelService.deleteImport(importId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            log.warn("Failed to delete import {}: {}", importId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @DeleteMapping("/{importId}/files/{fileId}")
    public ResponseEntity<Void> removeSourceFile(
            @PathVariable Long importId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            stagedParcelService.removeSourceFile(importId, fileId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @PostMapping(value = "/{importId}/add-files", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter addFilesToImport(
            @PathVariable Long importId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            trySend(emitter, "error", "{\"message\":\"No files provided\"}");
            emitter.complete();
            return emitter;
        }

        final List<ImportService.FileEntry> fileEntries = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                fileEntries.add(new ImportService.FileEntry(file.getOriginalFilename(), file.getBytes()));
            } catch (IOException e) {
                trySend(emitter, "error", "{\"message\":\"Failed to read upload: " + file.getOriginalFilename() + "\"}");
                emitter.complete();
                return emitter;
            }
        }

        final String username = userDetails.getUsername();
        Thread thread = new Thread(() -> {
            try {
                ImportRecord record = importService.addFilesToImport(importId, fileEntries, username,
                        (current, total, message) -> {
                            try {
                                Map<String, Object> data = new HashMap<>();
                                data.put("type", "progress");
                                data.put("current", current);
                                data.put("total", total);
                                data.put("message", message);
                                emitter.send(SseEmitter.event().name("progress")
                                        .data(objectMapper.writeValueAsString(data)));
                            } catch (Exception ignored) {}
                        });
                Map<String, Object> done = new HashMap<>();
                done.put("type", "done");
                done.put("importId", record.getId());
                done.put("count", record.getParcels() != null ? record.getParcels().size() : 0);
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done)));
                emitter.complete();
            } catch (Exception e) {
                trySendError(emitter, e.getMessage() != null ? e.getMessage() : "Failed to add files");
            }
        });
        thread.setDaemon(true);
        thread.setName("import-add-" + username);
        thread.start();
        return emitter;
    }


    @GetMapping("/{importId}/preview")
    public ResponseEntity<ImportPreviewDto> getImportPreview(
            @PathVariable Long importId,
            @RequestParam(required = false) Long farmId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getImportPreview(importId, userDetails.getUsername(), farmId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{importId}/preview/parcels")
    public ResponseEntity<Map<String, Object>> getPreviewParcels(
            @PathVariable Long importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long farmId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getPreviewParcels(
                    importId, userDetails.getUsername(), farmId, page, size));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{importId}/preview/equipments")
    public ResponseEntity<Map<String, Object>> getPreviewEquipments(
            @PathVariable Long importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getPreviewEquipments(
                    importId, userDetails.getUsername(), page, size));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{importId}/preview/products")
    public ResponseEntity<Map<String, Object>> getPreviewProducts(
            @PathVariable Long importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getPreviewProducts(
                    importId, userDetails.getUsername(), page, size));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{importId}/preview/activities")
    public ResponseEntity<Map<String, Object>> getPreviewActivities(
            @PathVariable Long importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.getPreviewActivities(
                    importId, userDetails.getUsername(), page, size));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @GetMapping("/{importId}/parcels")
    public ResponseEntity<List<ParcelDto>> listImportParcels(
            @PathVariable Long importId,
            @RequestParam(required = false) ParcelStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (status != null) {
                return ResponseEntity.ok(stagedParcelService.getImportParcelsByStatus(
                        importId, status, userDetails.getUsername()));
            }
            return ResponseEntity.ok(stagedParcelService.getImportParcels(importId, userDetails.getUsername()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }

    @PatchMapping("/parcels/{parcelId}")
    public ResponseEntity<ParcelDto> updateStagedParcel(
            @PathVariable Long parcelId,
            @RequestBody UpdateStagedParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(stagedParcelService.updateStagedParcel(
                    parcelId, request, userDetails.getUsername()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/parcels/{parcelId}/promote")
    public ResponseEntity<ParcelDto> promoteSingleParcel(
            @PathVariable Long parcelId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long farmId = body != null ? body.get("farmId") : null;
            return ResponseEntity.ok(stagedParcelService.promoteSingle(
                    parcelId, farmId, userDetails.getUsername()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/parcels/{parcelId}/reject")
    public ResponseEntity<ParcelDto> rejectStagedParcel(
            @PathVariable Long parcelId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String notes = body != null ? body.get("notes") : null;
            return ResponseEntity.ok(stagedParcelService.rejectStagedParcel(
                    parcelId, userDetails.getUsername(), notes));
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
            return ResponseEntity.ok(stagedParcelService.assignImportToFarm(
                    importId, request, userDetails.getUsername()));
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
            stagedParcelService.approveImport(importId, userDetails.getUsername(), farmId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{importId}/reject")
    public ResponseEntity<Void> rejectImport(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            stagedParcelService.rejectStagedRows(importId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    private void trySend(SseEmitter emitter, String name, String data) {
        try { emitter.send(SseEmitter.event().name(name).data(data)); } catch (IOException ignored) {}
    }

    private void trySendError(SseEmitter emitter, String message) {
        try {
            Map<String, String> err = new HashMap<>();
            err.put("type", "error");
            err.put("message", message);
            emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(err)));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }
}

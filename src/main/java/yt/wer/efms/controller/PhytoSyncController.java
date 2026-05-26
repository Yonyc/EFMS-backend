package yt.wer.efms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import yt.wer.efms.dto.PhytoImportResult;
import yt.wer.efms.service.OfficialProductService;
import yt.wer.efms.service.PermissionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/phyto")
public class PhytoSyncController {
    private final OfficialProductService officialProductService;
    private final PermissionService permissionService;

    public PhytoSyncController(OfficialProductService officialProductService, PermissionService permissionService) {
        this.officialProductService = officialProductService;
        this.permissionService = permissionService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        requireAdmin();
        return ResponseEntity.ok(Map.of(
                "logs", officialProductService.listSyncLogs(),
                "syncInProgress", officialProductService.isSyncInProgress()
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        requireAdmin();
        try {
            return ResponseEntity.ok(officialProductService.syncFromRemote());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    private void requireAdmin() {
        if (!permissionService.isCurrentUserAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}

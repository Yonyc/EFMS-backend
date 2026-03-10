package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.CreatePeriodRequest;
import yt.wer.efms.dto.FarmMemberDto;
import yt.wer.efms.dto.FarmMemberRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.dto.ParcelListDto;
import yt.wer.efms.dto.ParcelShareDto;
import yt.wer.efms.dto.ParcelShareRequest;
import yt.wer.efms.dto.PeriodDto;
import yt.wer.efms.service.FarmService;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/farm")
public class FarmController {
    private final FarmService farmService;
    private final PermissionService permissionService;

    public FarmController(FarmService farmService, PermissionService permissionService) {
        this.farmService = farmService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<FarmDto> list() {
        return farmService.listAll();
    }

    @GetMapping("/public")
    public List<FarmDto> listPublic() {
        return farmService.listPublic();
    }

    @GetMapping("/my-farms")
    public ResponseEntity<List<FarmDto>> getMyFarms(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        List<FarmDto> farms = farmService.listUserFarms(username);
        return ResponseEntity.ok(farms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmDto> get(@PathVariable Long id) {
        return farmService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FarmDto> create(@RequestBody FarmDto input) {
        FarmDto created = farmService.create(input.getName(), input.getDescription(), input.getLocation(), input.getIsPublic(), input.getShowName(), input.getShowDescription(), input.getShowLocation());
        return ResponseEntity.created(URI.create("/farm/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmDto> update(@PathVariable Long id, @RequestBody FarmDto input) {
        return farmService.update(id, input).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        farmService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/parcels")
    public ResponseEntity<List<ParcelDto>> listParcels(@PathVariable Long id) {
        List<ParcelDto> parcels = farmService.listParcels(id);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/all")
    public ResponseEntity<List<ParcelListDto>> listParcelsAll(@PathVariable Long id) {
        List<ParcelListDto> parcels = farmService.listParcelSummaries(id);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/viewport")
    public ResponseEntity<List<ParcelDto>> listParcelsViewport(@PathVariable Long id,
                                                               @RequestParam Double minLat,
                                                               @RequestParam Double minLng,
                                                               @RequestParam Double maxLat,
                                                               @RequestParam Double maxLng) {
        List<ParcelDto> parcels = farmService.listParcelsWithinBounds(id, minLat, minLng, maxLat, maxLng);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/search")
    public ResponseEntity<List<ParcelDto>> searchParcels(@PathVariable Long id,
                                                         @RequestParam(required = false) Long periodId,
                                                         @RequestParam(required = false) Long toolId,
                                                         @RequestParam(required = false) Long productId,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                         @RequestParam(required = false) String polygonWkt,
                                                         @RequestParam(required = false) Double minLat,
                                                         @RequestParam(required = false) Double minLng,
                                                         @RequestParam(required = false) Double maxLat,
                                                         @RequestParam(required = false) Double maxLng) {
        List<ParcelDto> parcels = farmService.searchParcels(id, periodId, toolId, productId, startDate, endDate, polygonWkt, minLat, minLng, maxLat, maxLng);
        return ResponseEntity.ok(parcels);
    }

    @PostMapping("/{id}/parcels")
    public ResponseEntity<ParcelDto> createParcel(@PathVariable Long id, @RequestBody CreateParcelRequest request) {
        ParcelDto created = farmService.createParcel(id, request);
        return ResponseEntity.created(URI.create("/farm/" + id + "/parcels/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/parcels/{parcelId}")
    public ResponseEntity<ParcelDto> updateParcel(@PathVariable Long id, @PathVariable Long parcelId, @RequestBody CreateParcelRequest request) {
        return farmService.updateParcel(id, parcelId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/periods")
    public ResponseEntity<List<PeriodDto>> listPeriods(@PathVariable Long id) {
        return ResponseEntity.ok(farmService.listPeriods(id));
    }

    @PostMapping("/{id}/periods")
    public ResponseEntity<PeriodDto> createPeriod(@PathVariable Long id, @RequestBody CreatePeriodRequest request) {
        PeriodDto created = farmService.createPeriod(id, request);
        return ResponseEntity.created(URI.create("/farm/" + id + "/periods/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/periods/{periodId}")
    public ResponseEntity<PeriodDto> updatePeriod(@PathVariable Long id, @PathVariable Long periodId, @RequestBody CreatePeriodRequest request) {
        return farmService.updatePeriod(id, periodId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<FarmMemberDto>> listMembers(@PathVariable Long id) {
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(id, username)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(farmService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<FarmMemberDto> addMember(@PathVariable Long id, @RequestBody FarmMemberRequest request) {
        FarmMemberDto created = farmService.addMember(id, request.getUsername(), request.getRole());
        return ResponseEntity.created(URI.create("/farm/" + id + "/members/" + created.getUserId())).body(created);
    }

    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<FarmMemberDto> updateMember(@PathVariable Long id, @PathVariable Long userId, @RequestBody FarmMemberRequest request) {
        return farmService.updateMember(id, userId, request.getRole())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        farmService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/parcels/{parcelId}/shares")
    public ResponseEntity<List<ParcelShareDto>> listParcelShares(@PathVariable Long id, @PathVariable Long parcelId) {
        return ResponseEntity.ok(farmService.listParcelShares(id, parcelId));
    }

    @PostMapping("/{id}/parcels/{parcelId}/shares")
    public ResponseEntity<ParcelShareDto> addParcelShare(@PathVariable Long id, @PathVariable Long parcelId, @RequestBody ParcelShareRequest request) {
        ParcelShareDto created = farmService.addParcelShare(id, parcelId, request.getUsername(), request.getRole());
        return ResponseEntity.created(URI.create("/farm/" + id + "/parcels/" + parcelId + "/shares/" + created.getUserId())).body(created);
    }

    @PutMapping("/{id}/parcels/{parcelId}/shares/{userId}")
    public ResponseEntity<ParcelShareDto> updateParcelShare(@PathVariable Long id, @PathVariable Long parcelId, @PathVariable Long userId, @RequestBody ParcelShareRequest request) {
        return farmService.updateParcelShare(id, parcelId, userId, request.getRole())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/parcels/{parcelId}/shares/{userId}")
    public ResponseEntity<Void> removeParcelShare(@PathVariable Long id, @PathVariable Long parcelId, @PathVariable Long userId) {
        farmService.removeParcelShare(id, parcelId, userId);
        return ResponseEntity.noContent().build();
    }
}

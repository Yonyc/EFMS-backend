package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.CreatePeriodRequest;
import yt.wer.efms.dto.ClaimResearchZoneShareRequest;
import yt.wer.efms.dto.FarmMemberDto;
import yt.wer.efms.dto.FarmMemberRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.dto.ParcelListDto;
import yt.wer.efms.dto.ParcelShareDto;
import yt.wer.efms.dto.ParcelShareRequest;
import yt.wer.efms.dto.PeriodDto;
import yt.wer.efms.dto.ResearchZoneShareDto;
import yt.wer.efms.dto.ResearchZoneShareRequest;
import yt.wer.efms.service.FarmService;
import yt.wer.efms.service.PermissionService;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    public Object listPublic(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null) {
            int pageSize = size != null ? size : 10;
            return farmService.listPublic(org.springframework.data.domain.PageRequest.of(page, pageSize));
        }
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

    /**
     * Set the current user's default-period preference for the given farm.
     * Body: {@code { "defaultPeriodId": <id or null> }}. Null clears the
     * preference.
     */
    @PutMapping("/{id}/preferences")
    public ResponseEntity<FarmDto> updateMyFarmPreferences(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        Long periodId = body != null ? body.get("defaultPeriodId") : null;
        FarmDto updated = farmService.setDefaultPeriod(id, periodId, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @PostMapping
    public ResponseEntity<FarmDto> create(@RequestBody FarmDto input) {
        FarmDto created = farmService.create(input.getName(), input.getDescription(), input.getLocation(),
                input.getIsPublic(), input.getShowName(), input.getShowDescription(), input.getShowLocation());
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
    public ResponseEntity<List<ParcelDto>> listParcels(@PathVariable Long id,
            @RequestParam(required = false) String shareToken) {
        List<ParcelDto> parcels = farmService.listParcels(id, shareToken);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/all")
    public ResponseEntity<List<ParcelListDto>> listParcelsAll(@PathVariable Long id,
            @RequestParam(required = false) String shareToken) {
        List<ParcelListDto> parcels = farmService.listParcelSummaries(id, shareToken);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/page")
    public ResponseEntity<org.springframework.data.domain.Page<ParcelDto>> listParcelsPaged(@PathVariable Long id,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(0, page), Math.min(Math.max(1, size), 200),
                org.springframework.data.domain.Sort.by("id"));
        return ResponseEntity.ok(farmService.searchParcelsPaged(id, search, shareToken, pageable));
    }

    @GetMapping("/{id}/parcels/viewport")
    public ResponseEntity<List<ParcelDto>> listParcelsViewport(@PathVariable Long id,
            @RequestParam(required = false) String shareToken,
            @RequestParam Double minLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLat,
            @RequestParam Double maxLng,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) List<Long> periodIds,
            @RequestParam(required = false) Long shareId) {
        Set<Long> resolvedPeriodIds = mergeFilterValues(periodId, periodIds);
        List<ParcelDto> parcels = farmService.listParcelsWithinBounds(id, minLat, minLng, maxLat, maxLng, shareToken,
                resolvedPeriodIds, shareId);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/{id}/parcels/search")
    public ResponseEntity<List<ParcelDto>> searchParcels(@PathVariable Long id,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false) Long shareId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) List<Long> periodIds,
            @RequestParam(required = false) Long operationTypeId,
            @RequestParam(required = false) List<Long> operationTypeIds,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) List<Long> toolIds,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String polygonWkt,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLng) {
        Set<Long> resolvedPeriodIds = mergeFilterValues(periodId, periodIds);
        Set<Long> resolvedOperationTypeIds = mergeFilterValues(operationTypeId, operationTypeIds);
        Set<Long> resolvedToolIds = mergeFilterValues(toolId, toolIds);
        Set<Long> resolvedProductIds = mergeFilterValues(productId, productIds);

        List<ParcelDto> parcels = farmService.searchParcels(
                id,
                resolvedPeriodIds,
                resolvedOperationTypeIds,
                resolvedToolIds,
                resolvedProductIds,
                startDate,
                endDate,
                polygonWkt,
                minLat,
                minLng,
                maxLat,
                maxLng,
                shareToken,
                shareId);
        return ResponseEntity.ok(parcels);
    }

    private Set<Long> mergeFilterValues(Long singleValue, List<Long> multiValues) {
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        if (multiValues != null) {
            for (Long value : multiValues) {
                if (value != null) {
                    merged.add(value);
                }
            }
        }
        if (singleValue != null) {
            merged.add(singleValue);
        }
        return merged.isEmpty() ? null : merged;
    }

    @GetMapping("/{id}/research-shares")
    public ResponseEntity<List<ResearchZoneShareDto>> listResearchZoneShares(@PathVariable Long id) {
        return ResponseEntity.ok(farmService.listResearchZoneShares(id));
    }

    @GetMapping("/{id}/research-shares/enrolled")
    public ResponseEntity<List<ResearchZoneShareDto>> listEnrolledResearchZoneShares(@PathVariable Long id) {
        return ResponseEntity.ok(farmService.listEnrolledResearchZoneShares(id));
    }

    @GetMapping("/{id}/research-shares/filter-options")
    public ResponseEntity<List<yt.wer.efms.dto.ShareFilterOptionsDto>> listShareFilterOptions(
            @PathVariable Long id,
            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(farmService.listShareFilterOptions(id, shareToken));
    }

    @PostMapping("/{id}/research-shares")
    public ResponseEntity<ResearchZoneShareDto> addResearchZoneShare(@PathVariable Long id,
            @RequestBody ResearchZoneShareRequest request) {
        ResearchZoneShareDto created = farmService.addResearchZoneShare(id, request);
        return ResponseEntity.created(URI.create("/farm/" + id + "/research-shares/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/research-shares/{shareId}")
    public ResponseEntity<ResearchZoneShareDto> updateResearchZoneShare(@PathVariable Long id,
            @PathVariable Long shareId,
            @RequestBody ResearchZoneShareRequest request) {
        return farmService.updateResearchZoneShare(id, shareId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/research-shares/claim")
    public ResponseEntity<ResearchZoneShareDto> claimResearchZoneShare(@PathVariable Long id,
            @RequestBody ClaimResearchZoneShareRequest request) {
        return farmService.claimResearchZoneShare(id, request.getToken())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/research-shares/{shareId}")
    public ResponseEntity<Void> removeResearchZoneShare(@PathVariable Long id, @PathVariable Long shareId) {
        farmService.removeResearchZoneShare(id, shareId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/research-shares/{shareId}/enrollment")
    public ResponseEntity<ResearchZoneShareDto> leaveResearchZoneShare(@PathVariable Long id,
            @PathVariable Long shareId) {
        return farmService.leaveResearchZoneShare(id, shareId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/research-shares/resolve")
    public ResponseEntity<ResearchZoneShareDto> resolveResearchZoneShare(@RequestParam String token) {
        return farmService.resolveResearchZoneShare(token)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/parcels")
    public ResponseEntity<ParcelDto> createParcel(@PathVariable Long id, @RequestBody CreateParcelRequest request) {
        ParcelDto created = farmService.createParcel(id, request);
        return ResponseEntity.created(URI.create("/farm/" + id + "/parcels/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/parcels/{parcelId}")
    public ResponseEntity<ParcelDto> updateParcel(@PathVariable Long id, @PathVariable Long parcelId,
            @RequestBody CreateParcelRequest request) {
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
    public ResponseEntity<PeriodDto> updatePeriod(@PathVariable Long id, @PathVariable Long periodId,
            @RequestBody CreatePeriodRequest request) {
        return farmService.updatePeriod(id, periodId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<FarmMemberDto>> listMembers(@PathVariable Long id) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(id, actionUserId, isAdmin)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(farmService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<FarmMemberDto> addMember(@PathVariable Long id, @RequestBody FarmMemberRequest request) {
        FarmMemberDto created = farmService.addMember(id, request.getUserId(), request.getUsername(),
                request.getRole());
        return ResponseEntity.created(URI.create("/farm/" + id + "/members/" + created.getUserId())).body(created);
    }

    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<FarmMemberDto> updateMember(@PathVariable Long id, @PathVariable Long userId,
            @RequestBody FarmMemberRequest request) {
        return farmService.updateMember(id, userId, request.getRole())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        farmService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/parcel-shares")
    public ResponseEntity<List<yt.wer.efms.dto.FarmParcelShareDto>> listFarmParcelShares(@PathVariable Long id) {
        return ResponseEntity.ok(farmService.listFarmParcelShares(id));
    }

    @GetMapping("/{id}/parcels/{parcelId}/shares")
    public ResponseEntity<List<ParcelShareDto>> listParcelShares(@PathVariable Long id, @PathVariable Long parcelId) {
        return ResponseEntity.ok(farmService.listParcelShares(id, parcelId));
    }

    @PostMapping("/{id}/parcels/{parcelId}/shares")
    public ResponseEntity<ParcelShareDto> addParcelShare(@PathVariable Long id, @PathVariable Long parcelId,
            @RequestBody ParcelShareRequest request) {
        ParcelShareDto created = farmService.addParcelShare(id, parcelId, request.getUsername(), request.getRole(),
                request.getIncludeChildren());
        return ResponseEntity
                .created(URI.create("/farm/" + id + "/parcels/" + parcelId + "/shares/" + created.getUserId()))
                .body(created);
    }

    @PutMapping("/{id}/parcels/{parcelId}/shares/{userId}")
    public ResponseEntity<ParcelShareDto> updateParcelShare(@PathVariable Long id, @PathVariable Long parcelId,
            @PathVariable Long userId, @RequestBody ParcelShareRequest request) {
        return farmService.updateParcelShare(id, parcelId, userId, request.getRole(), request.getIncludeChildren())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/parcels/{parcelId}/shares/{userId}")
    public ResponseEntity<Void> removeParcelShare(@PathVariable Long id, @PathVariable Long parcelId,
            @PathVariable Long userId) {
        farmService.removeParcelShare(id, parcelId, userId);
        return ResponseEntity.noContent().build();
    }
}

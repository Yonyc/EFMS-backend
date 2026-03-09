package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.CreatePeriodRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.dto.PeriodDto;
import yt.wer.efms.service.FarmService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/farm")
public class FarmController {
    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
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
}

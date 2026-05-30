package yt.wer.efms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.service.FarmService;

import java.util.List;

@RestController
@RequestMapping("/parcels")
public class ParcelController {

    @Autowired
    private FarmService farmService;

    @GetMapping
    public ResponseEntity<List<ParcelDto>> listAllParcels() {
        List<ParcelDto> parcels = farmService.listAllParcels();
        return ResponseEntity.ok(parcels);
    }

    // Get a specific parcel by ID
    @GetMapping("/{id}")
    public ResponseEntity<ParcelDto> getParcel(@PathVariable Long id) {
        return farmService.findParcelById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a parcel by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParcel(@PathVariable Long id) {
        boolean deleted = farmService.deleteParcel(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{parcelId}/periods/{parcelPeriodId}/active")
    public ResponseEntity<ParcelDto> setParcelPeriodActive(@PathVariable Long parcelId,
                                                            @PathVariable Long parcelPeriodId,
                                                            @RequestBody java.util.Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(farmService.setParcelPeriodActive(parcelId, parcelPeriodId, active));
    }

    @PostMapping("/{parcelId}/periods")
    public ResponseEntity<ParcelDto> addParcelPeriod(
            @PathVariable Long parcelId,
            @RequestBody yt.wer.efms.dto.ParcelPeriodEditRequest body) {
        return ResponseEntity.ok(farmService.addParcelPeriod(parcelId, body));
    }

    @PatchMapping("/{parcelId}/periods/{parcelPeriodId}")
    public ResponseEntity<ParcelDto> updateParcelPeriod(
            @PathVariable Long parcelId,
            @PathVariable Long parcelPeriodId,
            @RequestBody yt.wer.efms.dto.ParcelPeriodEditRequest body) {
        return ResponseEntity.ok(farmService.updateParcelPeriod(parcelId, parcelPeriodId, body));
    }

    @DeleteMapping("/{parcelId}/periods/{parcelPeriodId}")
    public ResponseEntity<ParcelDto> deleteParcelPeriod(
            @PathVariable Long parcelId,
            @PathVariable Long parcelPeriodId) {
        return ResponseEntity.ok(farmService.deleteParcelPeriod(parcelId, parcelPeriodId));
    }
}

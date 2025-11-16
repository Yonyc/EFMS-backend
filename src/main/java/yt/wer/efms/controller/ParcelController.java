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

    // Get all parcels (across all farms)
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
}

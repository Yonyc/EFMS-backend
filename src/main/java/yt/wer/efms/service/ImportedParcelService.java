package yt.wer.efms.service;

import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.ConvertParcelRequest;
import yt.wer.efms.dto.ImportRecordDto;
import yt.wer.efms.dto.ImportedParcelDto;
import yt.wer.efms.dto.ValidateParcelRequest;
import yt.wer.efms.model.*;
import yt.wer.efms.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImportedParcelService {

    @Autowired
    private ImportRecordRepository importRecordRepository;

    @Autowired
    private ImportedParcelRepository importedParcelRepository;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private FarmRepository farmRepository;

    private final WKTWriter wktWriter = new WKTWriter();

    // Get all imports for a user
    public List<ImportRecordDto> getUserImports(String username) {
        List<ImportRecord> imports = importRecordRepository.findByUserUsernameOrderByCreatedAtDesc(username);
        return imports.stream().map(this::toImportRecordDto).collect(Collectors.toList());
    }

    // Get imported parcels by import ID
    public List<ImportedParcelDto> getImportedParcels(Long importId, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        // Check ownership
        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        List<ImportedParcel> parcels = importedParcelRepository.findByImportRecordId(importId);
        return parcels.stream().map(this::toImportedParcelDto).collect(Collectors.toList());
    }

    // Get parcels by status
    public List<ImportedParcelDto> getImportedParcelsByStatus(Long importId, ValidationStatus status, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        // Check ownership
        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        List<ImportedParcel> parcels = importedParcelRepository.findByImportRecordIdAndValidationStatus(importId, status);
        return parcels.stream().map(this::toImportedParcelDto).collect(Collectors.toList());
    }

    // Update validation status of a parcel
    @Transactional
    public ImportedParcelDto validateParcel(Long parcelId, ValidateParcelRequest request, String username) {
        ImportedParcel parcel = importedParcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Imported parcel not found"));

        // Check ownership
        if (!parcel.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }

        // Don't allow changing status if already converted
        if (parcel.getValidationStatus() == ValidationStatus.CONVERTED) {
            throw new RuntimeException("Cannot modify converted parcel");
        }

        parcel.setValidationStatus(request.getValidationStatus());
        parcel.setValidationNotes(request.getValidationNotes());
        parcel.setModifiedAt(LocalDateTime.now());

        ImportedParcel saved = importedParcelRepository.save(parcel);
        return toImportedParcelDto(saved);
    }

    // Convert approved parcel to actual Parcel in a Farm
    @Transactional
    public ImportedParcelDto convertToParcel(Long importedParcelId, ConvertParcelRequest request, String username) {
        ImportedParcel importedParcel = importedParcelRepository.findById(importedParcelId)
                .orElseThrow(() -> new RuntimeException("Imported parcel not found"));

        // Check ownership
        if (!importedParcel.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }

        // Must be approved to convert
        if (importedParcel.getValidationStatus() != ValidationStatus.APPROVED) {
            throw new RuntimeException("Only approved parcels can be converted");
        }

        // Check if already converted
        if (importedParcel.getValidationStatus() == ValidationStatus.CONVERTED) {
            throw new RuntimeException("Parcel already converted");
        }

        // Get the farm and verify ownership
        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        if (!farm.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("You can only add parcels to your own farms");
        }

        // Create new Parcel from ImportedParcel
        Parcel newParcel = new Parcel();
        newParcel.setName(request.getParcelName());
        newParcel.setFarm(farm);
        newParcel.setColor(request.getColor());
        newParcel.setActive(true);
        newParcel.setStartValidity(LocalDateTime.now());
        newParcel.setCreatedAt(LocalDateTime.now());
        newParcel.setCorrespondingPac(importedParcel);

        // Copy Geometry directly (no need to convert to WKT anymore)
        if (importedParcel.getGeodata() != null) {
            newParcel.setGeodata(importedParcel.getGeodata());
        }

        Parcel savedParcel = parcelRepository.save(newParcel);

        // Update imported parcel status
        importedParcel.setValidationStatus(ValidationStatus.CONVERTED);
        importedParcel.setParcel(savedParcel);
        importedParcel.setModifiedAt(LocalDateTime.now());
        ImportedParcel updated = importedParcelRepository.save(importedParcel);

        return toImportedParcelDto(updated);
    }

    // Helper method to convert entity to DTO
    private ImportedParcelDto toImportedParcelDto(ImportedParcel parcel) {
        ImportedParcelDto dto = new ImportedParcelDto();
        dto.setId(parcel.getId());
        dto.setCreatedAt(parcel.getCreatedAt());
        dto.setDate(parcel.getDate());
        dto.setValidationStatus(parcel.getValidationStatus());
        dto.setValidationNotes(parcel.getValidationNotes());

        if (parcel.getImportRecord() != null) {
            dto.setImportRecordId(parcel.getImportRecord().getId());
        }

        if (parcel.getParcel() != null) {
            dto.setConvertedParcelId(parcel.getParcel().getId());
        }

        // Convert geometry to WKT
        if (parcel.getGeodata() != null) {
            try {
                String wkt = wktWriter.write(parcel.getGeodata());
                dto.setGeodataWkt(wkt);
            } catch (Exception e) {
                dto.setGeodataWkt(null);
            }
        }

        return dto;
    }

    private ImportRecordDto toImportRecordDto(ImportRecord record) {
        ImportRecordDto dto = new ImportRecordDto();
        dto.setId(record.getId());
        dto.setFilename(record.getFilename());
        dto.setCreatedAt(record.getCreatedAt());
        if (record.getUser() != null) {
            dto.setUsername(record.getUser().getUsername());
        }

        // Count parcels by status
        List<ImportedParcel> parcels = record.getImportedParcels().stream().collect(Collectors.toList());
        dto.setTotalParcels(parcels.size());
        dto.setPendingParcels((int) parcels.stream()
                .filter(p -> p.getValidationStatus() == ValidationStatus.PENDING).count());
        dto.setApprovedParcels((int) parcels.stream()
                .filter(p -> p.getValidationStatus() == ValidationStatus.APPROVED).count());
        dto.setRejectedParcels((int) parcels.stream()
                .filter(p -> p.getValidationStatus() == ValidationStatus.REJECTED).count());
        dto.setConvertedParcels((int) parcels.stream()
                .filter(p -> p.getValidationStatus() == ValidationStatus.CONVERTED).count());

        return dto;
    }
}

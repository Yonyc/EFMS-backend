package yt.wer.efms.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.*;
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

    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    // Get all imports for a user
    public List<ImportRecordDto> getUserImports(String username) {
        List<ImportRecord> imports = importRecordRepository.findByUserUsernameOrderByCreatedAtDesc(username);
        return imports.stream().map(this::toImportRecordDto).collect(Collectors.toList());
    }

    public ImportRecordDto getImportRecord(Long importId, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        return toImportRecordDto(importRecord);
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

        ValidationStatus requestedStatus = request.getValidationStatus();

        // Block updates if parcel already materialized, but allow idempotent approve/convert calls
        if (parcel.getValidationStatus() == ValidationStatus.CONVERTED || parcel.getParcel() != null) {
            boolean requestingMaterialized = requestedStatus == ValidationStatus.APPROVED || requestedStatus == ValidationStatus.CONVERTED;
            boolean notesUnchanged = request.getValidationNotes() == null || request.getValidationNotes().equals(parcel.getValidationNotes());
            if (requestingMaterialized && notesUnchanged) {
                return toImportedParcelDto(parcel);
            }
            throw new RuntimeException("Cannot modify a parcel that has already been materialized");
        }

        parcel.setValidationNotes(request.getValidationNotes());
        parcel.setModifiedAt(LocalDateTime.now());

        if (requestedStatus == ValidationStatus.APPROVED) {
            Parcel existingParcel = parcelRepository.findByCorrespondingPacId(parcel.getId());
            if (existingParcel != null) {
                parcel.setParcel(existingParcel);
                parcel.setValidationStatus(ValidationStatus.APPROVED);
                ImportedParcel saved = importedParcelRepository.save(parcel);
                return toImportedParcelDto(saved);
            }

            Farm farm = null;
            Long farmId = request.getFarmId();
            if (farmId != null) {
                farm = farmRepository.findById(farmId)
                        .orElseThrow(() -> new RuntimeException("Farm not found"));
                if (!farm.getOwner().getUsername().equals(username)) {
                    throw new RuntimeException("You can only add parcels to your own farms");
                }
            } else {
                List<Farm> farms = farmRepository.findByOwnerUsername(username);
                if (farms.size() == 1) {
                    farm = farms.get(0);
                } else if (farms.isEmpty()) {
                    throw new RuntimeException("No farm available to attach the parcel");
                } else {
                    throw new RuntimeException("Farm id is required when multiple farms exist");
                }
            }

            // Auto-create a Parcel mirroring the imported geometry
            Parcel newParcel = new Parcel();
            newParcel.setName("Imported Parcel " + parcel.getId());
            newParcel.setFarm(farm);
            newParcel.setActive(true);
            newParcel.setStartValidity(LocalDateTime.now());
            newParcel.setCreatedAt(LocalDateTime.now());
            newParcel.setColor("#4CAF50");
            newParcel.setCorrespondingPac(parcel);
            if (parcel.getGeodata() != null) {
                newParcel.setGeodata(parcel.getGeodata());
            }
            Parcel savedParcel = parcelRepository.save(newParcel);

            parcel.setParcel(savedParcel);
            parcel.setValidationStatus(ValidationStatus.APPROVED);
        } else {
            parcel.setValidationStatus(requestedStatus);
        }

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
        if (importedParcel.getValidationStatus() == ValidationStatus.CONVERTED || importedParcel.getParcel() != null) {
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

    @Transactional
    public ImportedParcelDto updateImportedParcel(Long parcelId, UpdateImportedParcelRequest request, String username) {
        ImportedParcel parcel = importedParcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Imported parcel not found"));

        if (!parcel.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }

        if (parcel.getValidationStatus() == ValidationStatus.CONVERTED || parcel.getParcel() != null) {
            throw new RuntimeException("Cannot modify converted parcel");
        }

        if (request.getGeodata() != null && !request.getGeodata().isBlank()) {
            try {
                Geometry geometry = wktReader.read(request.getGeodata());
                parcel.setGeodata(geometry);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid geometry payload: " + e.getMessage());
            }
        }

        parcel.setValidationNotes(request.getValidationNotes());
        parcel.setModifiedAt(LocalDateTime.now());

        ImportedParcel saved = importedParcelRepository.save(parcel);
        return toImportedParcelDto(saved);
    }

    @Transactional
    public AssignImportResponse assignImportToFarm(Long importId, AssignImportRequest request, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        if (!farm.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("You can only assign imports to your own farms");
        }

        List<ImportedParcel> parcels = importedParcelRepository.findByImportRecordId(importId);
        boolean approvedOnly = request.getConvertOnlyApproved() == null || request.getConvertOnlyApproved();
        String prefix = (request.getParcelNamePrefix() == null || request.getParcelNamePrefix().isBlank())
                ? "Imported Parcel"
                : request.getParcelNamePrefix();
        String defaultColor = (request.getDefaultColor() == null || request.getDefaultColor().isBlank())
                ? "#4CAF50"
                : request.getDefaultColor();

        int convertedCount = 0;
        int skippedCount = 0;
        int sequence = 1;

        for (ImportedParcel importedParcel : parcels) {
            boolean alreadyConverted = importedParcel.getValidationStatus() == ValidationStatus.CONVERTED;
            boolean notApproved = importedParcel.getValidationStatus() != ValidationStatus.APPROVED;

            if (alreadyConverted || (approvedOnly && notApproved)) {
                skippedCount++;
                continue;
            }

            Parcel newParcel = new Parcel();
            newParcel.setName(prefix + " " + sequence++);
            newParcel.setFarm(farm);
            newParcel.setColor(defaultColor);
            newParcel.setActive(true);
            newParcel.setStartValidity(LocalDateTime.now());
            newParcel.setCreatedAt(LocalDateTime.now());
            newParcel.setCorrespondingPac(importedParcel);
            if (importedParcel.getGeodata() != null) {
                newParcel.setGeodata(importedParcel.getGeodata());
            }

            Parcel savedParcel = parcelRepository.save(newParcel);

            importedParcel.setValidationStatus(ValidationStatus.CONVERTED);
            importedParcel.setParcel(savedParcel);
            importedParcel.setModifiedAt(LocalDateTime.now());
            importedParcelRepository.save(importedParcel);
            convertedCount++;
        }

        return new AssignImportResponse(importId, farm.getId(), convertedCount, skippedCount);
    }

    @Transactional
    public void approveImport(Long importId, String username, Long farmId) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        List<ImportedParcel> parcels = importedParcelRepository.findByImportRecordId(importId);
        LocalDateTime now = LocalDateTime.now();
        Farm resolvedFarm = null;

        for (ImportedParcel parcel : parcels) {
            if (parcel.getValidationStatus() == ValidationStatus.CONVERTED) {
                continue;
            }

            if (parcel.getParcel() != null) {
                parcel.setValidationStatus(ValidationStatus.APPROVED);
                parcel.setModifiedAt(now);
                continue;
            }

            Parcel existingParcel = parcelRepository.findByCorrespondingPacId(parcel.getId());
            if (existingParcel != null) {
                parcel.setParcel(existingParcel);
                parcel.setValidationStatus(ValidationStatus.APPROVED);
                parcel.setModifiedAt(now);
                continue;
            }

            if (resolvedFarm == null) {
                resolvedFarm = resolveApprovalFarm(username, farmId);
            }

            Parcel newParcel = new Parcel();
            newParcel.setName("Imported Parcel " + parcel.getId());
            newParcel.setFarm(resolvedFarm);
            newParcel.setActive(true);
            newParcel.setStartValidity(now);
            newParcel.setCreatedAt(now);
            newParcel.setColor("#4CAF50");
            newParcel.setCorrespondingPac(parcel);
            if (parcel.getGeodata() != null) {
                newParcel.setGeodata(parcel.getGeodata());
            }

            Parcel savedParcel = parcelRepository.save(newParcel);

            parcel.setParcel(savedParcel);
            parcel.setValidationStatus(ValidationStatus.APPROVED);
            parcel.setModifiedAt(now);
        }

        importedParcelRepository.saveAll(parcels);

        if (importRecord.getApprovedAt() == null) {
            importRecord.setApprovedAt(now);
        }
        importRecordRepository.save(importRecord);
    }

    private Farm resolveApprovalFarm(String username, Long farmId) {
        if (farmId != null) {
            Farm farm = farmRepository.findById(farmId)
                    .orElseThrow(() -> new RuntimeException("Farm not found"));
            if (!farm.getOwner().getUsername().equals(username)) {
                throw new RuntimeException("You can only add parcels to your own farms");
            }
            return farm;
        }

        List<Farm> farms = farmRepository.findByOwnerUsername(username);
        if (farms.size() == 1) {
            return farms.get(0);
        }
        if (farms.isEmpty()) {
            throw new RuntimeException("No farm available to attach the parcel");
        }
        throw new RuntimeException("Farm id is required when multiple farms exist");
    }

    @Transactional
    public ImportRecordDto renameImport(Long importId, String username, String name) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        String cleanedName = (name == null || name.isBlank()) ? importRecord.getFilename() : name.trim();
        importRecord.setName(cleanedName);
        importRecordRepository.save(importRecord);

        return toImportRecordDto(importRecord);
    }

    @Transactional
    public void deleteImport(Long importId, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }

        // Detach created parcels so the import can be removed without FK issues
        List<Parcel> parcels = parcelRepository.findByCorrespondingPacImportRecordId(importId);
        for (Parcel parcel : parcels) {
            parcel.setCorrespondingPac(null);
            parcelRepository.save(parcel);
        }

        importedParcelRepository.deleteByImportRecordId(importId);
        importRecordRepository.delete(importRecord);
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
                dto.setGeodata(wkt);
            } catch (Exception e) {
                dto.setGeodata(null);
            }
        }

        return dto;
    }

    private ImportRecordDto toImportRecordDto(ImportRecord record) {
        ImportRecordDto dto = new ImportRecordDto();
        dto.setId(record.getId());
        dto.setFilename(record.getFilename());
        dto.setName(record.getName() != null ? record.getName() : record.getFilename());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setApprovedAt(record.getApprovedAt());
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

package yt.wer.efms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.*;
import yt.wer.efms.model.*;
import yt.wer.efms.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lifecycle manager for parcels staged from an import: lists imports, builds preview pages,
 * updates per-parcel attributes pre-approval, promotes STAGED rows to LIVE on approve/assign,
 * and the cleanup paths.
 *
 * <p>Replaces the previous {@code ImportedParcelService}, but operates directly on the unified
 * {@link Parcel} table filtered by {@link ParcelStatus}, alongside the equally-unified
 * {@link Tool} / {@link Product} / {@link ParcelOperation} tables.</p>
 */
@Service
public class StagedParcelService {

    @Autowired private ImportRecordRepository importRecordRepository;
    @Autowired private ImportSourceFileRepository importSourceFileRepository;
    @Autowired private ParcelRepository parcelRepository;
    @Autowired private FarmRepository farmRepository;
    @Autowired private PeriodRepository periodRepository;
    @Autowired private ParcelPeriodRepository parcelPeriodRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OperationTypeRepository operationTypeRepository;
    @Autowired private ParcelOperationRepository parcelOperationRepository;
    @Autowired private CultureTypeRepository cultureTypeRepository;
    @Autowired private OperationProductRepository operationProductRepository;
    @Autowired private ObjectMapper objectMapper;

    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();


    public List<ImportRecordDto> getUserImports(String username) {
        List<ImportRecord> imports = importRecordRepository.findByUserUsernameOrderByCreatedAtDesc(username);
        return imports.stream().map(this::toImportRecordDto).collect(Collectors.toList());
    }

    public ImportRecordDto getImportRecord(Long importId, String username) {
        ImportRecord importRecord = verifyOwnership(importId, username);
        return toImportRecordDto(importRecord);
    }

    public List<ParcelDto> getImportParcels(Long importId, String username) {
        verifyOwnership(importId, username);
        return parcelRepository.findByImportRecordId(importId).stream()
                .map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelDto> getImportParcelsByStatus(Long importId, ParcelStatus status, String username) {
        verifyOwnership(importId, username);
        return parcelRepository.findByImportRecordIdAndStatus(importId, status).stream()
                .map(this::toParcelDto).collect(Collectors.toList());
    }


    public ImportPreviewDto getImportPreview(Long importId, String username, Long farmId) {
        ImportRecord importRecord = verifyOwnership(importId, username);

        List<Parcel> parcels = parcelRepository.findByImportRecordIdAndStatus(importId, ParcelStatus.STAGED);
        List<ImportSourceFile> sourceFiles = importSourceFileRepository
                .findByImportRecordIdOrderByImportedAtAsc(importId);

        long equipmentCount = toolRepository.findByImportRecordId(importId).size();
        long productCount = productRepository.findByImportRecordId(importId).size();
        long activityCount = parcelOperationRepository.findByImportRecordId(importId).size();

        ImportPreviewDto preview = new ImportPreviewDto();
        preview.setImportId(importRecord.getId());
        preview.setImportName(importRecord.getName() != null ? importRecord.getName() : importRecord.getFilename());
        preview.setCreatedAt(importRecord.getCreatedAt());
        preview.setApprovedAt(importRecord.getApprovedAt());
        preview.setHasActionData(equipmentCount > 0 || productCount > 0 || activityCount > 0);
        preview.setParcelCount(parcels.size());
        preview.setEquipmentCount((int) equipmentCount);
        preview.setProductCount((int) productCount);
        preview.setActivityCount((int) activityCount);

        Map<Long, Long> parcelsByFile = parcels.stream()
                .filter(p -> p.getSourceFile() != null)
                .collect(Collectors.groupingBy(p -> p.getSourceFile().getId(), Collectors.counting()));
        Map<Long, Long> toolsByFile = toolRepository.findByImportRecordId(importId).stream()
                .filter(t -> t.getSourceFile() != null)
                .collect(Collectors.groupingBy(t -> t.getSourceFile().getId(), Collectors.counting()));
        Map<Long, Long> productsByFile = productRepository.findByImportRecordId(importId).stream()
                .filter(p -> p.getSourceFile() != null)
                .collect(Collectors.groupingBy(p -> p.getSourceFile().getId(), Collectors.counting()));
        Map<Long, Long> opsByFile = parcelOperationRepository.findByImportRecordId(importId).stream()
                .filter(o -> o.getSourceFile() != null)
                .collect(Collectors.groupingBy(o -> o.getSourceFile().getId(), Collectors.counting()));

        List<ImportSourceFileDto> fileDtos = sourceFiles.stream().map(sf -> {
            ImportSourceFileDto dto = new ImportSourceFileDto();
            dto.setId(sf.getId());
            dto.setFilename(sf.getFilename());
            dto.setImportedAt(sf.getImportedAt());
            dto.setParcelCount(parcelsByFile.getOrDefault(sf.getId(), 0L).intValue());
            dto.setToolCount(toolsByFile.getOrDefault(sf.getId(), 0L).intValue());
            dto.setProductCount(productsByFile.getOrDefault(sf.getId(), 0L).intValue());
            dto.setOperationCount(opsByFile.getOrDefault(sf.getId(), 0L).intValue());
            return dto;
        }).collect(Collectors.toList());
        preview.setFiles(fileDtos);

        return preview;
    }

    /** Paginated staged parcels section. */
    public Map<String, Object> getPreviewParcels(Long importId, String username, Long farmId, int page, int size) {
        verifyOwnership(importId, username);

        Pageable pageable = PageRequest.of(page, size);
        Page<Parcel> parcelPage = parcelRepository.findByImportRecordIdAndStatus(
                importId, ParcelStatus.STAGED, pageable);

        List<ParcelDto> dtos = parcelPage.getContent().stream().map(p -> {
            ParcelDto dto = toParcelDto(p);
            if (farmId != null && p.getSourceGuid() != null) {
                parcelRepository.findByFarmIdAndSourceGuidAndDeletedAtIsNull(farmId, p.getSourceGuid())
                        .ifPresent(existing -> {
                            dto.setParentParcelId(existing.getId());
                        });
            }
            return dto;
        }).collect(Collectors.toList());

        return pageResponse(dtos, parcelPage.getTotalElements(), parcelPage.getTotalPages(), page, size);
    }

    public Map<String, Object> getPreviewEquipments(Long importId, String username, int page, int size) {
        verifyOwnership(importId, username);
        List<Tool> all = toolRepository.findByImportRecordId(importId);
        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<ImportPreviewDto.EquipmentDto> dtos = all.subList(fromIdx, toIdx).stream().map(t -> {
            ImportPreviewDto.EquipmentDto dto = new ImportPreviewDto.EquipmentDto();
            dto.setId(t.getId());
            dto.setSourceGuid(t.getSourceGuid());
            dto.setName(t.getName());
            return dto;
        }).collect(Collectors.toList());
        return pagedResult(dtos, total, page, size);
    }

    public Map<String, Object> getPreviewProducts(Long importId, String username, int page, int size) {
        verifyOwnership(importId, username);
        List<Product> all = productRepository.findByImportRecordId(importId);
        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<ImportPreviewDto.ProductDto> dtos = all.subList(fromIdx, toIdx).stream().map(p -> {
            ImportPreviewDto.ProductDto dto = new ImportPreviewDto.ProductDto();
            dto.setId(p.getId());
            dto.setSourceGuid(p.getSourceGuid());
            dto.setName(p.getName());
            dto.setCode(p.getImportCode());
            dto.setBotanicalSpecies(p.getImportBotanicalSpecies());
            dto.setUnitSymbol(p.getImportUnitSymbol());
            return dto;
        }).collect(Collectors.toList());
        return pagedResult(dtos, total, page, size);
    }

    public Map<String, Object> getPreviewActivities(Long importId, String username, int page, int size) {
        verifyOwnership(importId, username);
        List<ParcelOperation> all = parcelOperationRepository.findByImportRecordId(importId);
        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);

        Map<String, String> guidToName = parcelRepository.findByImportRecordId(importId).stream()
                .filter(p -> p.getSourceGuid() != null)
                .collect(Collectors.toMap(Parcel::getSourceGuid,
                        p -> p.getSourceName() != null ? p.getSourceName() : p.getSourceGuid(),
                        (a, b) -> a));

        List<ImportPreviewDto.ActivityDto> dtos = all.subList(fromIdx, toIdx).stream().map(op -> {
            ImportPreviewDto.ActivityDto dto = new ImportPreviewDto.ActivityDto();
            dto.setId(op.getId());
            dto.setOperationName(op.getSourceOperationName());
            dto.setOperationCategory(op.getSourceOperationCategory());
            dto.setStartingDate(op.getDate() != null ? op.getDate().toLocalDate().toString() : null);
            if (op.getDurationSeconds() != null) dto.setDurationMinutes(op.getDurationSeconds() / 60);
            if (op.getDate() != null) {
                LocalDate d = op.getDate().toLocalDate();
                int yr = d.getMonthValue() >= 10 ? d.getYear() + 1 : d.getYear();
                dto.setPeriodName((yr - 1) + "-" + yr);
            }

            List<ImportPreviewDto.ParcelRefDto> parcelRefs = new ArrayList<>();
            if (op.getSourceParcelGuids() != null) {
                try {
                    List<String> guids = objectMapper.readValue(op.getSourceParcelGuids(),
                            new TypeReference<List<String>>() {});
                    for (String guid : guids) {
                        ImportPreviewDto.ParcelRefDto ref = new ImportPreviewDto.ParcelRefDto();
                        ref.setPlotId(guid);
                        ref.setParcelName(guidToName.get(guid));
                        parcelRefs.add(ref);
                    }
                } catch (Exception ignored) {}
            }
            dto.setParcels(parcelRefs);

            List<ImportPreviewDto.ProductUsageDto> usages = operationProductRepository
                    .findByOperationId(op.getId()).stream().map(opProd -> {
                        ImportPreviewDto.ProductUsageDto u = new ImportPreviewDto.ProductUsageDto();
                        if (opProd.getProduct() != null) {
                            u.setSupplyId(opProd.getProduct().getSourceGuid());
                            u.setSupplyName(opProd.getProduct().getName());
                        }
                        u.setQuantity(opProd.getQuantity());
                        if (opProd.getUnit() != null) u.setUnitSymbol(opProd.getUnit().getValue());
                        return u;
                    }).collect(Collectors.toList());
            dto.setProductUsages(usages);
            return dto;
        }).collect(Collectors.toList());

        return pagedResult(dtos, total, page, size);
    }

    private Map<String, Object> pagedResult(List<?> content, int total, int page, int size) {
        return pageResponse(content, total, size > 0 ? (int) Math.ceil((double) total / size) : 0, page, size);
    }

    private Map<String, Object> pageResponse(List<?> content, long totalElements, int totalPages, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", size);
        return result;
    }


    @Transactional
    public void removeSourceFile(Long importId, Long fileId, String username) {
        ImportRecord importRecord = verifyOwnership(importId, username);
        if (importRecord.getApprovedAt() != null) {
            throw new RuntimeException("Cannot remove files from an approved import");
        }
        ImportSourceFile sourceFile = importSourceFileRepository
                .findByIdAndImportRecordId(fileId, importId)
                .orElseThrow(() -> new RuntimeException("Source file not found in this import"));

        List<ParcelOperation> ops = parcelOperationRepository.findBySourceFileId(fileId);
        for (ParcelOperation op : ops) {
            operationProductRepository.deleteByOperationId(op.getId());
        }
        parcelOperationRepository.deleteBySourceFileId(fileId);
        productRepository.deleteBySourceFileId(fileId);
        toolRepository.deleteBySourceFileId(fileId);

        List<Parcel> stagedFromFile = parcelRepository.findBySourceFileIdAndStatus(fileId, ParcelStatus.STAGED);
        for (Parcel p : stagedFromFile) {
            parcelPeriodRepository.deleteByParcelId(p.getId());
        }
        parcelRepository.deleteBySourceFileIdAndStatus(fileId, ParcelStatus.STAGED);

        importSourceFileRepository.delete(sourceFile);
    }


    @Transactional
    public ParcelDto updateStagedParcel(Long parcelId, UpdateStagedParcelRequest request, String username) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Staged parcel not found"));

        if (parcel.getImportRecord() == null
                || !parcel.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }
        if (parcel.getStatus() != ParcelStatus.STAGED) {
            throw new RuntimeException("Cannot modify a parcel that is no longer STAGED");
        }

        if (request.getGeodata() != null && !request.getGeodata().isBlank()) {
            try {
                Geometry geometry = wktReader.read(request.getGeodata());
                parcel.setGeodata(geometry);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid geometry payload: " + e.getMessage());
            }
        }

        if (request.getParentParcelId() != null) {
            if (request.getParentParcelId() == 0) {
                parcel.setParentParcel(null);
            } else {
                Parcel parent = parcelRepository.findById(request.getParentParcelId())
                        .orElseThrow(() -> new RuntimeException("Parent parcel not found"));
                if (parent.getImportRecord() == null
                        || !parent.getImportRecord().getId().equals(parcel.getImportRecord().getId())) {
                    throw new RuntimeException("Parent must belong to the same import");
                }
                parcel.setParentParcel(parent);
            }
        }

        parcel.setValidationNotes(request.getValidationNotes());
        parcel.setModifiedAt(LocalDateTime.now());

        ParcelPeriod pp = primaryParcelPeriod(parcel);
        if (pp != null) {
            if (request.getCampaignYear() != null) {
                pp.setCampaignYear(request.getCampaignYear());
            }
            if (request.getForcedPeriodId() != null) {
                Period period = periodRepository.findById(request.getForcedPeriodId())
                        .orElseThrow(() -> new RuntimeException("Period not found"));
                pp.setForcedPeriod(period);
                pp.setPeriodNameOverride(null);
                pp.setPeriodStartOverride(null);
                pp.setPeriodEndOverride(null);
            } else {
                if (request.getPeriodNameOverride() != null) {
                    pp.setPeriodNameOverride(request.getPeriodNameOverride().isBlank()
                            ? null : request.getPeriodNameOverride().trim());
                    pp.setForcedPeriod(null);
                }
                if (request.getPeriodStartOverride() != null) {
                    pp.setPeriodStartOverride(request.getPeriodStartOverride());
                }
                if (request.getPeriodEndOverride() != null) {
                    pp.setPeriodEndOverride(request.getPeriodEndOverride());
                }
            }
            parcelPeriodRepository.save(pp);
        }

        Parcel saved = parcelRepository.save(parcel);
        return toParcelDto(saved);
    }


    @Transactional
    public ParcelDto rejectStagedParcel(Long parcelId, String username, String notes) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Staged parcel not found"));
        if (parcel.getImportRecord() == null
                || !parcel.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }
        if (parcel.getStatus() != ParcelStatus.STAGED) {
            throw new RuntimeException("Only staged parcels can be rejected");
        }
        parcel.setStatus(ParcelStatus.REJECTED);
        parcel.setValidationNotes(notes);
        parcel.setModifiedAt(LocalDateTime.now());
        return toParcelDto(parcelRepository.save(parcel));
    }


    @Transactional
    public AssignImportResponse assignImportToFarm(Long importId, AssignImportRequest request, String username) {
        verifyOwnership(importId, username);

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new RuntimeException("Farm not found"));
        if (!farm.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("You can only assign imports to your own farms");
        }

        List<Parcel> staged = parcelRepository.findByImportRecordIdAndStatus(importId, ParcelStatus.STAGED);
        boolean groupByBlock = Boolean.TRUE.equals(request.getGroupByBlock());
        String prefix = (request.getParcelNamePrefix() == null || request.getParcelNamePrefix().isBlank())
                ? "Imported Parcel" : request.getParcelNamePrefix();
        String defaultColor = (request.getDefaultColor() == null || request.getDefaultColor().isBlank())
                ? "#4CAF50" : request.getDefaultColor();

        int convertedCount = 0;
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        if (groupByBlock) {
            Map<String, List<Parcel>> byBlock = new LinkedHashMap<>();
            for (Parcel p : staged) {
                String key = (p.getSourceBlockCode() != null && !p.getSourceBlockCode().isBlank())
                        ? p.getSourceBlockCode() : null;
                byBlock.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }

            for (Map.Entry<String, List<Parcel>> entry : byBlock.entrySet()) {
                String blockCode = entry.getKey();
                List<Parcel> group = entry.getValue();

                Parcel blockParent = null;
                if (blockCode != null) {
                    blockParent = new Parcel();
                    blockParent.setStatus(ParcelStatus.LIVE);
                    blockParent.setName("Îlot " + blockCode);
                    blockParent.setFarm(farm);
                    blockParent.setColor(defaultColor);
                    blockParent.setCreatedAt(now);
                    blockParent = parcelRepository.save(blockParent);
                }

                int seq = 1;
                for (Parcel staging : group) {
                    Parcel promoted = promoteStaged(staging, farm,
                            resolveParcelName(staging, prefix + " " + seq++),
                            defaultColor, now);
                    if (promoted == null) { skippedCount++; continue; }
                    if (blockParent != null) {
                        promoted.setParentParcel(blockParent);
                        parcelRepository.save(promoted);
                    }
                    convertedCount++;
                }
            }
        } else {
            int sequence = 1;
            for (Parcel staging : staged) {
                Parcel promoted = promoteStaged(staging, farm,
                        resolveParcelName(staging, prefix + " " + sequence++),
                        defaultColor, now);
                if (promoted == null) { skippedCount++; continue; }
                convertedCount++;
            }
        }

        return new AssignImportResponse(importId, farm.getId(), convertedCount, skippedCount);
    }

    /**
     * Promotes a STAGED parcel into LIVE state on the given farm. If a LIVE parcel with the
     * same sourceGuid already exists on the farm, the staged row's ParcelPeriod is migrated
     * onto the existing parcel and the staging row itself is deleted. Returns
     * the live parcel, or {@code null} when the staged row had to be skipped.
     */
    private Parcel promoteStaged(Parcel staging, Farm farm, String name, String color, LocalDateTime now) {
        if (staging.getStatus() != ParcelStatus.STAGED) return null;

        if (staging.getSourceGuid() != null) {
            Optional<Parcel> existing = parcelRepository
                    .findByFarmIdAndSourceGuidAndDeletedAtIsNull(farm.getId(), staging.getSourceGuid());
            if (existing.isPresent()) {
                Parcel target = existing.get();
                migrateStagedPeriodsTo(staging, target);
                parcelRepository.delete(staging);
                return target;
            }
        }

        staging.setStatus(ParcelStatus.LIVE);
        staging.setFarm(farm);
        if (staging.getName() == null || staging.getName().isBlank()) staging.setName(name);
        if (staging.getColor() == null || staging.getColor().isBlank()) staging.setColor(color);
        staging.setModifiedAt(now);

        ParcelPeriod pp = primaryParcelPeriod(staging);
        if (pp != null) {
            Period resolved = resolvePeriodFor(pp, farm);
            if (resolved != null) {
                pp.setPeriod(resolved);
                pp.setActive(true);
                if (pp.getStartValidity() == null) {
                    pp.setStartValidity(resolved.getStartDate() != null ? resolved.getStartDate() : now);
                }
                if (pp.getEndValidity() == null) pp.setEndValidity(resolved.getEndDate());
                parcelPeriodRepository.save(pp);
            }
        }

        return parcelRepository.save(staging);
    }

    /** Move staged ParcelPeriod rows onto the existing live parcel. */
    private void migrateStagedPeriodsTo(Parcel staging, Parcel target) {
        Farm farm = target.getFarm();
        for (ParcelPeriod pp : new ArrayList<>(parcelPeriodRepository.findByParcelId(staging.getId()))) {
            Period resolved = resolvePeriodFor(pp, farm);
            if (resolved == null) {
                parcelPeriodRepository.delete(pp);
                continue;
            }
            if (parcelPeriodRepository.findByParcelIdAndPeriodId(target.getId(), resolved.getId()).isPresent()) {
                parcelPeriodRepository.delete(pp);
                continue;
            }
            pp.setParcel(target);
            pp.setPeriod(resolved);
            parcelPeriodRepository.save(pp);
        }
    }


    @Transactional
    public ParcelDto promoteSingle(Long parcelId, Long farmId, String username) {
        Parcel staged = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Staged parcel not found"));
        if (staged.getImportRecord() == null
                || !staged.getImportRecord().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to parcel");
        }
        if (staged.getStatus() != ParcelStatus.STAGED) {
            throw new RuntimeException("Only STAGED parcels can be promoted");
        }
        Farm farm = resolveApprovalFarm(username, farmId);
        Parcel promoted = promoteStaged(staged,
                farm,
                resolveParcelName(staged, "Imported Parcel " + staged.getId()),
                "#4CAF50",
                LocalDateTime.now());
        if (promoted == null) {
            throw new RuntimeException("Failed to promote parcel");
        }
        return toParcelDto(promoted);
    }


    @Transactional
    public void approveImport(Long importId, String username, Long farmId) {
        ImportRecord importRecord = verifyOwnership(importId, username);

        List<Parcel> stagedParcels = parcelRepository.findByImportRecordIdAndStatus(importId, ParcelStatus.STAGED);
        if (stagedParcels.isEmpty() && noStagedAssets(importId)) {
            if (importRecord.getApprovedAt() == null) {
                importRecord.setApprovedAt(LocalDateTime.now());
                importRecordRepository.save(importRecord);
            }
            return;
        }

        Farm farm = resolveApprovalFarm(username, farmId);
        LocalDateTime now = LocalDateTime.now();

        for (Parcel staging : stagedParcels) {
            String fallbackName = resolveParcelName(staging, "Imported Parcel " + staging.getId());
            promoteStaged(staging, farm, fallbackName, "#4CAF50", now);
        }

        promoteStagedAssets(importRecord, farm, now);

        if (importRecord.getApprovedAt() == null) {
            importRecord.setApprovedAt(now);
        }
        importRecordRepository.save(importRecord);
    }

    private boolean noStagedAssets(Long importId) {
        return toolRepository.findByImportRecordId(importId).isEmpty()
                && productRepository.findByImportRecordId(importId).isEmpty()
                && parcelOperationRepository.findByImportRecordId(importId).isEmpty();
    }

    private void promoteStagedAssets(ImportRecord importRecord, Farm farm, LocalDateTime now) {
        Map<String, Parcel> guidToLiveParcel = parcelRepository
                .findByImportRecordId(importRecord.getId()).stream()
                .filter(p -> p.getStatus() == ParcelStatus.LIVE && p.getSourceGuid() != null)
                .collect(Collectors.toMap(Parcel::getSourceGuid, p -> p, (a, b) -> a));

        for (Tool tool : toolRepository.findByImportRecordId(importRecord.getId())) {
            if (tool.getStatus() == ParcelStatus.LIVE) continue;
            if (tool.getSourceGuid() != null) {
                Optional<Tool> existing = toolRepository.findByFarmIdAndSourceGuid(farm.getId(), tool.getSourceGuid());
                if (existing.isPresent()) {
                    toolRepository.delete(tool);
                    continue;
                }
            }
            tool.setFarm(farm);
            tool.setStatus(ParcelStatus.LIVE);
            tool.setModifiedAt(now);
            toolRepository.save(tool);
        }

        for (Product product : productRepository.findByImportRecordId(importRecord.getId())) {
            if (product.getStatus() == ParcelStatus.LIVE) continue;
            if (product.getSourceGuid() != null) {
                Optional<Product> existing = productRepository
                        .findByFarmIdAndSourceGuid(farm.getId(), product.getSourceGuid());
                if (existing.isPresent()) {
                    Product realProduct = existing.get();
                    for (OperationProduct op : operationProductRepository.findByProductId(product.getId())) {
                        op.setProduct(realProduct);
                        operationProductRepository.save(op);
                    }
                    productRepository.delete(product);
                    continue;
                }
            }
            product.setFarm(farm);
            product.setStatus(ParcelStatus.LIVE);
            product.setModifiedAt(now);
            productRepository.save(product);
        }

        for (ParcelOperation op : parcelOperationRepository.findByImportRecordId(importRecord.getId())) {
            if (op.getStatus() == ParcelStatus.LIVE) continue;

            if (op.getSourceOperationName() != null) {
                final String opName = op.getSourceOperationName();
                OperationType opType = operationTypeRepository.findByNameAndFarmId(opName, farm.getId())
                        .orElseGet(() -> {
                            OperationType ot = new OperationType();
                            ot.setFarm(farm);
                            ot.setName(opName);
                            ot.setCreatedAt(now);
                            ot.setModifiedAt(now);
                            return operationTypeRepository.save(ot);
                        });
                op.setType(opType);
                op.setSourceOperationName(null);
            }

            if (op.getSourceParcelGuids() != null) {
                try {
                    List<String> guids = objectMapper.readValue(op.getSourceParcelGuids(),
                            new TypeReference<List<String>>() {});
                    for (String guid : guids) {
                        Parcel p = guidToLiveParcel.get(guid);
                        if (p != null) op.getParcels().add(p);
                    }
                    op.setSourceParcelGuids(null);
                } catch (Exception ignored) {}
            }

            if (op.getDate() != null && !op.getParcels().isEmpty()) {
                LocalDate d = op.getDate().toLocalDate();
                int campaignYear = d.getMonthValue() >= 10 ? d.getYear() + 1 : d.getYear();
                Period period = findOrCreatePeriod(farm, campaignYear);
                Parcel firstParcel = op.getParcels().iterator().next();
                ParcelPeriod pp = parcelPeriodRepository
                        .findByParcelIdAndPeriodId(firstParcel.getId(), period.getId())
                        .orElseGet(() -> {
                            ParcelPeriod newPp = new ParcelPeriod();
                            newPp.setParcel(firstParcel);
                            newPp.setPeriod(period);
                            newPp.setCreatedAt(now);
                            newPp.setActive(true);
                            newPp.setStartValidity(period.getStartDate() != null ? period.getStartDate() : now);
                            newPp.setEndValidity(period.getEndDate());
                            return parcelPeriodRepository.save(newPp);
                        });
                op.setParcelPeriod(pp);
            }

            op.setStatus(ParcelStatus.LIVE);
            op.setModifiedAt(now);
            parcelOperationRepository.save(op);
        }
    }


    @Transactional
    public void rejectStagedRows(Long importId, String username) {
        verifyOwnership(importId, username);
        LocalDateTime now = LocalDateTime.now();

        for (Parcel p : parcelRepository.findByImportRecordIdAndStatus(importId, ParcelStatus.STAGED)) {
            p.setStatus(ParcelStatus.REJECTED);
            p.setModifiedAt(now);
            parcelRepository.save(p);
        }
        for (Tool t : toolRepository.findByImportRecordId(importId)) {
            if (t.getStatus() == ParcelStatus.STAGED) {
                t.setStatus(ParcelStatus.REJECTED);
                t.setModifiedAt(now);
                toolRepository.save(t);
            }
        }
        for (Product p : productRepository.findByImportRecordId(importId)) {
            if (p.getStatus() == ParcelStatus.STAGED) {
                p.setStatus(ParcelStatus.REJECTED);
                p.setModifiedAt(now);
                productRepository.save(p);
            }
        }
        for (ParcelOperation op : parcelOperationRepository.findByImportRecordId(importId)) {
            if (op.getStatus() == ParcelStatus.STAGED) {
                op.setStatus(ParcelStatus.REJECTED);
                op.setModifiedAt(now);
                parcelOperationRepository.save(op);
            }
        }
    }


    @Transactional
    public void deleteImport(Long importId, String username) {
        verifyOwnership(importId, username);

        operationProductRepository.deleteByImportRecordId(importId);
        parcelOperationRepository.deleteByImportRecordId(importId);
        productRepository.deleteByImportRecordId(importId);
        toolRepository.deleteByImportRecordId(importId);

        List<Parcel> stagedParcels = parcelRepository.findByImportRecordIdAndStatus(importId, ParcelStatus.STAGED);
        for (Parcel p : stagedParcels) {
            if (p.getParentParcel() != null) p.setParentParcel(null);
        }
        parcelRepository.saveAll(stagedParcels);
        parcelRepository.flush();
        for (Parcel p : stagedParcels) {
            parcelPeriodRepository.deleteByParcelId(p.getId());
            parcelRepository.delete(p);
        }
        for (Parcel p : parcelRepository.findByImportRecordId(importId)) {
            p.setImportRecord(null);
            p.setSourceFile(null);
            parcelRepository.save(p);
        }

        importSourceFileRepository.deleteByImportRecordId(importId);
        importRecordRepository.deleteById(importId);
    }

    @Transactional
    public ImportRecordDto renameImport(Long importId, String username, String name) {
        ImportRecord importRecord = verifyOwnership(importId, username);
        String cleanedName = (name == null || name.isBlank()) ? importRecord.getFilename() : name.trim();
        importRecord.setName(cleanedName);
        importRecordRepository.save(importRecord);
        return toImportRecordDto(importRecord);
    }


    private Period resolvePeriodFor(ParcelPeriod pp, Farm farm) {
        if (pp.getForcedPeriod() != null) return pp.getForcedPeriod();
        if (pp.getPeriodNameOverride() != null) {
            final String name = pp.getPeriodNameOverride();
            return periodRepository.findByFarmIdAndName(farm.getId(), name).orElseGet(() -> {
                Period p = new Period();
                p.setFarm(farm);
                p.setName(name);
                p.setStartDate(pp.getPeriodStartOverride() != null ? pp.getPeriodStartOverride()
                        : LocalDate.now().atStartOfDay());
                p.setEndDate(pp.getPeriodEndOverride() != null ? pp.getPeriodEndOverride()
                        : LocalDate.now().atStartOfDay());
                p.setCreatedAt(LocalDateTime.now());
                p.setModifiedAt(LocalDateTime.now());
                return periodRepository.save(p);
            });
        }
        if (pp.getCampaignYear() != null) {
            return findOrCreatePeriod(farm, pp.getCampaignYear());
        }
        return pp.getPeriod();
    }

    private Period findOrCreatePeriod(Farm farm, int harvestYear) {
        String name = (harvestYear - 1) + "-" + harvestYear;
        return periodRepository.findByFarmIdAndName(farm.getId(), name).orElseGet(() -> {
            Period p = new Period();
            p.setFarm(farm);
            p.setName(name);
            p.setStartDate(LocalDate.of(harvestYear - 1, 9, 30).atStartOfDay());
            p.setEndDate(LocalDate.of(harvestYear, 9, 30).atStartOfDay());
            p.setCreatedAt(LocalDateTime.now());
            p.setModifiedAt(LocalDateTime.now());
            return periodRepository.save(p);
        });
    }


    private ImportRecord verifyOwnership(Long importId, String username) {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found"));
        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }
        return importRecord;
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
        if (farms.size() == 1) return farms.get(0);
        if (farms.isEmpty()) throw new RuntimeException("No farm available to attach the parcel");
        throw new RuntimeException("Farm id is required when multiple farms exist");
    }

    private String resolveParcelName(Parcel p, String fallback) {
        return (p.getSourceName() != null && !p.getSourceName().isBlank()) ? p.getSourceName() : fallback;
    }

    /** A staged parcel always has exactly one ParcelPeriod. */
    private ParcelPeriod primaryParcelPeriod(Parcel p) {
        return p.getParcelPeriods().stream().findFirst().orElse(null);
    }

    /** Default colour from the parcel's culture type. */
    private String resolveCultureColor(Parcel p) {
        if (p.getParcelPeriods() == null) return null;
        ParcelPeriod rep = p.getParcelPeriods().stream()
                .filter(pp -> pp.getCultureCode() != null && Boolean.TRUE.equals(pp.getActive()))
                .findFirst()
                .orElseGet(() -> p.getParcelPeriods().stream()
                        .filter(pp -> pp.getCultureCode() != null)
                        .findFirst().orElse(null));
        if (rep == null || rep.getCultureCode().getCode() == null) return null;
        String code = rep.getCultureCode().getCode();
        Long farmId = p.getFarm() != null ? p.getFarm().getId() : null;
        yt.wer.efms.model.CultureType ct = null;
        if (farmId != null) ct = cultureTypeRepository.findFirstByCodeAndFarmId(code, farmId).orElse(null);
        if (ct == null) ct = cultureTypeRepository.findByCodeAndFarmIsNull(code).orElse(null);
        return ct != null ? ct.getColor() : null;
    }

    private ParcelDto toParcelDto(Parcel p) {
        ParcelDto dto = new ParcelDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        dto.setColor(p.getColor());
        dto.setCultureColor(resolveCultureColor(p));
        if (p.getFarm() != null) dto.setFarmId(p.getFarm().getId());
        if (p.getImportRecord() != null) dto.setImportRecordId(p.getImportRecord().getId());
        if (p.getSourceFile() != null) dto.setSourceFileId(p.getSourceFile().getId());
        dto.setValidationNotes(p.getValidationNotes());

        if (p.getGeodata() != null) {
            try { dto.setGeodata(wktWriter.write(p.getGeodata())); }
            catch (Exception e) { dto.setGeodata(null); }
        }

        dto.setSourceName(p.getSourceName());
        dto.setSourceCode(p.getSourceCode());
        dto.setSourceBlockCode(p.getSourceBlockCode());
        dto.setExploitantCode(p.getExploitantCode());
        dto.setExploitantName(p.getExploitantName());
        dto.setMunicipality(p.getMunicipality());
        dto.setCadastralRef(p.getCadastralRef());
        dto.setSourceGuid(p.getSourceGuid());
        if (p.getParentParcel() != null) dto.setParentParcelId(p.getParentParcel().getId());

        List<ParcelPeriodSummaryDto> periodSummaries = p.getParcelPeriods().stream()
                .sorted(Comparator.comparing(pp -> {
                    Period pr = pp.getPeriod();
                    return pr != null && pr.getStartDate() != null ? pr.getStartDate() : LocalDateTime.MIN;
                }))
                .map(this::toParcelPeriodSummary)
                .collect(Collectors.toList());
        dto.setParcelPeriods(periodSummaries);

        return dto;
    }

    private ParcelPeriodSummaryDto toParcelPeriodSummary(ParcelPeriod pp) {
        ParcelPeriodSummaryDto dto = new ParcelPeriodSummaryDto();
        dto.setId(pp.getId());
        if (pp.getPeriod() != null) {
            dto.setPeriodId(pp.getPeriod().getId());
            dto.setPeriodName(pp.getPeriod().getName());
        } else if (pp.getCampaignYear() != null) {
            dto.setPeriodName((pp.getCampaignYear() - 1) + "-" + pp.getCampaignYear());
        }
        dto.setActive(pp.getActive());
        dto.setStartValidity(pp.getStartValidity());
        dto.setEndValidity(pp.getEndValidity());
        if (pp.getCultureCode() != null) {
            dto.setCultureCode(pp.getCultureCode().getCode());
            dto.setCultureLabel(pp.getCultureCode().getLabel());
        }
        if (pp.getCultureLabel() != null && dto.getCultureLabel() == null) {
            dto.setCultureLabel(pp.getCultureLabel());
        }
        dto.setVariety(pp.getVariety());
        dto.setDeclaredAreaHa(pp.getDeclaredAreaHa());
        dto.setMeasuredAreaHa(pp.getMeasuredAreaHa());
        dto.setTargetYieldTha(pp.getTargetYieldTha());
        dto.setSowingDensityKgha(pp.getSowingDensityKgha());
        dto.setRowSpacingCm(pp.getRowSpacingCm());
        dto.setSowingDate(pp.getSowingDate());
        dto.setHarvestDate(pp.getHarvestDate());
        dto.setYieldRealizedTha(pp.getYieldRealizedTha());
        dto.setCampaignYear(pp.getCampaignYear());
        dto.setEligibilityStatus(pp.getEligibilityStatus());
        dto.setComment(pp.getComment());
        if (pp.getForcedPeriod() != null) dto.setForcedPeriodId(pp.getForcedPeriod().getId());
        dto.setPeriodNameOverride(pp.getPeriodNameOverride());
        dto.setPeriodStartOverride(pp.getPeriodStartOverride());
        dto.setPeriodEndOverride(pp.getPeriodEndOverride());
        return dto;
    }

    private ImportRecordDto toImportRecordDto(ImportRecord record) {
        ImportRecordDto dto = new ImportRecordDto();
        dto.setId(record.getId());
        dto.setFilename(record.getFilename());
        dto.setName(record.getName() != null ? record.getName() : record.getFilename());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setApprovedAt(record.getApprovedAt());
        if (record.getUser() != null) dto.setUsername(record.getUser().getUsername());

        List<Parcel> parcels = parcelRepository.findByImportRecordId(record.getId());
        dto.setTotalParcels(parcels.size());
        dto.setStagedParcels((int) parcels.stream().filter(p -> p.getStatus() == ParcelStatus.STAGED).count());
        dto.setLiveParcels((int) parcels.stream().filter(p -> p.getStatus() == ParcelStatus.LIVE).count());
        dto.setRejectedParcels((int) parcels.stream().filter(p -> p.getStatus() == ParcelStatus.REJECTED).count());
        return dto;
    }
}

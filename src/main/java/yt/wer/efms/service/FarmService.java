package yt.wer.efms.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.dto.ParcelListDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.ImportedParcel;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.Period;
import yt.wer.efms.model.ParcelShare;
import yt.wer.efms.model.FarmUser;
import yt.wer.efms.model.FarmUserId;
import yt.wer.efms.model.Role;
import yt.wer.efms.model.ParcelShareRole;
import yt.wer.efms.model.ResearchZoneShare;
import yt.wer.efms.model.ResearchZoneShareClaim;
import yt.wer.efms.model.Tool;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.ImportedParcelRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.PeriodRepository;
import yt.wer.efms.repository.ParcelShareRepository;
import yt.wer.efms.repository.ResearchZoneShareRepository;
import yt.wer.efms.repository.ResearchZoneShareClaimRepository;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.repository.FarmUserRepository;
import yt.wer.efms.repository.ToolRepository;
import yt.wer.efms.repository.ProductRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmService {
    private final FarmRepository farmRepository;
    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;
    private final ImportedParcelRepository importedParcelRepository;
    private final PeriodRepository periodRepository;
    private final ParcelShareRepository parcelShareRepository;
    private final ResearchZoneShareRepository researchZoneShareRepository;
    private final ResearchZoneShareClaimRepository researchZoneShareClaimRepository;
    private final PermissionService permissionService;
    private final FarmUserRepository farmUserRepository;
    private final ToolRepository toolRepository;
    private final ProductRepository productRepository;
    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    public FarmService(FarmRepository farmRepository, ParcelRepository parcelRepository, 
                       UserRepository userRepository, ImportedParcelRepository importedParcelRepository,
                       PeriodRepository periodRepository,
                       ParcelShareRepository parcelShareRepository,
                       ResearchZoneShareRepository researchZoneShareRepository,
                       ResearchZoneShareClaimRepository researchZoneShareClaimRepository,
                       PermissionService permissionService,
                       FarmUserRepository farmUserRepository,
                       ToolRepository toolRepository,
                       ProductRepository productRepository) {
        this.farmRepository = farmRepository;
        this.parcelRepository = parcelRepository;
        this.userRepository = userRepository;
        this.importedParcelRepository = importedParcelRepository;
        this.periodRepository = periodRepository;
        this.parcelShareRepository = parcelShareRepository;
        this.researchZoneShareRepository = researchZoneShareRepository;
        this.researchZoneShareClaimRepository = researchZoneShareClaimRepository;
        this.permissionService = permissionService;
        this.farmUserRepository = farmUserRepository;
        this.toolRepository = toolRepository;
        this.productRepository = productRepository;
    }

    public List<FarmDto> listAll() {
        return farmRepository.findAll().stream()
                .map(this::toFarmDto)
                .collect(Collectors.toList());
    }

    public List<FarmDto> listUserFarms(String username) {
        List<Farm> owned = farmRepository.findByOwnerUsername(username);
        List<Farm> memberFarms = farmRepository.findAll().stream()
            .filter(farm -> permissionService.getFarmRole(farm.getId(), username).isPresent())
            .collect(Collectors.toList());
        List<Farm> sharedFarms = parcelShareRepository.findByUserUsername(username).stream()
            .map(ParcelShare::getParcel)
            .map(Parcel::getFarm)
            .filter(farm -> farm != null)
            .distinct()
            .collect(Collectors.toList());
        List<Farm> researchSharedFarms = researchZoneShareRepository.findByUserUsername(username).stream()
            .filter(this::isResearchShareActive)
            .map(ResearchZoneShare::getFarm)
            .filter(farm -> farm != null)
            .distinct()
            .collect(Collectors.toList());
        List<Farm> researchClaimedFarms = researchZoneShareClaimRepository.findClaimedFarmsByUsername(username).stream()
            .filter(farm -> farm != null)
            .distinct()
            .collect(Collectors.toList());
        return java.util.stream.Stream.of(owned, memberFarms, sharedFarms, researchSharedFarms, researchClaimedFarms)
            .flatMap(List::stream)
            .distinct()
            .map(farm -> {
                FarmDto dto = toFarmDto(farm);
                dto.setCanEdit(permissionService.canEditFarm(farm.getId(), username));
                dto.setCanManage(permissionService.canManageFarm(farm.getId(), username));
                return dto;
            })
            .collect(Collectors.toList());
    }

    public Optional<FarmDto> findById(Long id) {
        return farmRepository.findById(id).map(this::toFarmDto);
    }

    public FarmDto create(String name, String description, String location, Boolean isPublic, Boolean showName, Boolean showDescription, Boolean showLocation) {
        // Require authentication
        final String username;
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to create a farm");
        }

        if (username == null || username.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to create a farm");
        }

        Farm farm = new Farm();
        farm.setName(name);
        farm.setDescription(description);
        farm.setLocation(location);
        farm.setIsPublic(isPublic != null ? isPublic : false);
        farm.setShowName(showName != null ? showName : true);
        farm.setShowDescription(showDescription != null ? showDescription : true);
        farm.setShowLocation(showLocation != null ? showLocation : true);
        farm.setCreatedAt(LocalDateTime.now());
        farm.setModifiedAt(LocalDateTime.now());

        // Set owner
        userRepository.findAll().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .ifPresent(farm::setOwner);

        Farm saved = farmRepository.save(farm);
        return toFarmDto(saved);
    }

    public List<FarmDto> listPublic() {
        return farmRepository.findByIsPublicTrue().stream()
                .map(this::toFarmDto)
                .map(this::maskByVisibility)
                .collect(Collectors.toList());
    }

    public Optional<FarmDto> update(Long id, FarmDto input) {
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(id, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update farms you manage");
        }
        return farmRepository.findById(id).map(f -> {
            if (input.getName() != null) f.setName(input.getName());
            if (input.getDescription() != null) f.setDescription(input.getDescription());
            if (input.getLocation() != null) f.setLocation(input.getLocation());
            if (input.getIsPublic() != null) f.setIsPublic(input.getIsPublic());
            if (input.getShowName() != null) f.setShowName(input.getShowName());
            if (input.getShowDescription() != null) f.setShowDescription(input.getShowDescription());
            if (input.getShowLocation() != null) f.setShowLocation(input.getShowLocation());
            f.setModifiedAt(LocalDateTime.now());
            Farm s = farmRepository.save(f);
            return toFarmDto(s);
        });
    }

    public void delete(Long id) {
        String username = permissionService.currentUsername();
        Farm farm = farmRepository.findById(id).orElse(null);
        if (farm == null) return;
        if (permissionService.isOwner(farm, username)) {
            farmRepository.deleteById(id);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete farms you own");
    }

    public boolean deleteParcel(Long parcelId) {
        Optional<Parcel> parcelOpt = parcelRepository.findById(parcelId);
        if (parcelOpt.isEmpty()) {
            return false;
        }

        Parcel parcel = parcelOpt.get();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(parcel.getFarm().getId(), username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete parcels from farms you can edit");
        }

        ImportedParcel importedParcel = parcel.getCorrespondingPac();
        if (importedParcel != null) {
            importedParcel.setParcel(null);
            importedParcel.setModifiedAt(LocalDateTime.now());
            importedParcelRepository.save(importedParcel);
        }

        parcelRepository.delete(parcel);
        return true;
    }

    public List<ParcelDto> listParcels(Long farmId, String shareToken) {
        String username = permissionService.currentUsername();
        Set<Parcel> parcelSet = new HashSet<>();
        if (permissionService.canViewFarm(farmId, username)) {
            parcelSet.addAll(parcelRepository.findByFarmId(farmId));
        } else {
            parcelSet.addAll(parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toSet()));
            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            parcelSet.addAll(findParcelsFromResearchShares(farmId, activeShares, null, null, null, null, null, null, null, null, null, null));
            if (parcelSet.isEmpty() && !activeShares.isEmpty()) {
                parcelSet.addAll(findParcelsFromResearchSharesFallback(farmId, activeShares));
            }
        }
        return parcelSet.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelListDto> listParcelSummaries(Long farmId, String shareToken) {
        String username = permissionService.currentUsername();
        Set<Parcel> parcelSet = new HashSet<>();
        if (permissionService.canViewFarm(farmId, username)) {
            parcelSet.addAll(parcelRepository.findByFarmId(farmId));
        } else {
            parcelSet.addAll(parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toSet()));
            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            parcelSet.addAll(findParcelsFromResearchShares(farmId, activeShares, null, null, null, null, null, null, null, null, null, null));
            if (parcelSet.isEmpty() && !activeShares.isEmpty()) {
                parcelSet.addAll(findParcelsFromResearchSharesFallback(farmId, activeShares));
            }
        }
        return parcelSet.stream().map(this::toParcelListDto).collect(Collectors.toList());
    }

    public List<ParcelDto> listParcelsWithinBounds(Long farmId, Double minLat, Double minLng, Double maxLat, Double maxLng, String shareToken) {
        String username = permissionService.currentUsername();
        List<Parcel> parcels = parcelRepository.findByFarmIdWithinBounds(farmId, minLng, minLat, maxLng, maxLat);
        if (!permissionService.canViewFarm(farmId, username)) {
            Set<Long> allowedIds = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(share -> share.getParcel().getId())
                    .collect(java.util.stream.Collectors.toSet());
            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            Set<Long> zoneIds = findParcelsFromResearchShares(
                    farmId,
                    activeShares,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    minLat,
                    minLng,
                    maxLat,
                    maxLng
            ).stream().map(Parcel::getId).collect(Collectors.toSet());
            allowedIds.addAll(zoneIds);
            parcels = parcels.stream().filter(p -> allowedIds.contains(p.getId())).collect(Collectors.toList());
        }
        return parcels.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelDto> searchParcels(Long farmId,
                                         Set<Long> periodIds,
                                         Set<Long> toolIds,
                                         Set<Long> productIds,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         String polygonWkt,
                                         Double minLat,
                                         Double minLng,
                                         Double maxLat,
                                         Double maxLng,
                                         String shareToken) {
        String username = permissionService.currentUsername();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        boolean hasBounds = minLat != null && minLng != null && maxLat != null && maxLng != null;
        Double resolvedMinLat = hasBounds ? minLat : null;
        Double resolvedMinLng = hasBounds ? minLng : null;
        Double resolvedMaxLat = hasBounds ? maxLat : null;
        Double resolvedMaxLng = hasBounds ? maxLng : null;

        boolean periodFilter = periodIds != null && !periodIds.isEmpty();
        boolean toolFilter = toolIds != null && !toolIds.isEmpty();
        boolean productFilter = productIds != null && !productIds.isEmpty();

        List<Parcel> parcels = parcelRepository.searchParcels(
                farmId,
                periodFilter,
                toQueryFilterValues(periodIds),
                toolFilter,
                toQueryFilterValues(toolIds),
                productFilter,
                toQueryFilterValues(productIds),
                startDateTime,
                endDateTime,
                polygonWkt,
                resolvedMinLng,
                resolvedMinLat,
                resolvedMaxLng,
                resolvedMaxLat
        );

        if (!permissionService.canViewFarm(farmId, username)) {
            Set<Long> allowedIds = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                .map(share -> share.getParcel().getId())
                .collect(java.util.stream.Collectors.toSet());

            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            Set<Long> researchZoneIds = findParcelsFromResearchShares(
                    farmId,
                    activeShares,
                    periodIds,
                    toolIds,
                    productIds,
                    startDate,
                    endDate,
                    polygonWkt,
                    minLat,
                    minLng,
                    maxLat,
                    maxLng
            ).stream().map(Parcel::getId).collect(Collectors.toSet());

            allowedIds.addAll(researchZoneIds);
            parcels = parcels.stream().filter(p -> allowedIds.contains(p.getId())).collect(Collectors.toList());
        }

        return parcels.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public ParcelDto createParcel(Long farmId, CreateParcelRequest request) {
        // Verify farm exists and user has permission
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add parcels to farms you can edit");
        }

        Parcel parcel = new Parcel();
        parcel.setName(request.getName());
        parcel.setFarm(farm);
        parcel.setActive(request.getActive() != null ? request.getActive() : true);
        parcel.setStartValidity(request.getStartValidity() != null ? request.getStartValidity() : LocalDateTime.now());
        parcel.setEndValidity(request.getEndValidity());
        
        // Convert WKT string to Geometry
        if (request.getGeodata() != null && !request.getGeodata().isEmpty()) {
            try {
                Geometry geom = wktReader.read(request.getGeodata());
                parcel.setGeodata(geom);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid WKT geometry: " + e.getMessage(), e);
            }
        }
        
        parcel.setColor(request.getColor());
        parcel.setCreatedAt(LocalDateTime.now());
        parcel.setModifiedAt(LocalDateTime.now());

        // Link to corresponding imported parcel if specified
        if (request.getCorrespondingPacId() != null) {
            importedParcelRepository.findById(request.getCorrespondingPacId())
                    .ifPresent(parcel::setCorrespondingPac);
        }

        if (request.getPeriodId() != null) {
            Period period = periodRepository.findById(request.getPeriodId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Period not found"));
            if (period.getFarm() == null || !period.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period does not belong to this farm");
            }
            parcel.setPeriod(period);
        }

        if (request.getParentParcelId() != null) {
            Parcel parent = parcelRepository.findById(request.getParentParcelId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent parcel not found"));
            if (parent.getFarm() == null || !parent.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent parcel does not belong to this farm");
            }
            parcel.setParentParcel(parent);
        }

        Parcel saved = parcelRepository.save(parcel);
        return toParcelDto(saved);
    }

    public List<ParcelDto> listAllParcels() {
        return parcelRepository.findAll().stream()
                .map(this::toParcelDto)
                .collect(Collectors.toList());
    }

    public Optional<ParcelDto> findParcelById(Long parcelId) {
        String username = permissionService.currentUsername();
        return parcelRepository.findById(parcelId).map(parcel -> {
            if (!permissionService.canViewParcel(parcel, username)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view parcels you have access to");
            }
            return toParcelDto(parcel);
        });
    }

    public Optional<ParcelDto> updateParcel(Long farmId, Long parcelId, CreateParcelRequest request) {
        // Require authentication
        final String username;
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to update a parcel");
        }

        if (username == null || username.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to update a parcel");
        }

        // Verify farm exists
        farmRepository.findById(farmId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));

        // Find the parcel and verify it belongs to this farm
        return parcelRepository.findById(parcelId).map(parcel -> {
            if (!parcel.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
            }
            if (!permissionService.canEditParcel(parcel, username)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update parcels you can edit");
            }

            // Update fields
            if (request.getName() != null) {
                parcel.setName(request.getName());
            }
            if (request.getActive() != null) {
                parcel.setActive(request.getActive());
            }
            if (request.getStartValidity() != null) {
                parcel.setStartValidity(request.getStartValidity());
            }
            if (request.getEndValidity() != null) {
                parcel.setEndValidity(request.getEndValidity());
            }
            
            // Update geometry if provided
            if (request.getGeodata() != null && !request.getGeodata().isEmpty()) {
                try {
                    Geometry geom = wktReader.read(request.getGeodata());
                    parcel.setGeodata(geom);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid WKT geometry: " + e.getMessage(), e);
                }
            }
            
            if (request.getColor() != null) {
                parcel.setColor(request.getColor());
            }
            
            parcel.setModifiedAt(LocalDateTime.now());

            // Update corresponding imported parcel if specified
            if (request.getCorrespondingPacId() != null) {
                importedParcelRepository.findById(request.getCorrespondingPacId())
                        .ifPresent(parcel::setCorrespondingPac);
            }

            if (request.getPeriodId() != null) {
                Period period = periodRepository.findById(request.getPeriodId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Period not found"));
                if (period.getFarm() == null || !period.getFarm().getId().equals(farmId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period does not belong to this farm");
                }
                parcel.setPeriod(period);
            }

            if (request.getParentParcelId() != null) {
                Parcel parent = parcelRepository.findById(request.getParentParcelId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent parcel not found"));
                if (parent.getFarm() == null || !parent.getFarm().getId().equals(farmId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent parcel does not belong to this farm");
                }
                parcel.setParentParcel(parent);
            }

            Parcel saved = parcelRepository.save(parcel);
            return toParcelDto(saved);
        });
    }

    private ParcelDto toParcelDto(Parcel p) {
        String username = permissionService.currentUsername();
        ParcelDto dto = new ParcelDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setActive(p.getActive());
        dto.setStartValidity(p.getStartValidity());
        dto.setEndValidity(p.getEndValidity());
        
        // Convert Geometry to WKT string
        if (p.getGeodata() != null) {
            String wkt = wktWriter.write(p.getGeodata());
            dto.setGeodata(wkt);
        }
        
        dto.setColor(p.getColor());
        if (p.getFarm() != null) {
            dto.setFarmId(p.getFarm().getId());
        }
        if (p.getCorrespondingPac() != null) {
            dto.setCorrespondingPacId(p.getCorrespondingPac().getId());
        }
        if (p.getPeriod() != null) {
            dto.setPeriodId(p.getPeriod().getId());
        }
        if (p.getParentParcel() != null) {
            dto.setParentParcelId(p.getParentParcel().getId());
        }
        dto.setCanEdit(permissionService.canEditParcel(p, username));
        dto.setCanShare(permissionService.canShareParcel(p, username));
        return dto;
    }

    private ParcelListDto toParcelListDto(Parcel p) {
        String username = permissionService.currentUsername();
        ParcelListDto dto = new ParcelListDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setActive(p.getActive());
        dto.setColor(p.getColor());
        if (p.getFarm() != null) dto.setFarmId(p.getFarm().getId());
        if (p.getPeriod() != null) dto.setPeriodId(p.getPeriod().getId());
        dto.setCanEdit(permissionService.canEditParcel(p, username));
        dto.setCanShare(permissionService.canShareParcel(p, username));
        return dto;
    }

    public List<yt.wer.efms.dto.PeriodDto> listPeriods(Long farmId) {
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        return periodRepository.findByFarmId(farmId).stream()
                .map(this::toPeriodDto)
                .collect(Collectors.toList());
    }

    public yt.wer.efms.dto.PeriodDto createPeriod(Long farmId, yt.wer.efms.dto.CreatePeriodRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create periods for farms you manage");
        }

        Period period = new Period();
        period.setName(request.getName());
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());
        period.setFarm(farm);
        period.setCreatedAt(LocalDateTime.now());
        period.setModifiedAt(LocalDateTime.now());

        Period saved = periodRepository.save(period);
        return toPeriodDto(saved);
    }

    public Optional<yt.wer.efms.dto.PeriodDto> updatePeriod(Long farmId, Long periodId, yt.wer.efms.dto.CreatePeriodRequest request) {
        farmRepository.findById(farmId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update periods for farms you manage");
        }

        return periodRepository.findById(periodId).map(period -> {
            if (period.getFarm() == null || !period.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period does not belong to this farm");
            }
            if (request.getName() != null) period.setName(request.getName());
            if (request.getStartDate() != null) period.setStartDate(request.getStartDate());
            if (request.getEndDate() != null) period.setEndDate(request.getEndDate());
            period.setModifiedAt(LocalDateTime.now());
            return toPeriodDto(periodRepository.save(period));
        });
    }

    private yt.wer.efms.dto.PeriodDto toPeriodDto(Period period) {
        return new yt.wer.efms.dto.PeriodDto(
                period.getId(),
                period.getName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getFarm() != null ? period.getFarm().getId() : null
        );
    }

    private FarmDto toFarmDto(Farm f) {
        return new FarmDto(
                f.getId(),
                f.getName(),
                f.getDescription(),
                f.getLocation(),
                f.getIsPublic(),
                f.getShowName(),
                f.getShowDescription(),
                f.getShowLocation(),
                f.getCreatedAt(),
                f.getModifiedAt()
        );
    }

    private FarmDto maskByVisibility(FarmDto dto) {
        if (dto.getShowName() != null && !dto.getShowName()) dto.setName(null);
        if (dto.getShowDescription() != null && !dto.getShowDescription()) dto.setDescription(null);
        if (dto.getShowLocation() != null && !dto.getShowLocation()) dto.setLocation(null);
        return dto;
    }

    public List<yt.wer.efms.dto.FarmMemberDto> listMembers(Long farmId) {
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view members for farms you manage");
        }
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        List<yt.wer.efms.dto.FarmMemberDto> members = farmUserRepository.findByFarmId(farmId).stream()
                .map(fu -> new yt.wer.efms.dto.FarmMemberDto(
                        fu.getUser().getId(),
                        fu.getUser().getUsername(),
                        fu.getRole().name(),
                        false
                ))
                .collect(Collectors.toList());
        if (farm.getOwner() != null) {
            boolean ownerAlreadyListed = members.stream().anyMatch(m -> m.getUserId().equals(farm.getOwner().getId()));
            if (!ownerAlreadyListed) {
            members.add(new yt.wer.efms.dto.FarmMemberDto(
                farm.getOwner().getId(),
                farm.getOwner().getUsername(),
                "OWNER",
                true
            ));
            }
        }
        return members;
    }

    public yt.wer.efms.dto.FarmMemberDto addMember(Long farmId, String username, String role) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role resolvedRole = Role.valueOf(role.toUpperCase());
        FarmUserId id = new FarmUserId(farmId, user.getId());
        FarmUser farmUser = new FarmUser();
        farmUser.setId(id);
        farmUser.setFarm(farm);
        farmUser.setUser(user);
        farmUser.setRole(resolvedRole);
        farmUser.setCreatedAt(LocalDateTime.now());
        farmUser.setModifiedAt(LocalDateTime.now());
        farmUserRepository.save(farmUser);
        return new yt.wer.efms.dto.FarmMemberDto(user.getId(), user.getUsername(), resolvedRole.name(), false);
    }

    public Optional<yt.wer.efms.dto.FarmMemberDto> updateMember(Long farmId, Long userId, String role) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        FarmUserId id = new FarmUserId(farmId, userId);
        return farmUserRepository.findById(id).map(fu -> {
            Role resolvedRole = Role.valueOf(role.toUpperCase());
            fu.setRole(resolvedRole);
            fu.setModifiedAt(LocalDateTime.now());
            farmUserRepository.save(fu);
            return new yt.wer.efms.dto.FarmMemberDto(fu.getUser().getId(), fu.getUser().getUsername(), resolvedRole.name(), false);
        });
    }

    public void removeMember(Long farmId, Long userId) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        FarmUserId id = new FarmUserId(farmId, userId);
        farmUserRepository.deleteById(id);
    }

    public List<yt.wer.efms.dto.ResearchZoneShareDto> listResearchZoneShares(Long farmId) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view research shares for farms you manage");
        }
        return researchZoneShareRepository.findByFarmId(farmId).stream()
                .map(this::toResearchZoneShareDto)
                .collect(Collectors.toList());
    }

    public List<yt.wer.efms.dto.ResearchZoneShareDto> listEnrolledResearchZoneShares(Long farmId) {
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Set<ResearchZoneShare> enrolled = new LinkedHashSet<>();
        enrolled.addAll(researchZoneShareRepository.findByFarmIdAndUserUsername(farmId, current));
        enrolled.addAll(researchZoneShareClaimRepository.findSharesByFarmIdAndUsername(farmId, current));

        return enrolled.stream()
                .filter(this::isResearchShareActive)
                .map(this::toResearchZoneShareDto)
                .collect(Collectors.toList());
    }

    public yt.wer.efms.dto.ResearchZoneShareDto addResearchZoneShare(Long farmId, yt.wer.efms.dto.ResearchZoneShareRequest request) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create research shares for farms you manage");
        }

        if (request.getZoneWkt() == null || request.getZoneWkt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zoneWkt is required");
        }
        try {
            wktReader.read(request.getZoneWkt());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid zoneWkt", e);
        }

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        User creator = userRepository.findByUsername(current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        User recipient = null;
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            recipient = userRepository.findByUsername(request.getUsername().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found"));
        }

        Set<Long> periodIds = resolveRequestedIds(request.getPeriodId(), request.getPeriodIds());
        Set<Long> toolIds = resolveRequestedIds(request.getToolId(), request.getToolIds());
        Set<Long> productIds = resolveRequestedIds(request.getProductId(), request.getProductIds());

        for (Long periodId : periodIds) {
            Period period = periodRepository.findById(periodId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Period not found"));
            if (period.getFarm() == null || !period.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period does not belong to this farm");
            }
        }

        for (Long toolId : toolIds) {
            Tool tool = toolRepository.findById(toolId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));
            if (tool.getFarm() == null || !tool.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool does not belong to this farm");
            }
        }

        for (Long productId : productIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            if (product.getFarm() == null || !product.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not belong to this farm");
            }
        }

        if (request.getFilterStartDate() != null && request.getFilterEndDate() != null
                && request.getFilterStartDate().isAfter(request.getFilterEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "filterStartDate cannot be after filterEndDate");
        }

        if (request.getMaxUsers() != null && request.getMaxUsers() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxUsers must be greater than 0");
        }

        LocalDateTime shareStartAt = request.getShareStartAt() != null ? request.getShareStartAt() : LocalDateTime.now();
        if (request.getShareEndAt() != null && request.getShareEndAt().isBefore(shareStartAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shareEndAt cannot be before shareStartAt");
        }

        ResearchZoneShare share = new ResearchZoneShare();
        share.setFarm(farm);
        share.setCreatedBy(creator);
        share.setUser(recipient);
        share.setZoneWkt(request.getZoneWkt());
        share.setShareToken(generateShareToken());
        share.setPeriod(periodIds.size() == 1 ? periodRepository.findById(periodIds.iterator().next()).orElse(null) : null);
        share.setTool(toolIds.size() == 1 ? toolRepository.findById(toolIds.iterator().next()).orElse(null) : null);
        share.setProduct(productIds.size() == 1 ? productRepository.findById(productIds.iterator().next()).orElse(null) : null);
        share.setPeriodIds(toCsv(periodIds));
        share.setToolIds(toCsv(toolIds));
        share.setProductIds(toCsv(productIds));
        share.setFilterStartDate(request.getFilterStartDate());
        share.setFilterEndDate(request.getFilterEndDate());
        share.setShareStartAt(shareStartAt);
        share.setShareEndAt(request.getShareEndAt());
        share.setMaxUsers(request.getMaxUsers());
        share.setCreatedAt(LocalDateTime.now());
        share.setModifiedAt(LocalDateTime.now());

        ResearchZoneShare saved = researchZoneShareRepository.save(share);
        return toResearchZoneShareDto(saved);
    }

    public Optional<yt.wer.efms.dto.ResearchZoneShareDto> updateResearchZoneShare(Long farmId,
                                                                                     Long shareId,
                                                                                     yt.wer.efms.dto.ResearchZoneShareRequest request) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update research shares for farms you manage");
        }

        return researchZoneShareRepository.findById(shareId).map(share -> {
            if (share.getFarm() == null || !share.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Research share does not belong to this farm");
            }

            if (request.getZoneWkt() != null && !request.getZoneWkt().isBlank()) {
                try {
                    wktReader.read(request.getZoneWkt());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid zoneWkt", e);
                }
                share.setZoneWkt(request.getZoneWkt());
            }

            if (request.getUsername() != null) {
                if (request.getUsername().isBlank()) {
                    share.setUser(null);
                } else {
                    User recipient = userRepository.findByUsername(request.getUsername().trim())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found"));
                    share.setUser(recipient);
                }
            }

            Set<Long> periodIds = resolveRequestedIds(request.getPeriodId(), request.getPeriodIds());
            Set<Long> toolIds = resolveRequestedIds(request.getToolId(), request.getToolIds());
            Set<Long> productIds = resolveRequestedIds(request.getProductId(), request.getProductIds());

            for (Long periodId : periodIds) {
                Period period = periodRepository.findById(periodId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Period not found"));
                if (period.getFarm() == null || !period.getFarm().getId().equals(farmId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period does not belong to this farm");
                }
            }

            for (Long toolId : toolIds) {
                Tool tool = toolRepository.findById(toolId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));
                if (tool.getFarm() == null || !tool.getFarm().getId().equals(farmId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool does not belong to this farm");
                }
            }

            for (Long productId : productIds) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
                if (product.getFarm() == null || !product.getFarm().getId().equals(farmId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not belong to this farm");
                }
            }

            if (request.getFilterStartDate() != null && request.getFilterEndDate() != null
                    && request.getFilterStartDate().isAfter(request.getFilterEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "filterStartDate cannot be after filterEndDate");
            }

            if (request.getMaxUsers() != null && request.getMaxUsers() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxUsers must be greater than 0");
            }

            LocalDateTime shareStartAt = request.getShareStartAt();
            if (request.getShareEndAt() != null && shareStartAt != null && request.getShareEndAt().isBefore(shareStartAt)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shareEndAt cannot be before shareStartAt");
            }

            share.setPeriod(periodIds.size() == 1 ? periodRepository.findById(periodIds.iterator().next()).orElse(null) : null);
            share.setTool(toolIds.size() == 1 ? toolRepository.findById(toolIds.iterator().next()).orElse(null) : null);
            share.setProduct(productIds.size() == 1 ? productRepository.findById(productIds.iterator().next()).orElse(null) : null);
            share.setPeriodIds(toCsv(periodIds));
            share.setToolIds(toCsv(toolIds));
            share.setProductIds(toCsv(productIds));
            share.setFilterStartDate(request.getFilterStartDate());
            share.setFilterEndDate(request.getFilterEndDate());
            share.setShareStartAt(request.getShareStartAt());
            share.setShareEndAt(request.getShareEndAt());
            share.setMaxUsers(request.getMaxUsers());
            share.setModifiedAt(LocalDateTime.now());

            ResearchZoneShare saved = researchZoneShareRepository.save(share);
            return toResearchZoneShareDto(saved);
        });
    }

    public Optional<yt.wer.efms.dto.ResearchZoneShareDto> claimResearchZoneShare(Long farmId, String token) {
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        User currentUser = userRepository.findByUsername(current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        return researchZoneShareRepository.findByShareToken(token.trim()).map(share -> {
            if (share.getFarm() == null || !share.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Share does not belong to this farm");
            }
            if (!isResearchShareActive(share)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share is not active");
            }
            if (share.getUser() != null && !share.getUser().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share is assigned to another user");
            }
            enforceShareClaimLimit(share, currentUser);
            return toResearchZoneShareDto(share);
        });
    }

    public Optional<yt.wer.efms.dto.ResearchZoneShareDto> resolveResearchZoneShare(String token) {
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        User currentUser = userRepository.findByUsername(current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        return researchZoneShareRepository.findByShareToken(token.trim()).map(share -> {
            if (!isResearchShareActive(share)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share is not active");
            }
            if (share.getUser() != null && !share.getUser().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share is assigned to another user");
            }
            enforceShareClaimLimit(share, currentUser);
            return toResearchZoneShareDto(share);
        });
    }

    public void removeResearchZoneShare(Long farmId, Long shareId) {
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove research shares for farms you manage");
        }

        ResearchZoneShare share = researchZoneShareRepository.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Research share not found"));
        if (share.getFarm() == null || !share.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Research share does not belong to this farm");
        }
        researchZoneShareRepository.deleteById(shareId);
    }

    public Optional<yt.wer.efms.dto.ResearchZoneShareDto> leaveResearchZoneShare(Long farmId, Long shareId) {
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User currentUser = userRepository.findByUsername(current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        return researchZoneShareRepository.findById(shareId).map(share -> {
            if (share.getFarm() == null || !share.getFarm().getId().equals(farmId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Research share does not belong to this farm");
            }

            boolean changed = false;
            if (share.getUser() != null && share.getUser().getId().equals(currentUser.getId())) {
                share.setUser(null);
                changed = true;
            }

            Optional<ResearchZoneShareClaim> claim = researchZoneShareClaimRepository.findByShareIdAndUserId(share.getId(), currentUser.getId());
            if (claim.isPresent()) {
                researchZoneShareClaimRepository.delete(claim.get());
                changed = true;
            }

            if (!changed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this share");
            }

            share.setModifiedAt(LocalDateTime.now());
            ResearchZoneShare saved = researchZoneShareRepository.save(share);
            return toResearchZoneShareDto(saved);
        });
    }

    private yt.wer.efms.dto.ResearchZoneShareDto toResearchZoneShareDto(ResearchZoneShare share) {
        List<Long> periodIds = toSortedList(getShareFilterIds(share.getPeriodIds(), share.getPeriod() != null ? share.getPeriod().getId() : null));
        List<Long> toolIds = toSortedList(getShareFilterIds(share.getToolIds(), share.getTool() != null ? share.getTool().getId() : null));
        List<Long> productIds = toSortedList(getShareFilterIds(share.getProductIds(), share.getProduct() != null ? share.getProduct().getId() : null));
        LinkedHashSet<String> accessUsers = new LinkedHashSet<>();
        if (share.getUser() != null && share.getUser().getUsername() != null) {
            accessUsers.add(share.getUser().getUsername());
        }
        accessUsers.addAll(researchZoneShareClaimRepository.findClaimedUsernamesByShareId(share.getId()));

        return new yt.wer.efms.dto.ResearchZoneShareDto(
                share.getId(),
                share.getFarm() != null ? share.getFarm().getId() : null,
                share.getUser() != null ? share.getUser().getId() : null,
                share.getUser() != null ? share.getUser().getUsername() : null,
                share.getShareToken(),
                share.getZoneWkt(),
            periodIds.size() == 1 ? periodIds.get(0) : null,
            periodIds,
            toolIds.size() == 1 ? toolIds.get(0) : null,
            toolIds,
            productIds.size() == 1 ? productIds.get(0) : null,
            productIds,
                share.getFilterStartDate(),
                share.getFilterEndDate(),
                share.getShareStartAt(),
                share.getShareEndAt(),
                share.getMaxUsers(),
                researchZoneShareClaimRepository.countDistinctUsersByShareId(share.getId()),
                new ArrayList<>(accessUsers),
                share.getCreatedAt()
        );
    }

    private String generateShareToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        } while (researchZoneShareRepository.findByShareToken(token).isPresent());
        return token;
    }

    private boolean isResearchShareActive(ResearchZoneShare share) {
        LocalDateTime now = LocalDateTime.now();
        if (share.getShareStartAt() != null && now.isBefore(share.getShareStartAt())) return false;
        if (share.getShareEndAt() != null && now.isAfter(share.getShareEndAt())) return false;
        return true;
    }

    private List<ResearchZoneShare> resolveActiveResearchShares(Long farmId, String username, String shareToken) {
        List<ResearchZoneShare> candidates = new ArrayList<>();
        if (username != null && !username.equals("anonymousUser")) {
            candidates.addAll(researchZoneShareRepository.findByFarmIdAndUserUsername(farmId, username));
            candidates.addAll(researchZoneShareClaimRepository.findSharesByFarmIdAndUsername(farmId, username));
        }
        if (shareToken != null && !shareToken.isBlank()) {
            researchZoneShareRepository.findByShareToken(shareToken.trim()).ifPresent(candidates::add);
        }

        Set<Long> seen = new HashSet<>();
        return candidates.stream()
                .filter(share -> share.getFarm() != null && share.getFarm().getId().equals(farmId))
                .filter(this::isResearchShareActive)
                .filter(share -> seen.add(share.getId()))
                .collect(Collectors.toList());
    }

    private Set<Long> mergeLockedFilter(Set<Long> requestedValues, Set<Long> lockedValues) {
        if (lockedValues == null || lockedValues.isEmpty()) return requestedValues;
        if (requestedValues == null || requestedValues.isEmpty()) {
            return new LinkedHashSet<>(lockedValues);
        }

        Set<Long> intersection = requestedValues.stream()
                .filter(lockedValues::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (intersection.isEmpty()) {
            return null;
        }
        return intersection;
    }

    private List<Parcel> findParcelsFromResearchShares(Long farmId,
                                                       List<ResearchZoneShare> shares,
                                                       Set<Long> periodIds,
                                                       Set<Long> toolIds,
                                                       Set<Long> productIds,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       String polygonWkt,
                                                       Double minLat,
                                                       Double minLng,
                                                       Double maxLat,
                                                       Double maxLng) {
        if (shares == null || shares.isEmpty()) return List.of();

        Set<Long> parcelIds = new HashSet<>();
        List<Parcel> result = new ArrayList<>();

        for (ResearchZoneShare share : shares) {
            Set<Long> sharePeriodIds = getShareFilterIds(share.getPeriodIds(), share.getPeriod() != null ? share.getPeriod().getId() : null);
            Set<Long> shareToolIds = getShareFilterIds(share.getToolIds(), share.getTool() != null ? share.getTool().getId() : null);
            Set<Long> shareProductIds = getShareFilterIds(share.getProductIds(), share.getProduct() != null ? share.getProduct().getId() : null);

            Set<Long> effectivePeriodIds = mergeLockedFilter(periodIds, sharePeriodIds);
            Set<Long> effectiveToolIds = mergeLockedFilter(toolIds, shareToolIds);
            Set<Long> effectiveProductIds = mergeLockedFilter(productIds, shareProductIds);
            if (effectivePeriodIds == null) continue;
            if (effectiveToolIds == null) continue;
            if (effectiveProductIds == null) continue;

            LocalDate effectiveStartDate = startDate;
            LocalDate effectiveEndDate = endDate;
            if (share.getFilterStartDate() != null && (effectiveStartDate == null || share.getFilterStartDate().isAfter(effectiveStartDate))) {
                effectiveStartDate = share.getFilterStartDate();
            }
            if (share.getFilterEndDate() != null && (effectiveEndDate == null || share.getFilterEndDate().isBefore(effectiveEndDate))) {
                effectiveEndDate = share.getFilterEndDate();
            }
            if (effectiveStartDate != null && effectiveEndDate != null && effectiveStartDate.isAfter(effectiveEndDate)) {
                continue;
            }

            LocalDateTime startDateTime = effectiveStartDate != null ? effectiveStartDate.atStartOfDay() : null;
            LocalDateTime endDateTime = effectiveEndDate != null ? effectiveEndDate.atTime(LocalTime.MAX) : null;

                boolean periodFilter = effectivePeriodIds != null && !effectivePeriodIds.isEmpty();
                boolean toolFilter = effectiveToolIds != null && !effectiveToolIds.isEmpty();
                boolean productFilter = effectiveProductIds != null && !effectiveProductIds.isEmpty();

            List<Parcel> candidates = parcelRepository.searchParcels(
                    farmId,
                    periodFilter,
                    toQueryFilterValues(effectivePeriodIds),
                    toolFilter,
                    toQueryFilterValues(effectiveToolIds),
                    productFilter,
                    toQueryFilterValues(effectiveProductIds),
                    startDateTime,
                    endDateTime,
                    polygonWkt,
                    minLng,
                    minLat,
                    maxLng,
                    maxLat
            );

            boolean noOperationFilters = (effectiveToolIds == null || effectiveToolIds.isEmpty())
                    && (effectiveProductIds == null || effectiveProductIds.isEmpty())
                    && startDateTime == null
                    && endDateTime == null
                    && polygonWkt == null
                    && minLat == null
                    && minLng == null
                    && maxLat == null
                    && maxLng == null;

            if (candidates.isEmpty() && noOperationFilters) {
                candidates = parcelRepository.findByFarmId(farmId).stream()
                        .filter(parcel -> {
                            if (effectivePeriodIds == null || effectivePeriodIds.isEmpty()) {
                                return true;
                            }
                            if (parcel.getPeriod() == null || parcel.getPeriod().getId() == null) {
                                return false;
                            }
                            return effectivePeriodIds.contains(parcel.getPeriod().getId());
                        })
                        .collect(Collectors.toList());
            }

            Geometry shareZone;
            try {
                shareZone = wktReader.read(share.getZoneWkt());
            } catch (Exception e) {
                continue;
            }

            for (Parcel parcel : candidates) {
                if (parcel.getGeodata() == null) continue;
                if (!parcel.getGeodata().intersects(shareZone)) continue;
                if (parcelIds.add(parcel.getId())) {
                    result.add(parcel);
                }
            }
        }
        return result;
    }

    private List<Parcel> findParcelsFromResearchSharesFallback(Long farmId, List<ResearchZoneShare> shares) {
        if (shares == null || shares.isEmpty()) return List.of();

        List<Parcel> allParcels = parcelRepository.findByFarmId(farmId);
        Set<Long> parcelIds = new HashSet<>();
        List<Parcel> result = new ArrayList<>();

        for (ResearchZoneShare share : shares) {
            Set<Long> sharePeriodIds = getShareFilterIds(share.getPeriodIds(), share.getPeriod() != null ? share.getPeriod().getId() : null);

            Geometry shareZone;
            try {
                shareZone = wktReader.read(share.getZoneWkt());
            } catch (Exception e) {
                continue;
            }

            for (Parcel parcel : allParcels) {
                if (parcel.getGeodata() == null) continue;
                if (!sharePeriodIds.isEmpty()) {
                    if (parcel.getPeriod() == null || !sharePeriodIds.contains(parcel.getPeriod().getId())) {
                        continue;
                    }
                }
                if (!parcel.getGeodata().intersects(shareZone)) continue;
                if (parcelIds.add(parcel.getId())) {
                    result.add(parcel);
                }
            }
        }
        return result;
    }

    private List<Long> toQueryFilterValues(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of(-1L);
        }
        return new ArrayList<>(ids);
    }

    private Set<Long> resolveRequestedIds(Long singleValue, List<Long> listValues) {
        LinkedHashSet<Long> values = new LinkedHashSet<>();
        if (singleValue != null) {
            values.add(singleValue);
        }
        if (listValues != null) {
            for (Long value : listValues) {
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private Set<Long> getShareFilterIds(String csvValues, Long legacyValue) {
        LinkedHashSet<Long> values = new LinkedHashSet<>();
        if (csvValues != null && !csvValues.isBlank()) {
            for (String raw : csvValues.split(",")) {
                String token = raw.trim();
                if (token.isEmpty()) continue;
                try {
                    values.add(Long.parseLong(token));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed values to preserve backward compatibility.
                }
            }
        }
        if (values.isEmpty() && legacyValue != null) {
            values.add(legacyValue);
        }
        return values;
    }

    private String toCsv(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> toSortedList(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().sorted().collect(Collectors.toList());
    }

    private void enforceShareClaimLimit(ResearchZoneShare share, User currentUser) {
        if (share.getUser() != null) {
            return;
        }
        if (share.getMaxUsers() == null) {
            return;
        }

        if (researchZoneShareClaimRepository.existsByShareIdAndUserId(share.getId(), currentUser.getId())) {
            return;
        }

        long claimedUsers = researchZoneShareClaimRepository.countDistinctUsersByShareId(share.getId());
        if (claimedUsers >= share.getMaxUsers()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maximum number of users reached for this share");
        }

        ResearchZoneShareClaim claim = new ResearchZoneShareClaim();
        claim.setShare(share);
        claim.setUser(currentUser);
        claim.setCreatedAt(LocalDateTime.now());
        researchZoneShareClaimRepository.save(claim);
    }

    public List<yt.wer.efms.dto.ParcelShareDto> listParcelShares(Long farmId, Long parcelId) {
        String username = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view shares for parcels you manage");
        }
        return parcelShareRepository.findByParcelId(parcelId).stream()
                .map(share -> new yt.wer.efms.dto.ParcelShareDto(
                        share.getUser().getId(),
                        share.getUser().getUsername(),
                        share.getRole().name()
                ))
                .collect(Collectors.toList());
    }

    public yt.wer.efms.dto.ParcelShareDto addParcelShare(Long farmId, Long parcelId, String username, String role) {
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ParcelShareRole resolvedRole = ParcelShareRole.valueOf(role.toUpperCase());

        ParcelShare share = parcelShareRepository.findByParcelIdAndUserUsername(parcelId, username)
                .orElseGet(ParcelShare::new);
        share.setParcel(parcel);
        share.setUser(user);
        share.setRole(resolvedRole);
        if (share.getCreatedAt() == null) share.setCreatedAt(LocalDateTime.now());
        ParcelShare saved = parcelShareRepository.save(share);
        return new yt.wer.efms.dto.ParcelShareDto(saved.getUser().getId(), saved.getUser().getUsername(), saved.getRole().name());
    }

    public Optional<yt.wer.efms.dto.ParcelShareDto> updateParcelShare(Long farmId, Long parcelId, Long userId, String role) {
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ParcelShareRole resolvedRole = ParcelShareRole.valueOf(role.toUpperCase());
        return parcelShareRepository.findByParcelIdAndUserUsername(parcelId, user.getUsername()).map(share -> {
            share.setRole(resolvedRole);
            ParcelShare saved = parcelShareRepository.save(share);
            return new yt.wer.efms.dto.ParcelShareDto(saved.getUser().getId(), saved.getUser().getUsername(), saved.getRole().name());
        });
    }

    public void removeParcelShare(Long farmId, Long parcelId, Long userId) {
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        parcelShareRepository.deleteByParcelIdAndUserId(parcelId, userId);
    }
}

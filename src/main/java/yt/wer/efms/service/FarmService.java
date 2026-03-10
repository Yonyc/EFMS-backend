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
import yt.wer.efms.model.User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.ImportedParcelRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.PeriodRepository;
import yt.wer.efms.repository.ParcelShareRepository;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.repository.FarmUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private final PermissionService permissionService;
    private final FarmUserRepository farmUserRepository;
    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    public FarmService(FarmRepository farmRepository, ParcelRepository parcelRepository, 
                       UserRepository userRepository, ImportedParcelRepository importedParcelRepository,
                       PeriodRepository periodRepository,
                       ParcelShareRepository parcelShareRepository,
                       PermissionService permissionService,
                       FarmUserRepository farmUserRepository) {
        this.farmRepository = farmRepository;
        this.parcelRepository = parcelRepository;
        this.userRepository = userRepository;
        this.importedParcelRepository = importedParcelRepository;
        this.periodRepository = periodRepository;
        this.parcelShareRepository = parcelShareRepository;
        this.permissionService = permissionService;
        this.farmUserRepository = farmUserRepository;
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
        return java.util.stream.Stream.of(owned, memberFarms, sharedFarms)
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

    public List<ParcelDto> listParcels(Long farmId) {
        String username = permissionService.currentUsername();
        List<Parcel> parcels;
        if (permissionService.canViewFarm(farmId, username)) {
            parcels = parcelRepository.findByFarmId(farmId);
        } else {
            parcels = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toList());
        }
        return parcels.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelListDto> listParcelSummaries(Long farmId) {
        String username = permissionService.currentUsername();
        List<Parcel> parcels;
        if (permissionService.canViewFarm(farmId, username)) {
            parcels = parcelRepository.findByFarmId(farmId);
        } else {
            parcels = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toList());
        }
        return parcels.stream().map(this::toParcelListDto).collect(Collectors.toList());
    }

    public List<ParcelDto> listParcelsWithinBounds(Long farmId, Double minLat, Double minLng, Double maxLat, Double maxLng) {
        String username = permissionService.currentUsername();
        List<Parcel> parcels = parcelRepository.findByFarmIdWithinBounds(farmId, minLng, minLat, maxLng, maxLat);
        if (!permissionService.canViewFarm(farmId, username)) {
            java.util.Set<Long> allowedIds = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(share -> share.getParcel().getId())
                    .collect(java.util.stream.Collectors.toSet());
            parcels = parcels.stream().filter(p -> allowedIds.contains(p.getId())).collect(Collectors.toList());
        }
        return parcels.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelDto> searchParcels(Long farmId,
                                         Long periodId,
                                         Long toolId,
                                         Long productId,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         String polygonWkt,
                                         Double minLat,
                                         Double minLng,
                                         Double maxLat,
                                         Double maxLng) {
        String username = permissionService.currentUsername();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        boolean hasBounds = minLat != null && minLng != null && maxLat != null && maxLng != null;
        Double resolvedMinLat = hasBounds ? minLat : null;
        Double resolvedMinLng = hasBounds ? minLng : null;
        Double resolvedMaxLat = hasBounds ? maxLat : null;
        Double resolvedMaxLng = hasBounds ? maxLng : null;

        List<Parcel> parcels = parcelRepository.searchParcels(
                farmId,
            periodId,
                toolId,
                productId,
                startDateTime,
                endDateTime,
            polygonWkt,
                resolvedMinLng,
                resolvedMinLat,
                resolvedMaxLng,
                resolvedMaxLat
        );

        if (!permissionService.canViewFarm(farmId, username)) {
            java.util.Set<Long> allowedIds = parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                .map(share -> share.getParcel().getId())
                .collect(java.util.stream.Collectors.toSet());
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

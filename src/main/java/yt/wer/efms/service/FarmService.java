package yt.wer.efms.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.dto.ParcelListDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.ImportedParcel;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelOperation;
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
import yt.wer.efms.repository.ParcelOperationRepository;
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
    private final yt.wer.efms.repository.SystemSettingsRepository systemSettingsRepository;
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
    private final ParcelOperationRepository parcelOperationRepository;
    private final EmailService emailService;
    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    public FarmService(FarmRepository farmRepository, yt.wer.efms.repository.SystemSettingsRepository systemSettingsRepository, ParcelRepository parcelRepository, 
                       UserRepository userRepository, ImportedParcelRepository importedParcelRepository,
                       PeriodRepository periodRepository,
                       ParcelShareRepository parcelShareRepository,
                       ResearchZoneShareRepository researchZoneShareRepository,
                       ResearchZoneShareClaimRepository researchZoneShareClaimRepository,
                       PermissionService permissionService,
                       FarmUserRepository farmUserRepository,
                       ToolRepository toolRepository,
                       ProductRepository productRepository,
                       ParcelOperationRepository parcelOperationRepository,
                       EmailService emailService) {
        this.farmRepository = farmRepository;
        this.systemSettingsRepository = systemSettingsRepository;
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
        this.parcelOperationRepository = parcelOperationRepository;
        this.emailService = emailService;
    }

    private void sendFarmAlert(Farm farm, String actionType, String details) {
        String recipient = farm.getAlertRecipientEmail();
        if (recipient == null || recipient.trim().isEmpty()) {
            if (farm.getOwner() != null && farm.getOwner().getEmail() != null) {
                recipient = farm.getOwner().getEmail().trim();
            }
        }
        if (recipient == null || recipient.isEmpty()) {
            return; // No email configured
        }

        String subject = "EFMS Farm Security Alert: " + actionType + " on " + farm.getName();
        String htmlBody = "<div style=\"font-family: 'Outfit', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);\">"
                + "  <div style=\"background: linear-gradient(135deg, #4f46e5, #6366f1); padding: 24px; border-radius: 12px; color: #ffffff; text-align: center;\">"
                + "    <h2 style=\"margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.025em;\">EFMS SECURITY ALERT</h2>"
                + "    <p style=\"margin: 6px 0 0 0; font-size: 14px; opacity: 0.9; font-weight: 500;\">Farm: <strong>" + farm.getName() + "</strong></p>"
                + "  </div>"
                + "  <div style=\"padding: 24px; color: #334155;\">"
                + "    <p style=\"font-size: 16px; line-height: 1.6; font-weight: 600;\">Hello,</p>"
                + "    <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">A sensitive action (<strong>" + actionType + "</strong>) has been performed on your farm:</p>"
                + "    <div style=\"background-color: #f8fafc; border-left: 4px solid #6366f1; padding: 18px; margin: 24px 0; border-radius: 0 12px 12px 0; border-top: 1px solid #f1f5f9; border-right: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9;\">"
                + "      <p style=\"margin: 0 0 8px 0; font-size: 12px; color: #64748b; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase;\">Alert Details</p>"
                + "      <p style=\"margin: 0; font-size: 15px; line-height: 1.6; color: #1e293b; font-weight: 500;\">" + details + "</p>"
                + "    </div>"
                + "    <p style=\"font-size: 13px; color: #94a3b8; margin-top: 32px; border-top: 1px solid #f1f5f9; padding-top: 16px; line-height: 1.5;\">"
                + "      This is an automated message sent by the Exploitation Farm Management System (EFMS). You received this because email alerts are enabled for this farm."
                + "    </p>"
                + "  </div>"
                + "</div>";

        emailService.sendEmail(recipient, subject, htmlBody);
    }

    public List<FarmDto> listAll() {
        // Admin use: filter out deleted farms for normal listing
        return farmRepository.findAll().stream()
                .filter(f -> f.getDeletedAt() == null)
                .map(this::toFarmDto)
                .collect(Collectors.toList());
    }

    public List<FarmDto> listUserFarms(String username) {
        final Long actionUserId = userRepository.findByUsername(username).map(yt.wer.efms.model.User::getId).orElse(null);
        final boolean isAdmin = userRepository.findByUsername(username).map(yt.wer.efms.model.User::isAdmin).orElse(false);
        List<Farm> owned = farmRepository.findByOwnerUsernameAndDeletedAtIsNull(username);
        List<Farm> memberFarms = farmRepository.findAll().stream()
            .filter(farm -> farm.getDeletedAt() == null)
            .filter(farm -> permissionService.getFarmRole(farm.getId(), actionUserId).isPresent())
            .collect(Collectors.toList());
        List<Farm> sharedFarms = parcelShareRepository.findByUserUsername(username).stream()
            .map(ParcelShare::getParcel)
            .map(Parcel::getFarm)
            .filter(farm -> farm != null && farm.getDeletedAt() == null)
            .distinct()
            .collect(Collectors.toList());
        List<Farm> researchSharedFarms = researchZoneShareRepository.findByUserUsername(username).stream()
            .filter(this::isResearchShareActive)
            .map(ResearchZoneShare::getFarm)
            .filter(farm -> farm != null && farm.getDeletedAt() == null)
            .distinct()
            .collect(Collectors.toList());
        List<Farm> researchClaimedFarms = researchZoneShareClaimRepository.findClaimedFarmsByUsername(username).stream()
            .filter(farm -> farm != null && farm.getDeletedAt() == null)
            .distinct()
            .collect(Collectors.toList());
        return java.util.stream.Stream.of(owned, memberFarms, sharedFarms, researchSharedFarms, researchClaimedFarms)
            .flatMap(List::stream)
            .distinct()
            .map(this::toFarmDto)
            .collect(Collectors.toList());
    }

    public Optional<FarmDto> findById(Long id) {
        return farmRepository.findById(id)
                .filter(f -> f.getDeletedAt() == null)
                .map(this::toFarmDto);
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
        return farmRepository.findByIsPublicTrueAndDeletedAtIsNull().stream()
                .map(this::toFarmDto)
                .map(this::maskByVisibility)
                .collect(Collectors.toList());
    }

    public Page<FarmDto> listPublic(Pageable pageable) {
        return farmRepository.findByIsPublicTrueAndDeletedAtIsNull(pageable)
                .map(this::toFarmDto)
                .map(this::maskByVisibility);
    }

    public Optional<FarmDto> update(Long id, FarmDto input) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(id, actionUserId, isAdmin)) {
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
            if (input.getEnableMemberAlerts() != null) f.setEnableMemberAlerts(input.getEnableMemberAlerts());
            if (input.getEnableParcelAlerts() != null) f.setEnableParcelAlerts(input.getEnableParcelAlerts());
            if (input.getEnableOperationAlerts() != null) f.setEnableOperationAlerts(input.getEnableOperationAlerts());
            f.setAlertRecipientEmail(input.getAlertRecipientEmail());
            f.setModifiedAt(LocalDateTime.now());
            Farm s = farmRepository.save(f);
            return toFarmDto(s);
        });
    }

    public void delete(Long id) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        Farm farm = farmRepository.findById(id).orElse(null);
        if (farm == null || farm.getDeletedAt() != null) return;
        if (!permissionService.isOwner(farm, actionUserId) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete farms you own");
        }

        // Use the same timestamp for all cascade deletes so they can be identified later
        LocalDateTime deletedAt = LocalDateTime.now();
        User deletedByUser = userRepository.findById(actionUserId).orElse(null);

        // Cascade soft-delete: only parcels that are not already deleted
        List<Parcel> liveParcels = parcelRepository.findByFarmId(id).stream()
                .filter(p -> p.getDeletedAt() == null)
                .collect(Collectors.toList());
        for (Parcel parcel : liveParcels) {
            parcel.setDeletedAt(deletedAt);
            parcel.setDeletedBy(deletedByUser);
            parcelRepository.save(parcel);

            // Cascade soft-delete operations linked to this parcel (only live ones)
            Set<Long> savedOperationIds = new HashSet<>();
            for (ParcelOperation op : parcel.getParcelOperations()) {
                if (op.getDeletedAt() == null && savedOperationIds.add(op.getId())) {
                    op.setDeletedAt(deletedAt);
                    op.setDeletedBy(deletedByUser);
                    parcelOperationRepository.save(op);
                }
            }
        }

        // Soft-delete the farm itself
        farm.setDeletedAt(deletedAt);
        farm.setDeletedBy(deletedByUser);
        farmRepository.save(farm);
    }

    /**
     * Restore a soft-deleted farm and only those parcels/operations that were
     * cascade-deleted at the exact same timestamp (i.e. not independently deleted before).
     */
    public void restore(Long id) {
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can restore farms");
        }
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        if (farm.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm is not deleted");
        }

        LocalDateTime cascadeTimestamp = farm.getDeletedAt();

        // Restore parcels cascade-deleted at the same moment
        List<Parcel> cascadeParcels = parcelRepository.findByFarmIdAndDeletedAt(id, cascadeTimestamp);
        for (Parcel parcel : cascadeParcels) {
            parcel.setDeletedAt(null);
            parcel.setDeletedBy(null);
            parcelRepository.save(parcel);
        }

        // Restore operations cascade-deleted at the same moment
        List<ParcelOperation> cascadeOps = parcelOperationRepository.findByFarmIdAndDeletedAt(id, cascadeTimestamp);
        for (ParcelOperation op : cascadeOps) {
            op.setDeletedAt(null);
            op.setDeletedBy(null);
            parcelOperationRepository.save(op);
        }

        // Restore the farm
        farm.setDeletedAt(null);
        farm.setDeletedBy(null);
        farmRepository.save(farm);
    }

    public boolean deleteParcel(Long parcelId) {
        Optional<Parcel> parcelOpt = parcelRepository.findById(parcelId);
        if (parcelOpt.isEmpty() || parcelOpt.get().getDeletedAt() != null) {
            return false;
        }

        Parcel parcel = parcelOpt.get();
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        if (!permissionService.canEditFarm(parcel.getFarm().getId(), actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete parcels from farms you can edit");
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        User deletedByUser = userRepository.findById(actionUserId).orElse(null);

        ImportedParcel importedParcel = parcel.getCorrespondingPac();
        if (importedParcel != null) {
            importedParcel.setParcel(null);
            importedParcel.setModifiedAt(LocalDateTime.now());
            importedParcelRepository.save(importedParcel);
        }

        parcel.setDeletedAt(deletedAt);
        parcel.setDeletedBy(deletedByUser);
        parcelRepository.save(parcel);

        if (parcel.getFarm().isEnableParcelAlerts()) {
            String actor = permissionService.currentUsername();
            sendFarmAlert(parcel.getFarm(), "Parcel Deleted", "Parcel <strong>" + parcel.getName() + "</strong> (ID: " + parcel.getId() + ") has been <strong>deleted</strong> by user <strong>" + actor + "</strong>.");
        }
        return true;
    }

    public List<ParcelDto> listParcels(Long farmId, String shareToken) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        Set<Parcel> parcelSet = new HashSet<>();
        if (permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
            parcelSet.addAll(parcelRepository.findByFarmIdAndDeletedAtIsNull(farmId));
        } else {
            parcelSet.addAll(parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toSet()));
            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            List<Parcel> researchParcels = findParcelsFromResearchShares(farmId, activeShares, null, null, null, null, null, null, null, null, null, null);
            parcelSet.addAll(researchParcels);
        }
        return parcelSet.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public List<ParcelListDto> listParcelSummaries(Long farmId, String shareToken) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        Set<Parcel> parcelSet = new HashSet<>();
        if (permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
            parcelSet.addAll(parcelRepository.findByFarmIdAndDeletedAtIsNull(farmId));
        } else {
            parcelSet.addAll(parcelShareRepository.findByUserUsernameAndParcelFarmId(username, farmId).stream()
                    .map(ParcelShare::getParcel)
                    .collect(Collectors.toSet()));
            List<ResearchZoneShare> activeShares = resolveActiveResearchShares(farmId, username, shareToken);
            List<Parcel> researchParcels = findParcelsFromResearchShares(farmId, activeShares, null, null, null, null, null, null, null, null, null, null);
            parcelSet.addAll(researchParcels);
        }
        return parcelSet.stream().map(this::toParcelListDto).collect(Collectors.toList());
    }

    public List<ParcelDto> listParcelsWithinBounds(Long farmId, Double minLat, Double minLng, Double maxLat, Double maxLng, String shareToken) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        List<Parcel> parcels = parcelRepository.findByFarmIdWithinBounds(farmId, minLng, minLat, maxLng, maxLat);
        if (!permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
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

        if (!permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, actionUserId, isAdmin)) {
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
        // Audit: track who created / last modified this parcel
        userRepository.findById(permissionService.currentUserId()).ifPresent(u -> {
            parcel.setCreatedBy(u);
            parcel.setUpdatedBy(u);
        });

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
        if (farm.isEnableParcelAlerts()) {
            sendFarmAlert(farm, "Parcel Created", "A new parcel <strong>" + saved.getName() + "</strong> (ID: " + saved.getId() + ") has been <strong>created</strong> by user <strong>" + username + "</strong>.");
        }
        return toParcelDto(saved);
    }

    public List<ParcelDto> listAllParcels() {
        return parcelRepository.findAll().stream()
                .map(this::toParcelDto)
                .collect(Collectors.toList());
    }

    public Optional<ParcelDto> findParcelById(Long parcelId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        return parcelRepository.findById(parcelId).map(parcel -> {
            if (!permissionService.canViewParcel(parcel, actionUserId, isAdmin)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view parcels you have access to");
            }
            return toParcelDto(parcel);
        });
    }

    public Optional<ParcelDto> updateParcel(Long farmId, Long parcelId, CreateParcelRequest request) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
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
            if (!permissionService.canEditParcel(parcel, actionUserId, isAdmin)) {
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
            // Audit: update who last modified this parcel
            userRepository.findById(permissionService.currentUserId()).ifPresent(parcel::setUpdatedBy);

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
            if (saved.getFarm().isEnableParcelAlerts()) {
                sendFarmAlert(saved.getFarm(), "Parcel Updated", "Parcel <strong>" + saved.getName() + "</strong> (ID: " + saved.getId() + ") has been <strong>updated</strong> by user <strong>" + username + "</strong>.");
            }
            return toParcelDto(saved);
        });
    }

    private ParcelDto toParcelDto(Parcel p) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
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
        dto.setCanEdit(permissionService.canEditParcel(p, actionUserId, isAdmin));
        dto.setCanShare(permissionService.canShareParcel(p, actionUserId, isAdmin));
        return dto;
    }

    private ParcelListDto toParcelListDto(Parcel p) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        ParcelListDto dto = new ParcelListDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setActive(p.getActive());
        dto.setColor(p.getColor());
        if (p.getFarm() != null) dto.setFarmId(p.getFarm().getId());
        if (p.getPeriod() != null) dto.setPeriodId(p.getPeriod().getId());
        dto.setCanEdit(permissionService.canEditParcel(p, actionUserId, isAdmin));
        dto.setCanShare(permissionService.canShareParcel(p, actionUserId, isAdmin));
        return dto;
    }

    public List<yt.wer.efms.dto.PeriodDto> listPeriods(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        return periodRepository.findByFarmId(farmId).stream()
                .map(this::toPeriodDto)
                .collect(Collectors.toList());
    }

    public yt.wer.efms.dto.PeriodDto createPeriod(Long farmId, yt.wer.efms.dto.CreatePeriodRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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
        FarmDto dto = new FarmDto(
                f.getId(),
                f.getName(),
                f.getDescription(),
                f.getLocation(),
                f.getIsPublic(),
                f.getShowName(),
                f.getShowDescription(),
                f.getShowLocation(),
                f.getCreatedAt(),
                f.getModifiedAt(),
                f.isEnableMemberAlerts(),
                f.isEnableParcelAlerts(),
                f.isEnableOperationAlerts(),
                f.getAlertRecipientEmail()
        );
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        dto.setCanEdit(permissionService.canEditFarm(f.getId(), actionUserId, isAdmin));
        dto.setCanManage(permissionService.canManageFarm(f.getId(), actionUserId, isAdmin));
        return dto;
    }

    /** Public accessor for admin controller usage (e.g. listing deleted farms). */
    public FarmDto toFarmDtoPublic(Farm f) {
        return toFarmDto(f);
    }

    private FarmDto maskByVisibility(FarmDto dto) {
        if (dto.getShowName() != null && !dto.getShowName()) dto.setName(null);
        if (dto.getShowDescription() != null && !dto.getShowDescription()) dto.setDescription(null);
        if (dto.getShowLocation() != null && !dto.getShowLocation()) dto.setLocation(null);
        return dto;
    }

    public List<yt.wer.efms.dto.FarmMemberDto> listMembers(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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

    public yt.wer.efms.dto.FarmMemberDto addMember(Long farmId, Long newUserId, String username, String role) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        
        yt.wer.efms.model.User user;
        if (newUserId != null) {
            user = userRepository.findById(newUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } else {
            user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }

        Role resolvedRole = Role.valueOf(role.toUpperCase());

        if (farm.getOwner() != null && farm.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is the owner of the farm");
        }

        FarmUserId id = new FarmUserId(farmId, user.getId());
        if (farmUserRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member of this farm");
        }

        FarmUser farmUser = new FarmUser();
        farmUser.setId(id);
        farmUser.setFarm(farm);
        farmUser.setUser(user);
        farmUser.setRole(resolvedRole);
        farmUser.setCreatedAt(LocalDateTime.now());
        farmUser.setModifiedAt(LocalDateTime.now());
        farmUserRepository.save(farmUser);

        if (farm.isEnableMemberAlerts()) {
            sendFarmAlert(farm, "Member Added", "A new member <strong>" + user.getUsername() + "</strong> (ID: " + user.getId() + ") has been <strong>added</strong> to the farm with the role <strong>" + resolvedRole.name() + "</strong> by user <strong>" + current + "</strong>.");
        }

        return new yt.wer.efms.dto.FarmMemberDto(user.getId(), user.getUsername(), resolvedRole.name(), false);
    }

    public Optional<yt.wer.efms.dto.FarmMemberDto> updateMember(Long farmId, Long userId, String role) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        FarmUserId id = new FarmUserId(farmId, userId);
        return farmUserRepository.findById(id).map(fu -> {
            Role resolvedRole = Role.valueOf(role.toUpperCase());
            fu.setRole(resolvedRole);
            fu.setModifiedAt(LocalDateTime.now());
            farmUserRepository.save(fu);

            if (fu.getFarm().isEnableMemberAlerts()) {
                sendFarmAlert(fu.getFarm(), "Member Updated", "The role of member <strong>" + fu.getUser().getUsername() + "</strong> (ID: " + fu.getUser().getId() + ") was <strong>updated</strong> to <strong>" + resolvedRole.name() + "</strong> by user <strong>" + current + "</strong>.");
            }

            return new yt.wer.efms.dto.FarmMemberDto(fu.getUser().getId(), fu.getUser().getUsername(), resolvedRole.name(), false);
        });
    }

    public void removeMember(Long farmId, Long userId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage members for farms you manage");
        }
        FarmUserId id = new FarmUserId(farmId, userId);
        farmUserRepository.findById(id).ifPresent(fu -> {
            farmUserRepository.delete(fu);
            if (fu.getFarm().isEnableMemberAlerts()) {
                sendFarmAlert(fu.getFarm(), "Member Removed", "Member <strong>" + fu.getUser().getUsername() + "</strong> (ID: " + fu.getUser().getId() + ") was <strong>removed</strong> from the farm by user <strong>" + current + "</strong>.");
            }
        });
    }

    public List<yt.wer.efms.dto.ResearchZoneShareDto> listResearchZoneShares(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view research shares for farms you manage");
        }
        return researchZoneShareRepository.findByFarmId(farmId).stream()
                .map(this::toResearchZoneShareDto)
                .collect(Collectors.toList());
    }

    public List<yt.wer.efms.dto.ResearchZoneShareDto> listEnrolledResearchZoneShares(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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
        User creator = userRepository.findById(actionUserId)
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        User currentUser = userRepository.findById(actionUserId)
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        User currentUser = userRepository.findById(actionUserId)
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (!permissionService.canManageFarm(farmId, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        if (current == null || current.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User currentUser = userRepository.findById(actionUserId)
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
        boolean hasRequested = requestedValues != null && !requestedValues.isEmpty();
        boolean hasLocked = lockedValues != null && !lockedValues.isEmpty();

        if (!hasLocked && !hasRequested) return new HashSet<>();
        if (!hasLocked) return new LinkedHashSet<>(requestedValues);
        if (!hasRequested) return new LinkedHashSet<>(lockedValues);

        Set<Long> intersection = requestedValues.stream()
                .filter(lockedValues::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (intersection.isEmpty()) {
            return null; // Means no valid access
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, actionUserId, isAdmin)) {
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
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ParcelShareRole resolvedRole = ParcelShareRole.valueOf(role.toUpperCase());

        ParcelShare share = parcelShareRepository.findFirstByParcelIdAndUserUsername(parcelId, username)
                .orElseGet(ParcelShare::new);
        share.setParcel(parcel);
        share.setUser(user);
        share.setRole(resolvedRole);
        if (share.getCreatedAt() == null) share.setCreatedAt(LocalDateTime.now());
        ParcelShare saved = parcelShareRepository.save(share);
        return new yt.wer.efms.dto.ParcelShareDto(saved.getUser().getId(), saved.getUser().getUsername(), saved.getRole().name());
    }

    public Optional<yt.wer.efms.dto.ParcelShareDto> updateParcelShare(Long farmId, Long parcelId, Long userId, String role) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ParcelShareRole resolvedRole = ParcelShareRole.valueOf(role.toUpperCase());
        return parcelShareRepository.findFirstByParcelIdAndUserUsername(parcelId, user.getUsername()).map(share -> {
            share.setRole(resolvedRole);
            ParcelShare saved = parcelShareRepository.save(share);
            return new yt.wer.efms.dto.ParcelShareDto(saved.getUser().getId(), saved.getUser().getUsername(), saved.getRole().name());
        });
    }

    public void removeParcelShare(Long farmId, Long parcelId, Long userId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String current = permissionService.currentUsername();
        Parcel parcel = permissionService.requireParcel(parcelId);
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parcel does not belong to this farm");
        }
        if (!permissionService.canShareParcel(parcel, actionUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share parcels you manage");
        }
        parcelShareRepository.deleteByParcelIdAndUserId(parcelId, userId);
    }

    public List<yt.wer.efms.dto.ResearchZoneShareDto> getResearchSharesForParcel(Long farmId, Long parcelId, String shareToken) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        List<ResearchZoneShare> allShares = resolveActiveResearchShares(farmId, username, shareToken);
        if (allShares.isEmpty()) return List.of();

        Parcel parcel = parcelRepository.findById(parcelId).orElse(null);
        if (parcel == null || parcel.getGeodata() == null) return List.of();

        List<yt.wer.efms.dto.ResearchZoneShareDto> result = new ArrayList<>();
        for (ResearchZoneShare share : allShares) {
            Set<Long> sharePeriodIds = getShareFilterIds(share.getPeriodIds(), share.getPeriod() != null ? share.getPeriod().getId() : null);
            if (!sharePeriodIds.isEmpty() && (parcel.getPeriod() == null || !sharePeriodIds.contains(parcel.getPeriod().getId()))) {
                continue;
            }
            try {
                Geometry shareZone = wktReader.read(share.getZoneWkt());
                if (parcel.getGeodata().intersects(shareZone)) {
                    result.add(toResearchZoneShareDto(share));
                }
            } catch (Exception e) {
                // Ignore invalid geometry
            }
        }
        return result;
    }
}

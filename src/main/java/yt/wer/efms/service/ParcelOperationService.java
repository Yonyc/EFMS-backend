package yt.wer.efms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.CreateParcelOperationRequest;
import yt.wer.efms.dto.OperationProductDto;
import yt.wer.efms.dto.OperationProductInput;
import yt.wer.efms.dto.OperationTypeDto;
import yt.wer.efms.dto.ParcelOperationDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.OperationProduct;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelOperation;
import yt.wer.efms.model.ParcelPeriod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.Tool;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.AttachmentRepository;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.FarmOperationTypeDefaultToolRepository;
import yt.wer.efms.repository.OperationProductRepository;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.repository.ParcelOperationRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.ParcelShareRepository;
import yt.wer.efms.repository.ProductRepository;
import yt.wer.efms.repository.ToolRepository;
import yt.wer.efms.repository.UnitRepository;
import yt.wer.efms.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParcelOperationService {
    private final ParcelOperationRepository parcelOperationRepository;
    private final ParcelRepository parcelRepository;
    private final OperationTypeRepository operationTypeRepository;
    private final OperationProductRepository operationProductRepository;
    private final ProductRepository productRepository;
    private final ToolRepository toolRepository;
    private final UnitRepository unitRepository;
    private final FarmRepository farmRepository;
    private final PermissionService permissionService;
    private final ParcelShareRepository parcelShareRepository;
    private final FarmService farmService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FarmOperationTypeDefaultToolRepository farmOpTypeDefaultRepository;
    private final AttachmentRepository attachmentRepository;
    private final yt.wer.efms.repository.ParcelPeriodRepository parcelPeriodRepository;
    private final yt.wer.efms.repository.CultureCodeRepository cultureCodeRepository;

    public ParcelOperationService(ParcelOperationRepository parcelOperationRepository,
                                  ParcelRepository parcelRepository,
                                  OperationTypeRepository operationTypeRepository,
                                  OperationProductRepository operationProductRepository,
                                  ProductRepository productRepository,
                                  ToolRepository toolRepository,
                                  UnitRepository unitRepository,
                                  FarmRepository farmRepository,
                                  PermissionService permissionService,
                                  ParcelShareRepository parcelShareRepository,
                                  FarmService farmService,
                                  UserRepository userRepository,
                                  EmailService emailService,
                                  FarmOperationTypeDefaultToolRepository farmOpTypeDefaultRepository,
                                  AttachmentRepository attachmentRepository,
                                  yt.wer.efms.repository.ParcelPeriodRepository parcelPeriodRepository,
                                  yt.wer.efms.repository.CultureCodeRepository cultureCodeRepository) {
        this.cultureCodeRepository = cultureCodeRepository;
        this.parcelOperationRepository = parcelOperationRepository;
        this.parcelRepository = parcelRepository;
        this.operationTypeRepository = operationTypeRepository;
        this.operationProductRepository = operationProductRepository;
        this.productRepository = productRepository;
        this.toolRepository = toolRepository;
        this.unitRepository = unitRepository;
        this.farmRepository = farmRepository;
        this.permissionService = permissionService;
        this.parcelShareRepository = parcelShareRepository;
        this.farmService = farmService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.farmOpTypeDefaultRepository = farmOpTypeDefaultRepository;
        this.attachmentRepository = attachmentRepository;
        this.parcelPeriodRepository = parcelPeriodRepository;
    }

    private void sendOperationAlert(Farm farm, String actionType, String details) {
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

    public List<OperationTypeDto> listOperationTypes(Long farmId) {
        List<yt.wer.efms.model.OperationType> types = farmId != null
                ? operationTypeRepository.findByFarmIsNullOrFarmIdOrderByIdAsc(farmId)
                : operationTypeRepository.findByFarmIsNullOrderByIdAsc();

        java.util.Map<Long, yt.wer.efms.model.FarmOperationTypeDefaultTool> defaults =
                farmId != null
                        ? farmOpTypeDefaultRepository.findByFarmId(farmId).stream()
                                .collect(Collectors.toMap(
                                        d -> d.getOperationType().getId(), d -> d))
                        : java.util.Collections.emptyMap();

        return types.stream()
                .map(t -> {
                    yt.wer.efms.model.FarmOperationTypeDefaultTool def = defaults.get(t.getId());
                    return new OperationTypeDto(t.getId(), t.getName(),
                            t.getFarm() != null ? t.getFarm().getId() : null,
                            def != null ? def.getTool().getId() : null,
                            def != null ? def.getTool().getName() : null);
                })
                .collect(Collectors.toList());
    }

    public org.springframework.data.domain.Page<OperationTypeDto> listOperationTypes(Long farmId, org.springframework.data.domain.Pageable pageable) {
        if (farmId == null) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        java.util.Map<Long, yt.wer.efms.model.FarmOperationTypeDefaultTool> defaults =
                farmOpTypeDefaultRepository.findByFarmId(farmId).stream()
                        .collect(Collectors.toMap(d -> d.getOperationType().getId(), d -> d));

        return operationTypeRepository.findByFarmIsNullOrFarmIdOrderByIdAsc(farmId, pageable)
                .map(t -> {
                    yt.wer.efms.model.FarmOperationTypeDefaultTool def = defaults.get(t.getId());
                    return new OperationTypeDto(t.getId(), t.getName(),
                            t.getFarm() != null ? t.getFarm().getId() : null,
                            def != null ? def.getTool().getId() : null,
                            def != null ? def.getTool().getName() : null);
                });
    }

    /**
     * Lists a farm's operations, paged and newest first, with optional parcel/type/date filters.
     * Access is scoped to the parcels the caller can see: full-farm viewers get everything, while
     * shared users get only operations on their shared parcels.
     */
    public Page<ParcelOperationDto> listOperationsForFarm(Long farmId, Long parcelId, Long typeId,
                                                          LocalDateTime dateFrom, LocalDateTime dateTo,
                                                          Pageable pageable) {
        Set<Long> accessibleParcelIds = farmService.accessibleParcelIdsOrNull(farmId, null);
        if (accessibleParcelIds != null && accessibleParcelIds.isEmpty()) {
            return Page.empty(pageable);
        }

        org.springframework.data.jpa.domain.Specification<ParcelOperation> spec = (root, query, cb) -> {
            jakarta.persistence.criteria.Join<ParcelOperation, Parcel> parcelJoin =
                    root.join("parcels", jakarta.persistence.criteria.JoinType.INNER);

            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(parcelJoin.get("farm").get("id"), farmId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (accessibleParcelIds != null) {
                predicates.add(parcelJoin.get("id").in(accessibleParcelIds));
            }
            if (parcelId != null) predicates.add(cb.equal(parcelJoin.get("id"), parcelId));
            if (typeId  != null) predicates.add(cb.equal(root.get("type").get("id"), typeId));
            if (dateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dateFrom));
            if (dateTo  != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), dateTo));

            if (query != null) {
                query.distinct(true);
                query.orderBy(cb.desc(root.get("date")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return parcelOperationRepository.findAll(spec, pageable)
                .map(op -> {
                    ParcelOperationDto dto = toDto(op);
                    op.getParcels().stream()
                            .filter(p -> parcelId == null || p.getId().equals(parcelId))
                            .findFirst()
                            .ifPresent(p -> {
                                dto.setParcelId(p.getId());
                                dto.setParcelName(p.getName());
                            });
                    return dto;
                });
    }

    public List<ParcelOperationDto> listOperationsForParcel(Long farmId, Long parcelId, String shareToken) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }

        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        boolean hasAccess = permissionService.canViewParcel(parcel, actionUserId, isAdmin);

        List<yt.wer.efms.dto.ResearchZoneShareDto> activeShares = new java.util.ArrayList<>();
        if (!hasAccess) {
            activeShares = farmService.getResearchSharesForParcel(farmId, parcelId, shareToken);
            if (activeShares.isEmpty()) {
                throw new RuntimeException("Not allowed to view this parcel");
            }
        }

        List<ParcelOperationDto> ops = parcelOperationRepository.findDistinctByParcelsIdOrderByDateDesc(parcelId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        if (!hasAccess && !activeShares.isEmpty()) {
            final List<yt.wer.efms.dto.ResearchZoneShareDto> finalShares = activeShares;
            ops = ops.stream()
                    .filter(op -> finalShares.stream().anyMatch(share -> matchesShare(op, share)))
                    .collect(Collectors.toList());
        }

        return ops;
    }

    public Page<ParcelOperationDto> listOperationsForParcel(Long farmId, Long parcelId, String shareToken, Pageable pageable) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }

        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        boolean hasAccess = permissionService.canViewParcel(parcel, actionUserId, isAdmin);

        List<yt.wer.efms.dto.ResearchZoneShareDto> activeShares = new java.util.ArrayList<>();
        if (!hasAccess) {
            activeShares = farmService.getResearchSharesForParcel(farmId, parcelId, shareToken);
            if (activeShares.isEmpty()) {
                throw new RuntimeException("Not allowed to view this parcel");
            }
        }

        Page<ParcelOperation> entityPage = parcelOperationRepository.findDistinctByParcelsIdOrderByDateDesc(parcelId, pageable);

        if (!hasAccess && !activeShares.isEmpty()) {
            final List<yt.wer.efms.dto.ResearchZoneShareDto> finalShares = activeShares;
            java.util.List<ParcelOperationDto> content = entityPage.getContent().stream()
                    .map(this::toDto)
                    .filter(op -> finalShares.stream().anyMatch(share -> matchesShare(op, share)))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(content, pageable, entityPage.getTotalElements());
        }

        return entityPage.map(this::toDto);
    }

    private boolean matchesShare(ParcelOperationDto dto, yt.wer.efms.dto.ResearchZoneShareDto share) {
        if (share.getFilterStartDate() != null && dto.getDate().toLocalDate().isBefore(share.getFilterStartDate())) {
            return false;
        }
        if (share.getFilterEndDate() != null && dto.getDate().toLocalDate().isAfter(share.getFilterEndDate())) {
            return false;
        }
        if (share.getToolIds() != null && !share.getToolIds().isEmpty()) {
            boolean hasTool = dto.getProducts().stream()
                    .anyMatch(p -> p.getToolId() != null && share.getToolIds().contains(p.getToolId()));
            if (!hasTool) return false;
        }
        if (share.getOperationTypeIds() != null && !share.getOperationTypeIds().isEmpty()) {
            if (dto.getTypeId() == null || !share.getOperationTypeIds().contains(dto.getTypeId())) {
                return false;
            }
        }
        if (share.getProductIds() != null && !share.getProductIds().isEmpty()) {
            boolean hasProduct = dto.getProducts().stream()
                    .anyMatch(p -> p.getProductId() != null && share.getProductIds().contains(p.getProductId()));
            if (!hasProduct) return false;
        }
        return true;
    }

    public Optional<ParcelOperationDto> createOperation(Long farmId, Long parcelId, CreateParcelOperationRequest request) {
        Farm farm = requireFarmEdit(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditParcel(parcel, actionUserId, isAdmin)) {
            throw new RuntimeException("Not allowed to edit this parcel");
        }

        ParcelOperation operation = new ParcelOperation();
        operation.setDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        operation.setDurationSeconds(request.getDurationSeconds());
        operation.setCreatedAt(LocalDateTime.now());
        operation.setModifiedAt(LocalDateTime.now());
        // Audit
        userRepository.findById(actionUserId).ifPresent(u -> {
            operation.setCreatedBy(u);
            operation.setUpdatedBy(u);
        });

        if (request.getTypeId() != null) {
            OperationType type = operationTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new RuntimeException("Operation type not found"));
            operation.setType(type);
        }

        Set<Parcel> linkedParcels = new java.util.HashSet<>();
        linkedParcels.add(parcel);
        if (request.getParcelIds() != null) {
            for (Long extraId : request.getParcelIds()) {
                if (extraId.equals(parcelId)) continue;
                Parcel extra = parcelRepository.findById(extraId)
                        .orElseThrow(() -> new RuntimeException("Parcel not found: " + extraId));
                if (extra.getFarm() == null || !extra.getFarm().getId().equals(farmId)) {
                    throw new RuntimeException("Parcel does not belong to this farm: " + extraId);
                }
                linkedParcels.add(extra);
            }
        }
        operation.setParcels(linkedParcels);

        if (request.getParcelPeriodId() != null) {
            parcelPeriodRepository.findById(request.getParcelPeriodId())
                    .ifPresent(operation::setParcelPeriod);
        }
        if (operation.getParcelPeriod() == null) {
            ParcelPeriod resolved = resolveParcelPeriodForDate(parcel, operation.getDate());
            if (resolved != null) operation.setParcelPeriod(resolved);
        }

        requireParcelsActiveForPeriod(linkedParcels, operation.getParcelPeriod(), operation.getDate());

        ParcelOperation saved = parcelOperationRepository.save(operation);

        yt.wer.efms.model.CultureType seedCulture = null;
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            for (OperationProductInput input : request.getProducts()) {
                if (input.getProductId() == null) {
                    continue;
                }
                Product product = productRepository.findById(input.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                if (product.getFarm() != null && !product.getFarm().getId().equals(farm.getId())) {
                    throw new RuntimeException("Product does not belong to this farm");
                }
                if (seedCulture == null && product.getProductType() != null
                        && product.getProductType().isSeedType() && product.getCultureType() != null) {
                    seedCulture = product.getCultureType();
                }
                OperationProduct opProduct = new OperationProduct();
                opProduct.setOperation(saved);
                opProduct.setProduct(product);
                opProduct.setQuantity(input.getQuantity());
                opProduct.setCreatedAt(LocalDateTime.now());
                opProduct.setModifiedAt(LocalDateTime.now());

                if (input.getUnitId() != null) {
                    Unit unit = unitRepository.findById(input.getUnitId())
                            .orElseThrow(() -> new RuntimeException("Unit not found"));
                    opProduct.setUnit(unit);
                }

                if (input.getToolId() != null) {
                    Tool tool = toolRepository.findById(input.getToolId())
                            .orElseThrow(() -> new RuntimeException("Tool not found"));
                    if (tool.getFarm() != null && !tool.getFarm().getId().equals(farm.getId())) {
                        throw new RuntimeException("Tool does not belong to this farm");
                    }
                    opProduct.setTool(tool);
                }

                operationProductRepository.save(opProduct);
            }
        }

        applySeedCulture(seedCulture, linkedParcels, operation.getDate());

        if (farm.isEnableOperationAlerts()) {
            String opTypeName = saved.getType() != null ? saved.getType().getName() : "General Operation";
            int durationMinutes = saved.getDurationSeconds() != null ? (int)(saved.getDurationSeconds() / 60) : 0;
            sendOperationAlert(farm, "Operation Created", "A new parcel operation <strong>" + opTypeName + "</strong> was <strong>created</strong> on parcel <strong>" + parcel.getName() + "</strong> by user <strong>" + username + "</strong>. Date: <strong>" + saved.getDate() + "</strong>, Duration: <strong>" + durationMinutes + " min</strong>.");
        }

        return Optional.of(toDto(saved));
    }

    public Optional<ParcelOperationDto> updateOperation(Long farmId, Long parcelId, Long operationId, CreateParcelOperationRequest request) {
        Farm farm = requireFarmEdit(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditParcel(parcel, actionUserId, isAdmin)) {
            throw new RuntimeException("Not allowed to edit this parcel");
        }

        ParcelOperation operation = parcelOperationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Operation not found"));

        boolean linkedToParcel = operation.getParcels() != null && operation.getParcels().stream()
                .map(Parcel::getId)
                .anyMatch(id -> id.equals(parcelId));
        if (!linkedToParcel) {
            throw new RuntimeException("Operation not linked to this parcel");
        }

        if (request.getDate() != null) {
            operation.setDate(request.getDate());
        }
        if (request.getDurationSeconds() != null) {
            operation.setDurationSeconds(request.getDurationSeconds());
        }
        if (request.getTypeId() != null) {
            OperationType type = operationTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new RuntimeException("Operation type not found"));
            operation.setType(type);
        }
        if (request.getParcelIds() != null && !request.getParcelIds().isEmpty()) {
            Set<Parcel> linkedParcels = new java.util.HashSet<>();
            linkedParcels.add(parcel);
            for (Long extraId : request.getParcelIds()) {
                if (extraId.equals(parcelId)) continue;
                Parcel extra = parcelRepository.findById(extraId)
                        .orElseThrow(() -> new RuntimeException("Parcel not found: " + extraId));
                if (extra.getFarm() == null || !extra.getFarm().getId().equals(farmId)) {
                    throw new RuntimeException("Parcel does not belong to this farm: " + extraId);
                }
                linkedParcels.add(extra);
            }
            operation.setParcels(linkedParcels);
        }
        if (request.getParcelPeriodId() != null) {
            parcelPeriodRepository.findById(request.getParcelPeriodId())
                    .ifPresent(operation::setParcelPeriod);
        }
        if (operation.getParcelPeriod() == null
                || (request.getDate() != null && !periodContains(operation.getParcelPeriod(), operation.getDate()))) {
            ParcelPeriod resolved = resolveParcelPeriodForDate(parcel, operation.getDate());
            if (resolved != null) operation.setParcelPeriod(resolved);
        }

        operation.setModifiedAt(LocalDateTime.now());
        // Audit
        userRepository.findById(actionUserId).ifPresent(operation::setUpdatedBy);

        parcelOperationRepository.save(operation);

        operationProductRepository.deleteByOperationId(operation.getId());

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            for (OperationProductInput input : request.getProducts()) {
                if (input.getProductId() == null) {
                    continue;
                }
                Product product = productRepository.findById(input.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                if (product.getFarm() != null && !product.getFarm().getId().equals(farm.getId())) {
                    throw new RuntimeException("Product does not belong to this farm");
                }
                OperationProduct opProduct = new OperationProduct();
                opProduct.setOperation(operation);
                opProduct.setProduct(product);
                opProduct.setQuantity(input.getQuantity());
                opProduct.setCreatedAt(LocalDateTime.now());
                opProduct.setModifiedAt(LocalDateTime.now());

                if (input.getUnitId() != null) {
                    Unit unit = unitRepository.findById(input.getUnitId())
                            .orElseThrow(() -> new RuntimeException("Unit not found"));
                    opProduct.setUnit(unit);
                }

                if (input.getToolId() != null) {
                    Tool tool = toolRepository.findById(input.getToolId())
                            .orElseThrow(() -> new RuntimeException("Tool not found"));
                    if (tool.getFarm() != null && !tool.getFarm().getId().equals(farm.getId())) {
                        throw new RuntimeException("Tool does not belong to this farm");
                    }
                    opProduct.setTool(tool);
                }

                operationProductRepository.save(opProduct);
            }
        }

        if (farm.isEnableOperationAlerts()) {
            String opTypeName = operation.getType() != null ? operation.getType().getName() : "General Operation";
            int durationMinutes = operation.getDurationSeconds() != null ? (int)(operation.getDurationSeconds() / 60) : 0;
            sendOperationAlert(farm, "Operation Updated", "Parcel operation <strong>" + opTypeName + "</strong> (ID: " + operation.getId() + ") on parcel <strong>" + parcel.getName() + "</strong> was <strong>updated</strong> by user <strong>" + username + "</strong>.");
        }

        return Optional.of(toDto(operation));
    }

    /**
     * Soft-delete an operation. The operation row is kept in the DB with deletedAt set.
     */
    public boolean deleteOperation(Long farmId, Long parcelId, Long operationId) {
        requireFarmEdit(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        if (!permissionService.canEditParcel(parcel, actionUserId, isAdmin)) {
            throw new RuntimeException("Not allowed to edit this parcel");
        }
        ParcelOperation operation = parcelOperationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Operation not found"));
        if (operation.getDeletedAt() != null) return false; // already deleted

        operation.setDeletedAt(LocalDateTime.now());
        userRepository.findById(actionUserId).ifPresent(operation::setDeletedBy);
        parcelOperationRepository.save(operation);

        if (parcel.getFarm().isEnableOperationAlerts()) {
            String opTypeName = operation.getType() != null ? operation.getType().getName() : "General Operation";
            String actor = permissionService.currentUsername();
            sendOperationAlert(parcel.getFarm(), "Operation Deleted", "Parcel operation <strong>" + opTypeName + "</strong> (ID: " + operation.getId() + ") on parcel <strong>" + parcel.getName() + "</strong> was <strong>deleted</strong> by user <strong>" + actor + "</strong>.");
        }

        return true;
    }

    private ParcelOperationDto toDto(ParcelOperation op) {
        List<OperationProduct> operationProducts = operationProductRepository.findByOperationId(op.getId());
        List<OperationProductDto> productDtos = operationProducts.stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());

        List<yt.wer.efms.dto.AttachmentDto> attachmentDtos = attachmentRepository.findByOperationId(op.getId()).stream()
                .map(a -> new yt.wer.efms.dto.AttachmentDto(a.getId(), a.getOriginalFilename(),
                        a.getUrl(), a.getMimeType(), a.getFileSize(), a.getCreatedAt()))
                .collect(Collectors.toList());

        ParcelOperationDto dto = new ParcelOperationDto(
                op.getId(),
                op.getDate(),
                op.getDurationSeconds(),
                op.getType() != null ? op.getType().getId() : null,
                op.getType() != null ? op.getType().getName() : null,
                op.getCreatedAt(),
                op.getModifiedAt(),
                productDtos
        );
        dto.setAttachments(attachmentDtos);

        if (op.getParcels() != null && !op.getParcels().isEmpty()) {
            List<Parcel> sortedParcels = op.getParcels().stream()
                    .sorted(java.util.Comparator.comparing(Parcel::getId))
                    .collect(Collectors.toList());
            dto.setParcelIds(sortedParcels.stream().map(Parcel::getId).collect(Collectors.toList()));
            dto.setParcelNames(sortedParcels.stream().map(Parcel::getName).collect(Collectors.toList()));
            dto.setParcelId(sortedParcels.get(0).getId());
            dto.setParcelName(sortedParcels.get(0).getName());
        }

        if (op.getParcelPeriod() != null) {
            yt.wer.efms.model.ParcelPeriod pp = op.getParcelPeriod();
            if (pp.getPeriod() != null) {
                dto.setPeriodId(pp.getPeriod().getId());
                dto.setPeriodName(pp.getPeriod().getName());
            }
            if (pp.getCultureCode() != null) {
                dto.setCultureCode(pp.getCultureCode().getCode());
                dto.setCultureLabel(pp.getCultureCode().getLabel());
                dto.setCultureCodeId(pp.getCultureCode().getId());
            }
        }

        return dto;
    }

    private OperationProductDto toProductDto(OperationProduct opProduct) {
        OperationProductDto dto = new OperationProductDto(
                opProduct.getId(),
                opProduct.getQuantity(),
                opProduct.getProduct() != null ? opProduct.getProduct().getId() : null,
                opProduct.getProduct() != null ? opProduct.getProduct().getName() : null,
                opProduct.getUnit() != null ? opProduct.getUnit().getId() : null,
                opProduct.getUnit() != null ? opProduct.getUnit().getValue() : null,
                opProduct.getTool() != null ? opProduct.getTool().getId() : null,
                opProduct.getTool() != null ? opProduct.getTool().getName() : null,
                opProduct.getCreatedAt(),
                opProduct.getModifiedAt()
        );
        if (opProduct.getProduct() != null) {
            dto.setOfficialAuthNumber(opProduct.getProduct().getOfficialAuthNumber());
            dto.setOfficialDecisionCode(opProduct.getProduct().getOfficialDecisionCode());
            dto.setOfficialDateFrom(opProduct.getProduct().getOfficialDateFrom());
            dto.setOfficialDateTo(opProduct.getProduct().getOfficialDateTo());
            dto.setOfficialUserGroupCode(opProduct.getProduct().getOfficialUserGroupCode());
            dto.setOfficialFormulationTypeCode(opProduct.getProduct().getOfficialFormulationTypeCode());
            dto.setOfficialProductTypeCodes(opProduct.getProduct().getOfficialProductTypeCodes());
            dto.setOfficialVersionTag(opProduct.getProduct().getOfficialVersionTag());
        }
        return dto;
    }

    private Farm requireFarmView(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, actionUserId, isAdmin)) {
            throw new RuntimeException("You can only view operations for farms you can access");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    private Farm requireFarmEdit(Long farmId) {
        Long actionUserId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, actionUserId, isAdmin)) {
            throw new RuntimeException("You can only manage operations for farms you can edit");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    /**
     * Stamps a seed product's culture onto each operated parcel's period at the operation date,
     * but only where that period has no culture yet. Resolves or
     * creates the CultureCode from the seed's CultureType; no-op when there is no seed culture.
     */
    private void applySeedCulture(yt.wer.efms.model.CultureType seedCulture,
                                  java.util.Collection<Parcel> parcels, LocalDateTime date) {
        if (seedCulture == null || seedCulture.getCode() == null || parcels == null) return;
        yt.wer.efms.model.CultureCode cc = findOrCreateCultureCode(seedCulture.getCode(), seedCulture.getName());
        if (cc == null) return;
        for (Parcel parcel : parcels) {
            ParcelPeriod pp = resolveParcelPeriodForDate(parcel, date);
            if (pp != null && pp.getCultureCode() == null) {
                pp.setCultureCode(cc);
                parcelPeriodRepository.save(pp);
            }
        }
    }

    private yt.wer.efms.model.CultureCode findOrCreateCultureCode(String code, String label) {
        if (code == null || code.isBlank()) return null;
        return cultureCodeRepository.findByCode(code).orElseGet(() -> {
            yt.wer.efms.model.CultureCode cc = new yt.wer.efms.model.CultureCode();
            cc.setCode(code);
            cc.setLabel(label);
            return cultureCodeRepository.save(cc);
        });
    }

    /**
     * Enforce that every targeted parcel is active during the operation's period. A parcel passes
     * if it has an active ParcelPeriod for the resolved period. Throws with the offending parcel's name otherwise.
     */
    private void requireParcelsActiveForPeriod(java.util.Collection<Parcel> parcels,
                                               ParcelPeriod anchorPeriod, LocalDateTime date) {
        if (parcels == null || parcels.isEmpty()) return;
        Long targetPeriodId = (anchorPeriod != null && anchorPeriod.getPeriod() != null)
                ? anchorPeriod.getPeriod().getId() : null;
        for (Parcel p : parcels) {
            List<ParcelPeriod> periods = parcelPeriodRepository.findByParcelId(p.getId());
            boolean active = periods.stream().anyMatch(pp -> {
                if (!Boolean.TRUE.equals(pp.getActive())) return false;
                if (targetPeriodId != null) {
                    return pp.getPeriod() != null && targetPeriodId.equals(pp.getPeriod().getId());
                }
                return periodContains(pp, date);
            });
            if (!active) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Parcel \"" + (p.getName() != null ? p.getName() : p.getId())
                                + "\" is not active during the selected period");
            }
        }
    }

    /**
     * Returns the parcel's ParcelPeriod whose Period range contains {@code date}, or the parcel's
     * only ParcelPeriod when it has exactly one. Null if no match.
     */
    private ParcelPeriod resolveParcelPeriodForDate(Parcel parcel, LocalDateTime date) {
        if (parcel == null) return null;
        List<ParcelPeriod> periods = parcelPeriodRepository.findByParcelId(parcel.getId());
        if (periods.isEmpty()) return null;
        if (date == null) return periods.size() == 1 ? periods.get(0) : null;
        for (ParcelPeriod pp : periods) {
            if (periodContains(pp, date)) return pp;
        }
        return periods.size() == 1 ? periods.get(0) : null;
    }

    /** True if {@code date} falls within the Period date range. */
    private boolean periodContains(ParcelPeriod pp, LocalDateTime date) {
        if (pp == null || pp.getPeriod() == null || date == null) return false;
        LocalDateTime start = pp.getPeriod().getStartDate();
        LocalDateTime end = pp.getPeriod().getEndDate();
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }
}

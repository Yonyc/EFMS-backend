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
import yt.wer.efms.model.Product;
import yt.wer.efms.model.Tool;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.FarmRepository;
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
                                  EmailService emailService) {
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

    public List<OperationTypeDto> listOperationTypes() {
        return operationTypeRepository.findAll().stream()
                .map(t -> new OperationTypeDto(t.getId(), t.getName()))
                .collect(Collectors.toList());
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

        operation.setParcels(Set.of(parcel));

        ParcelOperation saved = parcelOperationRepository.save(operation);

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

        return new ParcelOperationDto(
                op.getId(),
                op.getDate(),
                op.getDurationSeconds(),
                op.getType() != null ? op.getType().getId() : null,
                op.getType() != null ? op.getType().getName() : null,
                op.getCreatedAt(),
                op.getModifiedAt(),
                productDtos
        );
    }

    private OperationProductDto toProductDto(OperationProduct opProduct) {
        return new OperationProductDto(
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
}

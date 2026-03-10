package yt.wer.efms.service;

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

    public ParcelOperationService(ParcelOperationRepository parcelOperationRepository,
                                  ParcelRepository parcelRepository,
                                  OperationTypeRepository operationTypeRepository,
                                  OperationProductRepository operationProductRepository,
                                  ProductRepository productRepository,
                                  ToolRepository toolRepository,
                                  UnitRepository unitRepository,
                                  FarmRepository farmRepository,
                                  PermissionService permissionService,
                                  ParcelShareRepository parcelShareRepository) {
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
    }

    public List<OperationTypeDto> listOperationTypes() {
        return operationTypeRepository.findAll().stream()
                .map(t -> new OperationTypeDto(t.getId(), t.getName()))
                .collect(Collectors.toList());
    }

    public List<ParcelOperationDto> listOperationsForParcel(Long farmId, Long parcelId) {
        requireFarmView(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        String username = permissionService.currentUsername();
        if (!permissionService.canViewParcel(parcel, username)) {
            throw new RuntimeException("Not allowed to view this parcel");
        }

        return parcelOperationRepository.findDistinctByParcelsIdOrderByDateDesc(parcelId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<ParcelOperationDto> createOperation(Long farmId, Long parcelId, CreateParcelOperationRequest request) {
        Farm farm = requireFarmEdit(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        String username = permissionService.currentUsername();
        if (!permissionService.canEditParcel(parcel, username)) {
            throw new RuntimeException("Not allowed to edit this parcel");
        }

        ParcelOperation operation = new ParcelOperation();
        operation.setDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        operation.setDurationSeconds(request.getDurationSeconds());
        operation.setCreatedAt(LocalDateTime.now());
        operation.setModifiedAt(LocalDateTime.now());

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

        return Optional.of(toDto(saved));
    }

    public Optional<ParcelOperationDto> updateOperation(Long farmId, Long parcelId, Long operationId, CreateParcelOperationRequest request) {
        Farm farm = requireFarmEdit(farmId);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Parcel not found"));
        if (parcel.getFarm() == null || !parcel.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Parcel does not belong to this farm");
        }
        String username = permissionService.currentUsername();
        if (!permissionService.canEditParcel(parcel, username)) {
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

        return Optional.of(toDto(operation));
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
        String username = permissionService.currentUsername();
        if (!permissionService.canViewFarm(farmId, username)) {
            throw new RuntimeException("You can only view operations for farms you can access");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    private Farm requireFarmEdit(Long farmId) {
        String username = permissionService.currentUsername();
        if (!permissionService.canEditFarm(farmId, username)) {
            throw new RuntimeException("You can only manage operations for farms you can edit");
        }
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }
}

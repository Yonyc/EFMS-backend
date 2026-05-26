package yt.wer.efms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.dto.AttachmentDto;
import yt.wer.efms.model.*;
import yt.wer.efms.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ParcelOperationRepository operationRepository;
    private final ProductRepository productRepository;
    private final ToolRepository toolRepository;
    private final PermissionService permissionService;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             ParcelOperationRepository operationRepository,
                             ProductRepository productRepository,
                             ToolRepository toolRepository,
                             PermissionService permissionService) {
        this.attachmentRepository = attachmentRepository;
        this.operationRepository = operationRepository;
        this.productRepository = productRepository;
        this.toolRepository = toolRepository;
        this.permissionService = permissionService;
    }

    public AttachmentDto uploadToOperation(Long farmId, Long operationId, MultipartFile file) {
        requireFarmEdit(farmId);
        ParcelOperation op = operationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Operation not found"));
        Attachment att = buildAttachment(file);
        att.setOperation(op);
        return toDto(attachmentRepository.save(att));
    }

    public AttachmentDto uploadToProduct(Long farmId, Long productId, MultipartFile file) {
        requireFarmEdit(farmId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getFarm() == null || !product.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Product does not belong to this farm");
        }
        Attachment att = buildAttachment(file);
        att.setProduct(product);
        return toDto(attachmentRepository.save(att));
    }

    public AttachmentDto uploadToTool(Long farmId, Long toolId, MultipartFile file) {
        requireFarmEdit(farmId);
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
        if (tool.getFarm() == null || !tool.getFarm().getId().equals(farmId)) {
            throw new RuntimeException("Tool does not belong to this farm");
        }
        Attachment att = buildAttachment(file);
        att.setTool(tool);
        return toDto(attachmentRepository.save(att));
    }

    public void delete(Long farmId, Long attachmentId) {
        requireFarmEdit(farmId);
        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        // Verify the attachment belongs to something in this farm
        boolean belongsToFarm = false;
        if (att.getOperation() != null) {
            belongsToFarm = att.getOperation().getParcels().stream()
                    .anyMatch(p -> p.getFarm() != null && p.getFarm().getId().equals(farmId));
        } else if (att.getProduct() != null && att.getProduct().getFarm() != null) {
            belongsToFarm = att.getProduct().getFarm().getId().equals(farmId);
        } else if (att.getTool() != null && att.getTool().getFarm() != null) {
            belongsToFarm = att.getTool().getFarm().getId().equals(farmId);
        }
        if (!belongsToFarm) {
            throw new RuntimeException("Not authorised to delete this attachment");
        }
        // Remove file from disk
        if (att.getUrl() != null) {
            try {
                String relativePath = att.getUrl().replaceFirst("^/", "");
                Path filePath = Paths.get(relativePath);
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {}
        }
        attachmentRepository.delete(att);
    }

    public List<AttachmentDto> listForOperation(Long operationId) {
        return attachmentRepository.findByOperationId(operationId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<AttachmentDto> listForProduct(Long productId) {
        return attachmentRepository.findByProductId(productId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<AttachmentDto> listForTool(Long toolId) {
        return attachmentRepository.findByToolId(toolId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public AttachmentDto toDto(Attachment a) {
        return new AttachmentDto(a.getId(), a.getOriginalFilename(), a.getUrl(),
                a.getMimeType(), a.getFileSize(), a.getCreatedAt());
    }

    private Attachment buildAttachment(MultipartFile file) {
        try {
            Path uploadDir = Paths.get("uploads", "attachments");
            Files.createDirectories(uploadDir);
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String storedName = UUID.randomUUID() + ext;
            Path dest = uploadDir.resolve(storedName);
            Files.write(dest, file.getBytes());

            Attachment att = new Attachment();
            att.setOriginalFilename(original != null ? original : storedName);
            att.setUrl("/uploads/attachments/" + storedName);
            att.setMimeType(file.getContentType());
            att.setFileSize(file.getSize());
            att.setCreatedAt(LocalDateTime.now());
            return att;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment", e);
        }
    }

    private void requireFarmEdit(Long farmId) {
        Long userId = permissionService.currentUserId();
        boolean isAdmin = permissionService.isCurrentUserAdmin();
        if (!permissionService.canEditFarm(farmId, userId, isAdmin)) {
            throw new RuntimeException("Not authorised to edit this farm");
        }
    }
}

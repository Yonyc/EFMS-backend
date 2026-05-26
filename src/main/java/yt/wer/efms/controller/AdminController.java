package yt.wer.efms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import yt.wer.efms.model.EmailTemplate;
import yt.wer.efms.model.SystemSettings;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.EmailTemplateRepository;
import yt.wer.efms.repository.SystemSettingsRepository;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.service.PermissionService;
import yt.wer.efms.model.Farm;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.service.EmailService;
import yt.wer.efms.service.FarmService;
import yt.wer.efms.service.OfficialProductService;
import org.springframework.beans.factory.annotation.Value;
import yt.wer.efms.dto.AdminProductInput;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.PhytoImportRequest;
import yt.wer.efms.dto.PhytoImportResult;
import yt.wer.efms.service.FarmAssetService;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final PermissionService permissionService;
    private final EmailService emailService;
    private final FarmService farmService;
    private final OfficialProductService officialProductService;
    private final FarmAssetService farmAssetService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AdminController(UserRepository userRepository, FarmRepository farmRepository,
            SystemSettingsRepository systemSettingsRepository,
            EmailTemplateRepository emailTemplateRepository,
            PermissionService permissionService,
            EmailService emailService,
            FarmService farmService,
            OfficialProductService officialProductService,
            FarmAssetService farmAssetService) {
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.permissionService = permissionService;
        this.emailService = emailService;
        this.farmService = farmService;
        this.officialProductService = officialProductService;
        this.farmAssetService = farmAssetService;
    }

    private void requireAdmin() {
        if (!permissionService.isCurrentUserAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    @GetMapping("/users")
    public Object getUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        requireAdmin();
        if (page != null) {
            int pageSize = size != null ? size : 10;
            return userRepository.findAll(org.springframework.data.domain.PageRequest.of(page, pageSize, org.springframework.data.domain.Sort.by("id").ascending()));
        }
        return userRepository.findAll(org.springframework.data.domain.Sort.by("id").ascending());
    }

    @PutMapping("/users/{id}/admin")
    public ResponseEntity<?> setAdminStatus(@PathVariable Long id, @RequestParam boolean isAdmin) {
        requireAdmin();
        return userRepository.findById(id).map(user -> {
            user.setAdmin(isAdmin);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/verify")
    public ResponseEntity<?> setVerifyStatus(@PathVariable Long id, @RequestParam boolean isVerified) {
        requireAdmin();
        return userRepository.findById(id).map(user -> {
            user.setVerified(isVerified);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/resend-verification")
    public ResponseEntity<?> resendVerification(@PathVariable Long id) {
        requireAdmin();
        return userRepository.findById(id).map(user -> {
            if (user.isVerified()) {
                return ResponseEntity.badRequest().body("user_already_verified");
            }
            if (user.getEmail() == null || !user.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body("user_no_email");
            }
            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            userRepository.save(user);

            String htmlBody = String.format(
                "<p>Please verify your email address by clicking the button below:</p>" +
                "<a href=\"%s/verify?token=%s\" style=\"display: inline-block; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #4f46e5; text-decoration: none; border-radius: 5px;\">Verify Email</a>",
                frontendUrl, token);
            emailService.sendTemplatedEmail(user.getEmail(), "VERIFICATION", Map.of("token", token), "Verify your email", htmlBody);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/farms")
    public Object getFarms(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        requireAdmin();
        if (page != null) {
            int pageSize = size != null ? size : 10;
            return farmRepository.findAll(org.springframework.data.domain.PageRequest.of(page, pageSize, org.springframework.data.domain.Sort.by("id").ascending()));
        }
        return farmRepository.findAll(org.springframework.data.domain.Sort.by("id").ascending());
    }

    @GetMapping("/farms/{id}")
    public ResponseEntity<?> getFarm(@PathVariable Long id) {
        requireAdmin();
        return farmRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/farms/{id}/approve")
    public ResponseEntity<?> setFarmApprovalStatus(@PathVariable Long id, @RequestParam boolean isApproved) {
        requireAdmin();
        return farmRepository.findById(id).map(farm -> {
            farm.setApproved(isApproved);
            farmRepository.save(farm);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updateData) {
        requireAdmin();
        return userRepository.findById(id).map(user -> {
            if (updateData.getUsername() != null) user.setUsername(updateData.getUsername());
            if (updateData.getEmail() != null) user.setEmail(updateData.getEmail());
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        requireAdmin();
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/farms/{id}")
    public ResponseEntity<?> updateFarm(@PathVariable Long id, @RequestBody FarmDto updateData) {
        requireAdmin();
        return farmService.update(id, updateData)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/farms/{id}")
    public ResponseEntity<?> deleteFarm(@PathVariable Long id) {
        requireAdmin();
        if (farmRepository.existsById(id)) {
            farmService.delete(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/farms/{id}/restore")
    public ResponseEntity<?> restoreFarm(@PathVariable Long id) {
        requireAdmin();
        try {
            farmService.restore(id);
            return ResponseEntity.ok().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/farms/deleted")
    public ResponseEntity<?> listDeletedFarms() {
        requireAdmin();
        List<yt.wer.efms.dto.FarmDto> deleted = farmRepository.findAll().stream()
                .filter(f -> f.getDeletedAt() != null)
                .map(f -> farmService.toFarmDtoPublic(f))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/settings")
    public SystemSettings getSettings() {
        requireAdmin();
        return systemSettingsRepository.findById(1L).orElse(new SystemSettings());
    }

    @PostMapping("/settings")
    public SystemSettings saveSettings(@RequestBody SystemSettings settings) {
        requireAdmin();
        SystemSettings existing = systemSettingsRepository.findById(1L).orElse(new SystemSettings());
        existing.setUserVerificationRequired(settings.isUserVerificationRequired());
        existing.setFarmApprovalRequired(settings.isFarmApprovalRequired());
        return systemSettingsRepository.save(existing);
    }

    @GetMapping("/email-templates")
    public List<EmailTemplate> getEmailTemplates() {
        requireAdmin();
        return emailTemplateRepository.findAll();
    }

    @PostMapping("/email-templates")
    public EmailTemplate saveEmailTemplate(@RequestBody EmailTemplate template) {
        requireAdmin();
        Optional<EmailTemplate> existing = emailTemplateRepository.findByIdentifier(template.getIdentifier());
        if (existing.isPresent()) {
            EmailTemplate e = existing.get();
            e.setSubject(template.getSubject());
            e.setBody(template.getBody());
            return emailTemplateRepository.save(e);
        }
        return emailTemplateRepository.save(template);
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<?> adminUpdateProduct(@PathVariable Long productId, @RequestBody AdminProductInput input) {
        requireAdmin();
        return farmAssetService.adminUpdateProduct(productId, input)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/phyto/import")
    public PhytoImportResult importPhyto(@RequestBody PhytoImportRequest request) {
        requireAdmin();
        return officialProductService.importFromFile(request.getFilePath(), request.getVersionTag());
    }
}

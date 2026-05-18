package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import yt.wer.efms.dto.AuthRequest;
import yt.wer.efms.dto.AuthResponse;
import yt.wer.efms.dto.RegisterRequest;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.security.JwtUtil;
import yt.wer.efms.model.TutorialState;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import yt.wer.efms.service.EmailService;
import yt.wer.efms.repository.SystemSettingsRepository;
import yt.wer.efms.model.SystemSettings;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SystemSettingsRepository systemSettingsRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService, SystemSettingsRepository systemSettingsRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.systemSettingsRepository = systemSettingsRepository;
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        SystemSettings settings = systemSettingsRepository.findById(1L).orElse(new SystemSettings());
        return ResponseEntity.ok(Map.of("verificationRequired", settings.isUserVerificationRequired()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("username_taken");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setEmail(req.getEmail());
        u.setCreatedAt(LocalDateTime.now());
        u.setModifiedAt(LocalDateTime.now());
        u.setTutorialState(TutorialState.NOT_STARTED);

        // First user becomes admin
        if (userRepository.count() == 0) {
            u.setAdmin(true);
        }

        SystemSettings settings = systemSettingsRepository.findById(1L).orElse(new SystemSettings());
        if (settings.isUserVerificationRequired()) {
            if (req.getEmail() == null || !req.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body("email_required");
            }
            u.setVerified(false);
            String token = UUID.randomUUID().toString();
            u.setVerificationToken(token);
            userRepository.save(u);

            String htmlBody = String.format(
                "<p>Please verify your email address by clicking the button below:</p>" +
                "<a href=\"%s/verify?token=%s\" style=\"display: inline-block; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #4f46e5; text-decoration: none; border-radius: 5px;\">Verify Email</a>",
                frontendUrl, token);
            emailService.sendTemplatedEmail(req.getEmail(), "VERIFICATION", Map.of("token", token), "Verify your email", htmlBody);
            return ResponseEntity.ok(Map.of("message", "verification_required"));
        } else {
            u.setVerified(true);
            userRepository.save(u);
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null) return ResponseEntity.badRequest().build();
        
        Optional<User> userOpt = userRepository.findAll().stream().filter(u -> token.equals(u.getVerificationToken())).findFirst();
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setVerified(true);
            u.setVerificationToken(null);
            userRepository.save(u);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(400).body("invalid_token");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("invalid_credentials");

        User u = userOpt.get();
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) return ResponseEntity.status(401).body("invalid_credentials");
        
        SystemSettings settings = systemSettingsRepository.findById(1L).orElse(new SystemSettings());
        if (settings.isUserVerificationRequired() && !u.isVerified()) {
            return ResponseEntity.status(403).body("not_verified");
        }

        String token = jwtUtil.generateToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getTutorialState(), u.isOperationsPopupTopRight(), u.getEmail(), u.getAvatarUrl(), u.isAdmin()));
    }
}

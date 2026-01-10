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

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("username_taken");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setCreatedAt(LocalDateTime.now());
        u.setModifiedAt(LocalDateTime.now());
        u.setTutorialState(TutorialState.NOT_STARTED);
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("invalid_credentials");

        User u = userOpt.get();
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) return ResponseEntity.status(401).body("invalid_credentials");
        String token = jwtUtil.generateToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getTutorialState(), u.isOperationsPopupTopRight(), u.getEmail(), u.getAvatarUrl()));
    }
}

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

import java.time.LocalDateTime;

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
        if (userRepository.findAll().stream().anyMatch(u -> req.getUsername().equals(u.getUsername()))) {
            return ResponseEntity.badRequest().body("username_taken");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setCreatedAt(LocalDateTime.now());
        u.setModifiedAt(LocalDateTime.now());
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        User u = userRepository.findAll().stream().filter(x -> req.getUsername().equals(x.getUsername())).findFirst().orElse(null);
        if (u == null) return ResponseEntity.status(401).body("invalid_credentials");
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) return ResponseEntity.status(401).body("invalid_credentials");
        String token = jwtUtil.generateToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, u.getId()));
    }
}

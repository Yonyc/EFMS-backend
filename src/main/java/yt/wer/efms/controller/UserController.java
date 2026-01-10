package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yt.wer.efms.dto.TutorialStateUpdateRequest;
import yt.wer.efms.dto.UpdateUserRequest;
import yt.wer.efms.dto.UserProfileResponse;
import yt.wer.efms.dto.UserPreferencesRequest;
import yt.wer.efms.dto.UserPreferencesResponse;
import yt.wer.efms.model.TutorialState;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        User user = userOpt.get();
        return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getTutorialState(), user.isOperationsPopupTopRight(), user.getAvatarUrl()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateUserRequest request, Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        if (request == null) {
            return ResponseEntity.badRequest().body("missing_body");
        }

        User user = userOpt.get();

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getOperationsPopupTopRight() != null) {
            user.setOperationsPopupTopRight(request.getOperationsPopupTopRight());
        }

        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getTutorialState(), user.isOperationsPopupTopRight(), user.getAvatarUrl()));
    }

    @PutMapping("/me/tutorial-state")
    public ResponseEntity<?> updateTutorialState(@RequestBody TutorialStateUpdateRequest request, Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        TutorialState nextState;
        try {
            nextState = TutorialState.valueOf(request.getTutorialState());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("invalid_state");
        }

        User user = userOpt.get();
        user.setTutorialState(nextState);
        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getTutorialState(), user.isOperationsPopupTopRight(), user.getAvatarUrl()));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<?> getPreferences(Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        User user = userOpt.get();
        return ResponseEntity.ok(new UserPreferencesResponse(user.isOperationsPopupTopRight()));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestPart("file") MultipartFile file, Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("missing_file");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().body("invalid_file_type");
        }

        try {
            Path uploadDir = Paths.get("uploads", "avatars");
            Files.createDirectories(uploadDir);
            String extension = contentType.equals("image/png") ? ".png" : ".jpg";
            String filename = userOpt.get().getId() + extension;
            Path target = uploadDir.resolve(filename);
            Files.write(target, file.getBytes());

            String url = "/uploads/avatars/" + filename;
            User user = userOpt.get();
            user.setAvatarUrl(url);
            user.setModifiedAt(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getTutorialState(), user.isOperationsPopupTopRight(), user.getAvatarUrl()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("avatar_upload_failed");
        }
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody UserPreferencesRequest request, Authentication authentication) {
        Optional<User> userOpt = resolveUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        if (request == null || request.getOperationsPopupTopRight() == null) {
            return ResponseEntity.badRequest().body("missing_preference");
        }

        User user = userOpt.get();
        user.setOperationsPopupTopRight(request.getOperationsPopupTopRight());
        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new UserPreferencesResponse(user.isOperationsPopupTopRight()));
    }

    private Optional<User> resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }
}

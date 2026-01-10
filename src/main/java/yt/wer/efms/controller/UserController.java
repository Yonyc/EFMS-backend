package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yt.wer.efms.dto.TutorialStateUpdateRequest;
import yt.wer.efms.dto.UserProfileResponse;
import yt.wer.efms.dto.UserPreferencesRequest;
import yt.wer.efms.dto.UserPreferencesResponse;
import yt.wer.efms.model.TutorialState;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.UserRepository;

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
        return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getTutorialState(), user.isOperationsPopupTopRight()));
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
        return ResponseEntity.ok(new UserProfileResponse(user.getId(), user.getUsername(), user.getTutorialState(), user.isOperationsPopupTopRight()));
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

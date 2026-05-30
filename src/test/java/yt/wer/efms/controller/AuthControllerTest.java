package yt.wer.efms.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import yt.wer.efms.dto.AuthRequest;
import yt.wer.efms.dto.AuthResponse;
import yt.wer.efms.dto.RegisterRequest;
import yt.wer.efms.model.SystemSettings;
import yt.wer.efms.model.TutorialState;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.SystemSettingsRepository;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.security.JwtUtil;
import yt.wer.efms.service.EmailService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private SystemSettingsRepository systemSettingsRepository;

    @InjectMocks
    private AuthController authController;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("arnaud");
        registerRequest.setPassword("secret");
        registerRequest.setEmail("arnaud@example.com");

        authRequest = new AuthRequest();
        authRequest.setUsername("arnaud");
        authRequest.setPassword("secret");

    }


    @Test
    void registerReturnsBadRequestWhenUsernameIsTaken() {
        User existing = new User();
        existing.setUsername("arnaud");
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("username_taken", response.getBody());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerCreatesUserWithEncodedPasswordAndDefaultTutorialState() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("arnaud", saved.getUsername());
        assertEquals("hashed-secret", saved.getPassword());
        assertEquals(TutorialState.NOT_STARTED, saved.getTutorialState());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getModifiedAt());
    }

    @Test
    void registerSetsEmailOnUser() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));

        authController.register(registerRequest);

        verify(userRepository).save(userCaptor.capture());
        assertEquals("arnaud@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void registerFirstUserBecomesAdmin() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));

        authController.register(registerRequest);

        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isAdmin());
    }

    @Test
    void registerSubsequentUserIsNotAdmin() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(5L);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));

        authController.register(registerRequest);

        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isAdmin());
    }

    @Test
    @SuppressWarnings("unchecked")
    void registerReturnsVerificationRequiredWhenEnabled() {
        SystemSettings settings = new SystemSettings();
        settings.setUserVerificationRequired(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("verification_required", body.get("message"));

        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isVerified());
        assertNotNull(userCaptor.getValue().getVerificationToken());
        verify(emailService).sendTemplatedEmail(eq("arnaud@example.com"), eq("VERIFICATION"), any(), any(), any());
    }

    @Test
    void registerReturnsEmailRequiredWhenVerificationEnabledAndEmailMissing() {
        SystemSettings settings = new SystemSettings();
        settings.setUserVerificationRequired(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        registerRequest.setEmail(null);

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("email_required", response.getBody());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerReturnsEmailRequiredWhenVerificationEnabledAndEmailInvalid() {
        SystemSettings settings = new SystemSettings();
        settings.setUserVerificationRequired(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        registerRequest.setEmail("notAnEmail");

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("email_required", response.getBody());
    }


    @Test
    void verifySucceedsWithValidToken() {
        User u = new User();
        u.setVerified(false);
        u.setVerificationToken("valid-token");
        when(userRepository.findAll()).thenReturn(List.of(u));

        ResponseEntity<?> response = authController.verify(Map.of("token", "valid-token"));

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isVerified());
        assertNull(userCaptor.getValue().getVerificationToken());
    }

    @Test
    void verifyReturns400WithInvalidToken() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = authController.verify(Map.of("token", "bad-token"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("invalid_token", response.getBody());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyReturns400WhenTokenKeyMissing() {
        ResponseEntity<?> response = authController.verify(Map.of());

        assertEquals(400, response.getStatusCode().value());
    }


    @Test
    @SuppressWarnings("unchecked")
    void getSettingsReturnsVerificationFlagFalseByDefault() {
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));

        ResponseEntity<?> response = authController.getSettings();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("verificationRequired"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getSettingsReturnsVerificationFlagTrueWhenSet() {
        SystemSettings settings = new SystemSettings();
        settings.setUserVerificationRequired(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        ResponseEntity<?> response = authController.getSettings();

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("verificationRequired"));
    }


    @Test
    void loginReturnsUnauthorizedWhenUserDoesNotExist() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.empty());
        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid_credentials", response.getBody());
    }

    @Test
    void loginReturnsUnauthorizedWhenPasswordDoesNotMatch() {
        User existing = new User();
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(true);

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(false);
        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid_credentials", response.getBody());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void loginReturns403WhenUserNotVerifiedAndVerificationRequired() {
        SystemSettings settings = new SystemSettings();
        settings.setUserVerificationRequired(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        User existing = new User();
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(false);

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("not_verified", response.getBody());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void loginSucceedsWhenVerificationDisabledEvenIfUserUnverified() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(false);
        existing.setTutorialState(TutorialState.NOT_STARTED);

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));
        when(jwtUtil.generateToken("arnaud")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void loginReturnsAuthResponseWhenCredentialsAreValid() {
        User existing = new User();
        existing.setId(10L);
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(true);
        existing.setTutorialState(TutorialState.IN_PROGRESS);
        existing.setOperationsPopupTopRight(true);
        existing.setEmail("arnaud@example.com");
        existing.setAvatarUrl("/avatars/arnaud.png");

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));
        when(jwtUtil.generateToken("arnaud")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof AuthResponse);

        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertNotNull(authResponse);
        assertEquals("jwt-token", authResponse.getToken());
        assertEquals(10L, authResponse.getUser_id());
        assertEquals(TutorialState.IN_PROGRESS, authResponse.getTutorialState());
        assertTrue(authResponse.isOperationsPopupTopRight());
        assertEquals("arnaud@example.com", authResponse.getEmail());
        assertEquals("/avatars/arnaud.png", authResponse.getAvatarUrl());
    }

    @Test
    void loginResponseIncludesAdminFlag() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(true);
        existing.setAdmin(true);
        existing.setTutorialState(TutorialState.NOT_STARTED);

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));
        when(jwtUtil.generateToken("arnaud")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(authRequest);

        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertNotNull(authResponse);
        assertTrue(authResponse.isAdmin());
    }

    @Test
    void loginResponseIncludesUserPreferences() {
        User existing = new User();
        existing.setId(42L);
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setVerified(true);
        existing.setTutorialState(TutorialState.NOT_STARTED);
        existing.setTimeFormat("HH:mm");
        existing.setDateFormat("dd/MM/yyyy");
        existing.setDefaultFarmId(7L);
        existing.setEmailNotificationsEnabled(false);
        existing.setPreferredLanguage("fr");

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(new SystemSettings()));
        when(jwtUtil.generateToken("arnaud")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(authRequest);

        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertNotNull(authResponse);
        assertEquals("HH:mm", authResponse.getTimeFormat());
        assertEquals("dd/MM/yyyy", authResponse.getDateFormat());
        assertEquals(7L, authResponse.getDefaultFarmId());
        assertFalse(authResponse.isEmailNotificationsEnabled());
        assertEquals("fr", authResponse.getPreferredLanguage());
    }
}

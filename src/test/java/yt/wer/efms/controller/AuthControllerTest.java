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
import yt.wer.efms.model.TutorialState;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.UserRepository;
import yt.wer.efms.security.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

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

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(false);

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid_credentials", response.getBody());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void loginReturnsAuthResponseWhenCredentialsAreValid() {
        User existing = new User();
        existing.setId(10L);
        existing.setUsername("arnaud");
        existing.setPassword("stored-hash");
        existing.setTutorialState(TutorialState.IN_PROGRESS);
        existing.setOperationsPopupTopRight(true);
        existing.setEmail("arnaud@example.com");
        existing.setAvatarUrl("/avatars/arnaud.png");

        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);
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
}

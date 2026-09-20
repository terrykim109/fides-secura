package com.bank.service;

import com.bank.api.dto.LoginRequest;
import com.bank.api.dto.RegisterRequest;
import com.bank.config.AppProperties;
import com.bank.domain.Role;
import com.bank.domain.User;
import com.bank.repository.UserRepository;
import com.bank.security.JwtService;
import com.bank.security.SecurityEventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    JwtService jwtService;
    @Mock
    SecurityEventRecorder securityEventRecorder;
    @Mock
    LoginRateLimiter loginRateLimiter;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("dev-only-change-me-to-a-long-random-secret-key", 15, 7),
                new AppProperties.Cors("http://localhost:5173"),
                new AppProperties.Auth(5, 15, 30)
        );
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                securityEventRecorder,
                loginRateLimiter,
                props
        );
    }

    @Test
    void loginSucceedsAndClearsFailures() {
        User user = new User("c@example.com", passwordEncoder.encode("password1"), "C", Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("c@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(any(), any(), any())).thenReturn("token");

        var response = authService.login(new LoginRequest("c@example.com", "password1"), "1.1.1.1", "test");

        assertEquals("token", response.accessToken());
        verify(securityEventRecorder).record(
                eq(SecurityEventTypes.LOGIN_SUCCESS),
                any(),
                any(),
                any(),
                eq("1.1.1.1"),
                eq("test"),
                any(),
                any()
        );
    }

    @Test
    void fiveFailuresEmitLockEvent() {
        User user = new User("c@example.com", passwordEncoder.encode("password1"), "C", Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("c@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 4; i++) {
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(new LoginRequest("c@example.com", "wrong"), "1.1.1.1", "test")
            );
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        ResponseStatusException fifth = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest("c@example.com", "wrong"), "1.1.1.1", "test")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, fifth.getStatusCode());

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(securityEventRecorder, atLeastOnce()).record(
                typeCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        assertTrue(typeCaptor.getAllValues().contains(SecurityEventTypes.ACCOUNT_LOCKED));
        assertTrue(user.isLocked(java.time.Instant.now()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("c@example.com")).thenReturn(true);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("c@example.com", "password1", "C", Role.CUSTOMER))
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}

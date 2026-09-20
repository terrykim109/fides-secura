package com.bank.service;

import com.bank.api.dto.AuthResponse;
import com.bank.api.dto.LoginRequest;
import com.bank.api.dto.RegisterRequest;
import com.bank.api.dto.UserResponse;
import com.bank.config.AppProperties;
import com.bank.domain.Role;
import com.bank.domain.SecuritySeverity;
import com.bank.domain.User;
import com.bank.repository.UserRepository;
import com.bank.security.JwtService;
import com.bank.security.SecurityEventTypes;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityEventRecorder securityEventRecorder;
    private final LoginRateLimiter loginRateLimiter;
    private final AppProperties appProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityEventRecorder securityEventRecorder,
            LoginRateLimiter loginRateLimiter,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityEventRecorder = securityEventRecorder;
        this.loginRateLimiter = loginRateLimiter;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role == Role.ADMIN || role == Role.ANALYST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot self-register privileged roles");
        }
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                role
        );
        userRepository.save(user);
        String token = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        loginRateLimiter.checkOrThrow(ipAddress == null ? "unknown" : ipAddress);

        String email = request.email().trim().toLowerCase();
        Instant now = Instant.now();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            securityEventRecorder.record(
                    SecurityEventTypes.LOGIN_FAILURE,
                    SecuritySeverity.LOW,
                    null,
                    null,
                    ipAddress,
                    userAgent,
                    null,
                    "{\"reason\":\"unknown_email\"}"
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        }

        if (user.isLocked(now)) {
            securityEventRecorder.record(
                    SecurityEventTypes.ACCOUNT_LOCKED,
                    SecuritySeverity.MEDIUM,
                    user.getId(),
                    user.getId(),
                    ipAddress,
                    userAgent,
                    null,
                    "{\"reason\":\"still_locked\",\"lockedUntil\":\"" + user.getLockedUntil() + "\"}"
            );
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account temporarily locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int maxFailed = appProperties.auth().maxFailedLogins();
            long lockMinutes = appProperties.auth().lockoutMinutes();
            user.registerFailedLogin(maxFailed, lockMinutes, now);
            userRepository.save(user);

            boolean justLocked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
            securityEventRecorder.record(
                    SecurityEventTypes.LOGIN_FAILURE,
                    SecuritySeverity.LOW,
                    user.getId(),
                    user.getId(),
                    ipAddress,
                    userAgent,
                    null,
                    "{\"failedLoginsBeforeLockLogic\":true}"
            );
            if (justLocked) {
                securityEventRecorder.record(
                        SecurityEventTypes.ACCOUNT_LOCKED,
                        SecuritySeverity.HIGH,
                        user.getId(),
                        user.getId(),
                        ipAddress,
                        userAgent,
                        null,
                        "{\"lockoutMinutes\":" + lockMinutes + "}"
                );
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.clearLockoutState();
        userRepository.save(user);

        securityEventRecorder.record(
                SecurityEventTypes.LOGIN_SUCCESS,
                SecuritySeverity.INFO,
                user.getId(),
                user.getId(),
                ipAddress,
                userAgent,
                null,
                "{\"ip\":\"" + sanitize(ipAddress) + "\"}"
        );

        String token = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return UserResponse.from(user);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "");
    }
}

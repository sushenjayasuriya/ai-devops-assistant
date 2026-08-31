package ai.devops.security.auth;

import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.dto.AuthResponse;
import ai.devops.security.auth.dto.LoginRequest;
import ai.devops.security.auth.dto.RefreshTokenRequest;
import ai.devops.security.auth.dto.UserProfileResponse;
import ai.devops.security.auth.entity.RefreshTokenEntity;
import ai.devops.security.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Email and password are required.");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedActionException("User account is disabled. Contact your administrator.");
        }

        // STRICT: Authentication succeeds ONLY when BCrypt matches stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.recordAudit(
                    "LOGIN_FAILED",
                    "USER",
                    user.getEmail(),
                    "AUTH",
                    RiskLevel.MEDIUM_RISK,
                    String.format("email=%s", user.getEmail()),
                    "FAILURE",
                    "Bad credentials",
                    null
            );
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = new ArrayList<>(user.getRoles());
        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), orgId, roles);

        // Generate and persist refresh token family
        String jti = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), orgId, jti);
        String tokenHash = hashToken(refreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity(
                jti,
                user,
                user.getOrganization(),
                familyId,
                tokenHash,
                expiresAt
        );
        refreshTokenRepository.save(refreshTokenEntity);

        auditService.recordAudit(
                "LOGIN_SUCCESS",
                "USER",
                user.getEmail(),
                "AUTH",
                RiskLevel.READ_ONLY,
                String.format("email=%s, org=%s", user.getEmail(), user.getOrganization() != null ? user.getOrganization().getName() : "None"),
                "SUCCESS",
                null,
                null
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                orgId,
                roles
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedActionException("Refresh token is required.");
        }

        if (!jwtTokenProvider.validateRefreshToken(rawRefreshToken)) {
            throw new UnauthorizedActionException("Invalid or expired refresh token signature.");
        }

        String tokenHash = hashToken(rawRefreshToken);
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new UnauthorizedActionException("Refresh token not found or already rotated.");
        }

        RefreshTokenEntity currentToken = tokenOpt.get();

        // Check for Token Replay / Reuse Attack
        if (currentToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token replay detected for family [{}] user [{}]! Revoking entire family.",
                    currentToken.getTokenFamily(), currentToken.getUser().getEmail());

            refreshTokenRepository.revokeFamily(currentToken.getTokenFamily(), Instant.now());

            auditService.recordAudit(
                    "SECURITY_ALERT:REFRESH_TOKEN_REPLAY",
                    "AUTH",
                    currentToken.getTokenFamily(),
                    "AUTH",
                    RiskLevel.HIGH_RISK,
                    String.format("user=%s, family=%s", currentToken.getUser().getEmail(), currentToken.getTokenFamily()),
                    "REJECTED",
                    "Attempted reuse of already rotated refresh token. Family revoked.",
                    null
            );

            throw new UnauthorizedActionException("Token reuse detected. Session has been revoked for security.");
        }

        if (currentToken.isExpired()) {
            throw new UnauthorizedActionException("Refresh token has expired. Please log in again.");
        }

        UserEntity user = currentToken.getUser();
        if (!user.isEnabled()) {
            throw new UnauthorizedActionException("User account is disabled.");
        }

        // Rotate: Revoke current token and record replacement
        String newJti = UUID.randomUUID().toString();
        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByJti(newJti);
        refreshTokenRepository.save(currentToken);

        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        List<String> roles = new ArrayList<>(user.getRoles());

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), orgId, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), orgId, newJti);
        String newTokenHash = hashToken(newRefreshToken);

        Instant newExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        RefreshTokenEntity newEntity = new RefreshTokenEntity(
                newJti,
                user,
                user.getOrganization(),
                currentToken.getTokenFamily(),
                newTokenHash,
                newExpiresAt
        );
        refreshTokenRepository.save(newEntity);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                orgId,
                roles
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedActionException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getOrganization() != null ? user.getOrganization().getName() : "None",
                new ArrayList<>(user.getRoles()),
                user.getLastLoginAt()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                refreshTokenRepository.revokeFamily(token.getTokenFamily(), Instant.now());
            });
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest unavailable", e);
        }
    }
}

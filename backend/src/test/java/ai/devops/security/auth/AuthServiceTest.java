package ai.devops.security.auth;

import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.dto.AuthResponse;
import ai.devops.security.auth.dto.LoginRequest;
import ai.devops.security.auth.dto.RefreshTokenRequest;
import ai.devops.security.auth.entity.RefreshTokenEntity;
import ai.devops.security.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditService auditService;

    private AuthService authService;

    private UserEntity mockUser;
    private OrganizationEntity mockOrg;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtTokenProvider, auditService);

        mockOrg = new OrganizationEntity("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());

        mockUser = new UserEntity();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("admin@devops.ai");
        mockUser.setPasswordHash("$2a$12$realHashedPasswordStringHere1234567890123456789012");
        mockUser.setFullName("Admin User");
        mockUser.setEnabled(true);
        mockUser.setOrganization(mockOrg);
        mockUser.setRoles(Set.of("ADMIN"));
    }

    @Test
    @DisplayName("Should successfully authenticate user when BCrypt password matches")
    void testLoginSuccess() {
        when(userRepository.findByEmail("admin@devops.ai")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Admin@123", mockUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any(), any())).thenReturn("mock-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        LoginRequest request = new LoginRequest("admin@devops.ai", "Admin@123");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("admin@devops.ai", response.getEmail());
        assertTrue(response.getRoles().contains("ADMIN"));
        verify(userRepository, times(1)).save(mockUser);
        verify(refreshTokenRepository, times(1)).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Should reject authentication when BCrypt password does not match")
    void testLoginInvalidPassword() {
        when(userRepository.findByEmail("admin@devops.ai")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPassword", mockUser.getPasswordHash())).thenReturn(false);

        LoginRequest request = new LoginRequest("admin@devops.ai", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should NOT allow password bypass for emails containing 'admin', 'devops', or 'viewer'")
    void testDevPasswordBypassIsRemoved() {
        UserEntity customUser = new UserEntity();
        customUser.setId(UUID.randomUUID());
        customUser.setEmail("admin-intruder@evil.org");
        customUser.setPasswordHash("$2a$12$anotherRandomHashString1234567890123456789012");
        customUser.setEnabled(true);

        when(userRepository.findByEmail("admin-intruder@evil.org")).thenReturn(Optional.of(customUser));
        when(passwordEncoder.matches("Admin@123", customUser.getPasswordHash())).thenReturn(false);

        LoginRequest request = new LoginRequest("admin-intruder@evil.org", "Admin@123");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should rotate refresh token and issue new token pair on valid refresh request")
    void testRefreshTokenRotationSuccess() {
        RefreshTokenEntity existingToken = new RefreshTokenEntity(
                "jti-1",
                mockUser,
                mockOrg,
                "family-1",
                "valid-hash",
                Instant.now().plusSeconds(3600)
        );

        when(jwtTokenProvider.validateRefreshToken("valid-raw-refresh-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existingToken));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any(), any())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        RefreshTokenRequest request = new RefreshTokenRequest("valid-raw-refresh-token");
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertTrue(existingToken.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Should detect token replay on already-revoked refresh token and revoke family")
    void testRefreshTokenReplayDetection() {
        RefreshTokenEntity revokedToken = new RefreshTokenEntity(
                "jti-1",
                mockUser,
                mockOrg,
                "family-1",
                "revoked-hash",
                Instant.now().plusSeconds(3600)
        );
        revokedToken.setRevokedAt(Instant.now().minusSeconds(60));

        when(jwtTokenProvider.validateRefreshToken("replayed-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest request = new RefreshTokenRequest("replayed-token");
        UnauthorizedActionException ex = assertThrows(UnauthorizedActionException.class, () -> authService.refreshToken(request));

        assertTrue(ex.getMessage().contains("Token reuse detected"));
        verify(refreshTokenRepository, times(1)).revokeFamily(eq("family-1"), any(Instant.class));
    }
}

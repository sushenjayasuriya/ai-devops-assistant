package ai.devops.security.auth;

import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.dto.AuthResponse;
import ai.devops.security.auth.dto.LoginRequest;
import ai.devops.security.auth.dto.RefreshTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private UserEntity mockUser;
    private OrganizationEntity mockOrg;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);

        mockOrg = new OrganizationEntity("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());

        mockUser = new UserEntity();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("admin@devops.ai");
        mockUser.setPasswordHash("$2a$12$hashedPassword");
        mockUser.setFullName("Admin User");
        mockUser.setEnabled(true);
        mockUser.setOrganization(mockOrg);
        mockUser.setRoles(Set.of("ADMIN"));
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void testLoginSuccess() {
        when(userRepository.findByEmail("admin@devops.ai")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Admin@123", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        LoginRequest request = new LoginRequest("admin@devops.ai", "Admin@123");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("admin@devops.ai", response.getEmail());
        assertTrue(response.getRoles().contains("ADMIN"));
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should reject authentication with invalid password")
    void testLoginInvalidPassword() {
        when(userRepository.findByEmail("admin@devops.ai")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest("admin@devops.ai", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should rotate tokens on valid refresh token")
    void testRefreshTokenSuccess() {
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(mockUser.getId().toString());
        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("new-refresh-token");

        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }
}

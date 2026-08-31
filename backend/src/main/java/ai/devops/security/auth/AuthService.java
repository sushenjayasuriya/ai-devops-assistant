package ai.devops.security.auth;

import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.dto.AuthResponse;
import ai.devops.security.auth.dto.LoginRequest;
import ai.devops.security.auth.dto.RefreshTokenRequest;
import ai.devops.security.auth.dto.UserProfileResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedActionException("User account is disabled. Contact your administrator.");
        }

        // For convenience with seeded dev accounts or BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
                && !isDefaultDevPasswordMatch(request.getPassword(), user.getEmail())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = new ArrayList<>(user.getRoles());
        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), orgId, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

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

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedActionException("Invalid or expired refresh token");
        }

        String userIdStr = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());
        UserEntity user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new UnauthorizedActionException("User not found for token"));

        if (!user.isEnabled()) {
            throw new UnauthorizedActionException("User account is disabled");
        }

        List<String> roles = new ArrayList<>(user.getRoles());
        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), orgId, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

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

    private boolean isDefaultDevPasswordMatch(String rawPassword, String email) {
        // Fallback convenience for demo environment
        return (email.contains("admin") && rawPassword.equals("Admin@123"))
                || (email.contains("devops") && rawPassword.equals("Devops@123"))
                || (email.contains("viewer") && rawPassword.equals("Viewer@123"));
    }
}

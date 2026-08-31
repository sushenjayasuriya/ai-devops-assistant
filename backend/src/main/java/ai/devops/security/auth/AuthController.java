package ai.devops.security.auth;

import ai.devops.common.logging.CorrelationIdFilter;
import ai.devops.common.response.ApiResponse;
import ai.devops.security.auth.dto.AuthResponse;
import ai.devops.security.auth.dto.LoginRequest;
import ai.devops.security.auth.dto.RefreshTokenRequest;
import ai.devops.security.auth.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication, token refresh, and identity")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response, CorrelationIdFilter.getCurrentCorrelationId()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response, CorrelationIdFilter.getCurrentCorrelationId()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserProfileResponse profile = authService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}

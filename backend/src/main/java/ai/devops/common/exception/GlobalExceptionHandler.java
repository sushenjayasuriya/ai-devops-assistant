package ai.devops.common.exception;

import ai.devops.common.logging.CorrelationIdFilter;
import ai.devops.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(ApprovalRequiredException.class)
    public ResponseEntity<ErrorResponse> handleApprovalRequired(ApprovalRequiredException ex) {
        log.warn("Approval required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAction(UnauthorizedActionException ex) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationUnavailable(IntegrationUnavailableException ex) {
        log.error("Integration unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationTimeout(IntegrationTimeoutException ex) {
        log.error("Integration timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationAuthException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationAuth(IntegrationAuthException ex) {
        log.error("Integration auth failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationForbidden(IntegrationForbiddenException ex) {
        log.warn("Integration forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationInvalidResponseException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationInvalidResponse(IntegrationInvalidResponseException ex) {
        log.error("Integration invalid response: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationConfigException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationConfig(IntegrationConfigException ex) {
        log.warn("Integration configuration error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(SsrfProtectionException.class)
    public ResponseEntity<ErrorResponse> handleSsrfException(SsrfProtectionException ex) {
        log.warn("SSRF protection triggered: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(IntegrationException ex) {
        log.error("Integration failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        log.error("Application error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), CorrelationIdFilter.getCurrentCorrelationId(), ex.getDetails()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password", CorrelationIdFilter.getCurrentCorrelationId(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "You do not have permission to perform this action", CorrelationIdFilter.getCurrentCorrelationId(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> validationErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "Validation failed for one or more fields", CorrelationIdFilter.getCurrentCorrelationId(), validationErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please contact support.", CorrelationIdFilter.getCurrentCorrelationId(), null));
    }
}

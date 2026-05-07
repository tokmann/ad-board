package com.adboard.exception;

import com.adboard.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 400: Validation error/Bad arguments
  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      IllegalArgumentException.class
  })
  public ResponseEntity<ErrorResponseDto> handleValidation(Exception ex, HttpServletRequest request) {
    String message = (ex instanceof MethodArgumentNotValidException validEx)
        ? validEx.getBindingResult().getAllErrors().get(0).getDefaultMessage()
        : ex.getMessage();

    log.error("Bad request: {}", message);
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  // 401: Authentication error
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponseDto> handleUnauthorized(BadCredentialsException ex, HttpServletRequest request) {
    log.error("Authentication failed: {}", ex.getMessage());
    return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
  }

  // 403: Access Violation
  @ExceptionHandler({
      UnauthorizedActionException.class,
      AccessDeniedException.class,
  })
  public ResponseEntity<ErrorResponseDto> handleForbidden(Exception ex, HttpServletRequest request) {
    log.error("Access denied: {}", ex.getMessage());
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  // 404: Everything that was not found
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    log.error("Resource not found: {}", ex.getMessage());
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  // 409: Business logic conflicts
  @ExceptionHandler({
      UserAlreadyExistsException.class,
      AdAlreadyPromotedException.class,
      ReviewAlreadyExistsException.class,
      ConversationBlockedException.class,
  })
  public ResponseEntity<ErrorResponseDto> handleBusinessConflict(RuntimeException ex, HttpServletRequest request) {
    log.error("Business logic conflict: {}", ex.getMessage());
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  // 500: Internal server errors
  @ExceptionHandler({
      IllegalStateException.class,
      Exception.class
  })
  public ResponseEntity<ErrorResponseDto> handleInternalError(Exception ex, HttpServletRequest request) {
    log.error("Internal server error: ", ex);
    String message = "An unexpected error occurred. Please try again later.";
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
  }

  private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .status(status.value())
            .message(message)
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build(),
        status);
  }
}

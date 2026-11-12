package com.eventpro.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        Map<String, Object> body = buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI()
        );

        body.put("validationErrors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        return ResponseEntity
                .status(status)
                .body(buildError(
                        status,
                        ex.getReason() != null
                                ? ex.getReason()
                                : "Request failed",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(
                        HttpStatus.CONFLICT,
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseError(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Database integrity error", ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(
                        HttpStatus.CONFLICT,
                        "Database constraint violation. Check server logs.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Invalid request body: {}", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(buildError(
                        HttpStatus.BAD_REQUEST,
                        "Invalid JSON body or field type.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, Object> body = buildError(
                HttpStatus.BAD_REQUEST,
                "Constraint validation failed",
                request.getRequestURI()
        );

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation ->
                errors.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );

        body.put("validationErrors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .badRequest()
                .body(buildError(
                        HttpStatus.BAD_REQUEST,
                        "Invalid parameter: " + ex.getName(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        // Print the full exception in IntelliJ
        log.error("Unexpected error at {}", request.getRequestURI(), ex);

        Map<String, Object> body = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "Unknown server error",
                request.getRequestURI()
        );

        // Show the exception class
        body.put("exception", ex.getClass().getName());

        // Show the root cause
        Throwable rootCause = ex;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        body.put("rootCause", rootCause.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    private Map<String, Object> buildError(
            HttpStatusCode status,
            String message,
            String path) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());

        HttpStatus httpStatus = HttpStatus.resolve(status.value());

        body.put(
                "error",
                httpStatus != null
                        ? httpStatus.getReasonPhrase()
                        : "HTTP Error"
        );

        body.put("message", message);
        body.put("path", path);

        return body;
    }


    //=========== promotion =================
//    @ExceptionHandler(EventNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleEventNotFound(
//            EventNotFoundException ex,
//            HttpServletRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(buildError(
//                        HttpStatus.NOT_FOUND,
//                        ex.getMessage(),
//                        request.getRequestURI()
//                ));
//    }
//
//    @ExceptionHandler(OrganizerNotFound.class)
//    public ResponseEntity<Map<String, Object>> handleOrganizerNotFound(
//            OrganizerNotFound ex,
//            HttpServletRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(buildError(
//                        HttpStatus.NOT_FOUND,
//                        ex.getMessage(),
//                        request.getRequestURI()
//                ));
//    }
//
//    @ExceptionHandler(PromotionCodeAlreadyExistsException.class)
//    public ResponseEntity<Map<String, Object>> handlePromotionCodeExists(
//            PromotionCodeAlreadyExistsException ex,
//            HttpServletRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.CONFLICT)
//                .body(buildError(
//                        HttpStatus.CONFLICT,
//                        ex.getMessage(),
//                        request.getRequestURI()
//                ));
//    }
//
//    @ExceptionHandler(InvalidUserTypeException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidUserType(
//            InvalidUserTypeException ex,
//            HttpServletRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(buildError(
//                        HttpStatus.BAD_REQUEST,
//                        ex.getMessage(),
//                        request.getRequestURI()
//                ));
//    }

}
package com.hubnex.backend.config;

import com.hubnex.backend.dto.imports.DocketRecordImportResultDto;
import com.hubnex.backend.exception.DocketRecordImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ValidationExceptionHandler {

    @ExceptionHandler(DocketRecordImportException.class)
    public ResponseEntity<DocketRecordImportResultDto> handleDocketRecordImport(DocketRecordImportException ex) {
        log.warn("Invalid DocketRecord Excel import: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getResult());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation error on {}: {}", ex.getParameter().getExecutable().toGenericString(), errors);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
}

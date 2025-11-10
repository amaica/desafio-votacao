package com.example.votacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> nf(NotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "type","not-found","message", ex.getMessage()));
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> cf(ConflictException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "type","conflict","message", ex.getMessage()));
    }
    @ExceptionHandler(UnprocessableException.class)
    public ResponseEntity<?> up(UnprocessableException ex){
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "type","unprocessable","message", ex.getMessage()));
    }
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> fb(ForbiddenException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "type","forbidden","message", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> bad(MethodArgumentNotValidException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "type","validation-error","message", ex.getBindingResult().toString()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> generic(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "type","error","message", ex.getMessage()));
    }
}

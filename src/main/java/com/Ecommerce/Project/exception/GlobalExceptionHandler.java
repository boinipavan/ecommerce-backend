package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;
import com.Ecommerce.Project.record.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex){
        log.error("unhandled exception occurred",ex);
        ErrorResponse response=new ErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(),
                      "Something went wrong",
                       MDC.get("traceId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyException(IdempotencyException ex){

        log.error(
                "Idempotency failure code={} key={}",
                ex.getCode(),
                ex.getIdempotencyKey(),
                ex
        );

        ErrorResponse response=new ErrorResponse(LocalDateTime.now(),HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getCode(),HttpStatus.INTERNAL_SERVER_ERROR.name(),ex.getMessage(),MDC.get("traceId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGeneric(
//            Exception ex) {
//
//        log.error("Unhandled exception occurred", ex);
//
//        ErrorResponse response =
//                new ErrorResponse(
//                        LocalDateTime.now(),
//                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                        "INTERNAL_SERVER_ERROR",
//                        "Something went wrong",
//                        MDC.get("traceId")
//                );
//
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(response);
//    }
}

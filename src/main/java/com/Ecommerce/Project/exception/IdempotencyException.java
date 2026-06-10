package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;
import lombok.Getter;

@Getter
public abstract class IdempotencyException extends RuntimeException{
    private final String idempotencyKey;
    private final ErrorCode code;
    protected IdempotencyException(String message, String idempotencyKey, ErrorCode code, Throwable cause) {
        super(message, cause);
        this.idempotencyKey=idempotencyKey;
        this.code=code;
    }
}

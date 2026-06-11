package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;

public class IdempotencyJsonProcessingFailureException extends IdempotencyException{


    public IdempotencyJsonProcessingFailureException(String message, String idempotencyKey, Throwable cause) {
        super(message,idempotencyKey, ErrorCode.IDEMPOTENCY_JSON_PROCESSING_FAILED,cause);
    }
}

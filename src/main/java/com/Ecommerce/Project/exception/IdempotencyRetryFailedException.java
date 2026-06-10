package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;

public class IdempotencyRetryFailedException extends IdempotencyException{

    public IdempotencyRetryFailedException(String message,  String idempotencyKey,Throwable cause) {
        super(message,idempotencyKey, ErrorCode.IDEMPOTENCY_RETRY_FAILED,cause);
    }
}

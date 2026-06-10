package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;

public class IdempotencyStateTransitionException extends IdempotencyException{

    public IdempotencyStateTransitionException(String message,  String idempotencyKey,Throwable cause) {
        super(message,idempotencyKey, ErrorCode.IDEMPOTENCY_STATE_TRANSITION_FAILED,cause);
    }
}

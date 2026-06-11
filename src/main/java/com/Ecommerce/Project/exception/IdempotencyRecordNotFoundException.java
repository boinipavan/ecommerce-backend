package com.Ecommerce.Project.exception;

import com.Ecommerce.Project.enums.ErrorCode;

public class IdempotencyRecordNotFoundException extends IdempotencyException{

    public IdempotencyRecordNotFoundException(String message,String idempotencyKey,Throwable cause) {
        super(message,idempotencyKey, ErrorCode.IDEMPOTENCY_RECORD_NOT_FOUND,cause);

    }

}

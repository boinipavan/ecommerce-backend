package com.Ecommerce.Project.APIResponse;

import com.Ecommerce.Project.Entity.Idempotency;

public class IdempotencyAPIResponse<T>{
    public Idempotency.Status status;
    public String message;
    public Integer statusCode;
    public  T response;

    public  IdempotencyAPIResponse(Idempotency.Status status, String message, Integer statusCode, T response) {
        this.status = status;
        this.message = message;
        this.statusCode = statusCode;
        this.response =  response;
    }
}

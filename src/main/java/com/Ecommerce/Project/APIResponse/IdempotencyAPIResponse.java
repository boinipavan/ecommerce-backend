package com.Ecommerce.Project.APIResponse;

import com.Ecommerce.Project.Entity.Idempotency;

public class IdempotencyAPIResponse<T>{
    public Idempotency.Status success;
    public String message;
    public Integer statusCode;
    public  T response;

    public  IdempotencyAPIResponse(Idempotency.Status success, String message, Integer statusCode, T response) {
        this.success = success;
        this.message = message;
        this.statusCode = statusCode;
        this.response =  response;
    }
}

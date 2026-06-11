package com.Ecommerce.Project.service;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

public interface IdempotencyService {

   public <T,R> IdempotencyAPIResponse<T> handleIdempotentRequest(String idempotency_key,int userId, R request, Supplier<ResponseEntity<T>> businessLogic, Class<T> responseType, HttpServletRequest servletRequest);

   public void cleanUpExpiredIdempotencyRecords();
}

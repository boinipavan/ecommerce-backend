package com.Ecommerce.Project.service;

import com.Ecommerce.Project.Entity.Idempotency;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;

public interface IdempotencyPersistenceService {
    public Idempotency markInProgress(String requestPayloadFingerprint, String idempotency_key, int userId, LocalDateTime now);
    public <T> void markSuccess(T order, int statusCode, Idempotency savedIdempotencyRecord) throws JsonProcessingException;
    public  void markFailure(Idempotency savedIdempotencyRecord);
    public boolean tryAcquireRowForRetry(Long id,LocalDateTime now,LocalDateTime newExpiry);
}

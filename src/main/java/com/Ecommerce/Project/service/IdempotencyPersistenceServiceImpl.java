package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.exception.IdempotencyRecordNotFoundException;
import com.Ecommerce.Project.exception.IdempotencyStateTransitionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Service
public class IdempotencyPersistenceServiceImpl implements IdempotencyPersistenceService{

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyPersistenceServiceImpl(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }


    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public  void markFailure(Idempotency savedIdempotencyRecord){
        //refetching idempotency record to avoid stale data update
        Idempotency record=idempotencyRepository.findById(savedIdempotencyRecord.getId()).orElseThrow(()->new IdempotencyRecordNotFoundException("Idempotency Record Not Found While Updating Record to Failure",savedIdempotencyRecord.getIdempotencyKey(),null));
        if(record.getStatus()== Idempotency.Status.COMPLETED){
            return ;
        }
        record.setUpdatedAt(LocalDateTime.now());
        record.setStatus(Idempotency.Status.FAILED);
        idempotencyRepository.save(record);
    }

    @Override
    @Transactional
    public boolean tryAcquireRowForRetry(Long id, LocalDateTime now, LocalDateTime newExpiry) {
        int isUpdated=idempotencyRepository.tryAcquireRowForRetry(id,now,newExpiry);
        return isUpdated==1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> void markSuccess(T order, int statusCode, Idempotency savedIdempotencyRecord) throws JsonProcessingException {
        //refetching idempotency record to avoid stale data update

        Idempotency record=idempotencyRepository.findById(savedIdempotencyRecord.getId()).orElseThrow(()->new IdempotencyRecordNotFoundException("Idempotency Record Not Found While Updating Record to Success",savedIdempotencyRecord.getIdempotencyKey(),null));
        if(record.getStatus()!= Idempotency.Status.IN_PROGRESS){
            return;
        }

        String responsePayload=objectMapper.writeValueAsString(order);

        int isUpdated=idempotencyRepository.markSuccessIfInProgress(record.getId(),responsePayload,LocalDateTime.now(),statusCode);
        if(isUpdated==0){
            throw new IdempotencyStateTransitionException("failure occurred while marking success",savedIdempotencyRecord.getIdempotencyKey(),null);
        }
    }

    @Transactional
    public Idempotency markInProgress(String requestPayloadFingerprint, String idempotency_key, int userId, LocalDateTime now) {
        Idempotency newIdempotencyEntry = new Idempotency();
        newIdempotencyEntry.setIdempotencyKey(idempotency_key);
        newIdempotencyEntry.setStatus(Idempotency.Status.IN_PROGRESS);
        newIdempotencyEntry.setRequestPayloadHash(requestPayloadFingerprint);
        newIdempotencyEntry.setUserId(userId);
        newIdempotencyEntry.setCreatedAt(now);
        newIdempotencyEntry.setUpdatedAt(now);
        newIdempotencyEntry.setExpireAt(now.plusHours(24));
        return idempotencyRepository.save(newIdempotencyEntry);
    }
}
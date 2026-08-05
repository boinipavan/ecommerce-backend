package com.Ecommerce.Project.service;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.exception.IdempotencyJsonProcessingFailureException;
import com.Ecommerce.Project.exception.IdempotencyRecordNotFoundException;
import com.Ecommerce.Project.exception.IdempotencyRetryFailedException;
import com.Ecommerce.Project.exception.IdempotencyStateTransitionException;
import com.Ecommerce.Project.infrastructure.hashing.HashService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final ObjectMapper objectMapper;
    private final IdempotencyRepository idempotencyRepository;
    private final HashService hashService;
    private final IdempotencyPersistenceService idempotencyPersistenceService;

    @Autowired
    public IdempotencyServiceImpl(IdempotencyRepository idempotencyRepository, HashService hashService,IdempotencyPersistenceService idempotencyPersistenceService,ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.hashService=hashService;
        this.idempotencyPersistenceService=idempotencyPersistenceService;
        this.objectMapper=objectMapper;
    }

    @Override
    public <T,R> IdempotencyAPIResponse<T> handleIdempotentRequest(String idempotency_key, int userId, R request, Supplier<ResponseEntity<T>> businessLogic, Class<T> responseType, HttpServletRequest servletRequest) {
        String requestpayloadFingerprint=null;
        Idempotency savedIdempotencyRecord =null;
        LocalDateTime now=LocalDateTime.now();

        try {
            String requestPayloadAsString = objectMapper.writeValueAsString(request);
            String hashedRequestPayload= hashService.generateHash(requestPayloadAsString);
            requestpayloadFingerprint= generateFingerprint(hashedRequestPayload,servletRequest);

            log.info(
                    "Idempotent request received key={} fingerprint={}",
                    idempotency_key,
                    requestpayloadFingerprint
            );

            //saving IN_PROGRESS in idempotency table
            savedIdempotencyRecord= idempotencyPersistenceService.markInProgress(requestpayloadFingerprint,idempotency_key,userId,now);

            log.info(
                    "Idempotency record created key={}",
                    idempotency_key
            );

            //this method have transactional at method definition
            //Executing business logic
            ResponseEntity<T> response=businessLogic.get();

            //mark success and return
            return updateSuccessStatusInIdempotencyTable(response,savedIdempotencyRecord);

        }catch(DataIntegrityViolationException ex){
            Idempotency existingIdempotencyRecord=idempotencyRepository.findByUserIdAndIdempotencyKey(userId,idempotency_key).orElseThrow(()->new IdempotencyRecordNotFoundException("Idempotency Record Not Found",idempotency_key,null));
            String existingRequestPayloadFingerprint=existingIdempotencyRecord.getRequestPayloadHash();
            if(!Objects.equals(requestpayloadFingerprint,existingRequestPayloadFingerprint)){
                log.warn("idempotency Key={} is in Conflict with other request",idempotency_key);
                return  new IdempotencyAPIResponse<>(Idempotency.Status.FAILED,
                        "Idempotency Key Is In Conflict With Other Already Processed Key, Use Different Key",409,null);
            }
            else {
                Idempotency.Status status=existingIdempotencyRecord.getStatus();
                Integer statusCode=existingIdempotencyRecord.getStatusCode();
                LocalDateTime expireAt=existingIdempotencyRecord.getExpireAt();
                if(expireAt.isBefore(now) && (status== Idempotency.Status.IN_PROGRESS || status== Idempotency.Status.FAILED)) {
                    ///here retry logic for failed requests depends on business rules like payments
                    //but for now I am allowing to reuse the row
                    return allowRetryOnExistingRecord(existingIdempotencyRecord, businessLogic, now,responseType,userId);
                }

                if(status== Idempotency.Status.FAILED){
                    log.warn(
                            "Returning FAILED response for idempotency key={}",
                            idempotency_key
                    );
                    return new IdempotencyAPIResponse<>(status,"Request Failed",
                            statusCode,null);
                }
                else if(status==Idempotency.Status.IN_PROGRESS ){
                    log.info("idempotency key={} request is inprogress",idempotency_key);
                    return new IdempotencyAPIResponse<>(status,"Request Is In Progress",
                            202,null);
                }
                try {
                    T existingOrder = objectMapper.readValue(existingIdempotencyRecord.getResponsePayload(), responseType);
                    log.info(
                            "Returning cached response for idempotency key={}",
                            idempotency_key
                    );
                    return new IdempotencyAPIResponse<>(status,
                            "Request Processed Successfully",
                            statusCode, existingOrder);
                } catch (JsonProcessingException e) {
                    throw new IdempotencyJsonProcessingFailureException("unhandled Exception Occurred with Object Mapper While Handling Idempotency Key Conflict for Idempotency Key",existingIdempotencyRecord.getIdempotencyKey(),e);
                }
            }
        }
        catch (Exception e) {
            Optional<Idempotency> refetchedIdempotency =idempotencyRepository.findByUserIdAndIdempotencyKey(savedIdempotencyRecord.getUserId(),savedIdempotencyRecord.getIdempotencyKey());
            if(refetchedIdempotency.isPresent()) {
               //idempotencyPersistenceService.markFailure(savedIdempotencyRecord);
            }
            throw new RuntimeException("unhandled Exception Occurred While Handling Idempotency Key Conflict",e);
        }
    }


    public String generateFingerprint(String hashPayload, HttpServletRequest servletRequest){
        return servletRequest.getMethod()+":"+servletRequest.getRequestURI()+":"+hashPayload;
    }


    public <T> IdempotencyAPIResponse<T> allowRetryOnExistingRecord(Idempotency existingIdempotencyRecord, Supplier<ResponseEntity<T>> businessLogic,LocalDateTime now,Class<T> responseType,int userId){
        log.info(
                "Expired idempotency record reused for retry key={} previousStatus={}",
                existingIdempotencyRecord.getIdempotencyKey(),
                existingIdempotencyRecord.getStatus()
        );


        //here updated means row is acquired and updated
        boolean isUpdated= idempotencyPersistenceService.tryAcquireRowForRetry(existingIdempotencyRecord.getId(),now,now.plusHours(24));
        if(!isUpdated){
            //other transaction is processing request
            Idempotency finalExistingIdempotencyRecord = existingIdempotencyRecord;
            Idempotency latestIdempotencyRecord=idempotencyRepository.findByUserIdAndIdempotencyKey(userId, existingIdempotencyRecord.getIdempotencyKey()).orElseThrow(()->new IdempotencyRecordNotFoundException("Record Not Found For Idempotency Key, While Trying To Acquire Record To Retry", finalExistingIdempotencyRecord.getIdempotencyKey(),null));
            Idempotency.Status status=latestIdempotencyRecord.getStatus();
            if(status==Idempotency.Status.COMPLETED) {
                try {
                    log.info(
                            "Try Acquire Failed But Retry completed successfully by Other Request for idempotency key={}",
                            existingIdempotencyRecord.getIdempotencyKey()
                    );

                    T existingOrder = objectMapper.readValue(latestIdempotencyRecord.getResponsePayload(), responseType);
                    return new IdempotencyAPIResponse<>(Idempotency.Status.COMPLETED, "Request Processed Successfully", 200, existingOrder);
                } catch ( JsonProcessingException ex) {
                    throw new IdempotencyJsonProcessingFailureException("unHandled Exception Occurred While Returning Success Message After Acquiring Record For Retry with Idempotency Key",existingIdempotencyRecord.getIdempotencyKey(),ex);
                }
            }
            else if(status== Idempotency.Status.FAILED){
                log.warn("Retry lock acquisition failed key={} reason=another_transaction_acquired_lock"
                        ,existingIdempotencyRecord.getIdempotencyKey());
                return new IdempotencyAPIResponse<>(status,"Request Failed",
                        latestIdempotencyRecord.getStatusCode(),null);
            }
            else if(status== Idempotency.Status.IN_PROGRESS) {
                return new IdempotencyAPIResponse<>(Idempotency.Status.IN_PROGRESS, "Request Is In Progress", 202, null);
            }
            else{
                throw new IdempotencyRetryFailedException("Unhandled Exception While Retrying idempotent request key= ",
                        existingIdempotencyRecord.getIdempotencyKey(),null);
            }
        }
        //refetch new idempotency record as it would have updated
        //executing business logic
        try {
            Idempotency finalExistingIdempotencyRecord1 = existingIdempotencyRecord;
            existingIdempotencyRecord =idempotencyRepository.findById(existingIdempotencyRecord.getId()).orElseThrow(()->new IdempotencyRecordNotFoundException("Idempotency Record Not Found While Refetching After Acquiring Lock", finalExistingIdempotencyRecord1.getIdempotencyKey(),null));
            ResponseEntity<T> order = businessLogic.get();
            return updateSuccessStatusInIdempotencyTable(order, existingIdempotencyRecord);
        }
        catch (Exception ex){
            markFailure(existingIdempotencyRecord);
            throw new IdempotencyRetryFailedException("Failure Occurred While Attempting to Retry Record With Idempotency Key",existingIdempotencyRecord.getIdempotencyKey(),ex);
        }
    }

    public <T> IdempotencyAPIResponse<T> updateSuccessStatusInIdempotencyTable(ResponseEntity<T> response, Idempotency savedIdempotencyRecord){
        T order=response.getBody();
        int statusCode=response.getStatusCode().value();
        try {
            idempotencyPersistenceService.markSuccess(order, statusCode, savedIdempotencyRecord);
            return new IdempotencyAPIResponse<>(Idempotency.Status.COMPLETED, "Request processed Successfully", statusCode, order);
        }
        catch (Exception ex){
            throw new IdempotencyStateTransitionException("Exception Occurred While Marking Success Status In Idempotency Table With idempotency key",savedIdempotencyRecord.getIdempotencyKey(),ex);
        }
    }

    public void markFailure(Idempotency existingIdempotencyRecord){
        try{
            idempotencyPersistenceService.markFailure(existingIdempotencyRecord);
        } catch (RuntimeException ex) {
            throw new IdempotencyStateTransitionException("Exception Occurred While Updating Record to Failure with Idempotency Key",existingIdempotencyRecord.getIdempotencyKey(),ex);
        }
    }

    @Transactional
    public void cleanUpExpiredIdempotencyRecords(){
        LocalDateTime now=LocalDateTime.now();
        int deletedRows= idempotencyRepository.cleanUpExpiredIdempotencyRecords(now);
        if(deletedRows>0) {
            log.info("Expired idempotency records deleted count={}",deletedRows);
        }
    }
}


/*TX1:
  save IN_PROGRESS   ✅ committed

NO TX:
  chargeCard()       ✅
  business logic     ❌/✅

TX2:
  update SUCCESS/FAILED  ✅ committed*/
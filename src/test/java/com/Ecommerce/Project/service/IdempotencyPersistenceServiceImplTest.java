package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.exception.IdempotencyRecordNotFoundException;
import com.Ecommerce.Project.exception.IdempotencyStateTransitionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyPersistenceServiceImplTest {
    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private IdempotencyPersistenceServiceImpl idempotencyPersistenceService;

    private static final String IDEMPOTENCY_KEY="KEY-12345";
    private static final String FINGERPRINT = "POST:/orders:hash123";
    private static final String RESPONSE_PAYLOAD="payload";

    @Test
    public void shouldThrowStateTransitionExceptionWhenMarkSuccessUpdateCountIsZero() throws JsonProcessingException {
        Idempotency idempotency=buildInProgressRecord();
        when(idempotencyRepository.findById(idempotency.getId())).thenReturn(Optional.of(idempotency));
        when(objectMapper.writeValueAsString(any())).thenReturn(RESPONSE_PAYLOAD);
        when(idempotencyRepository.markSuccessIfInProgress(eq(idempotency.getId()),eq(RESPONSE_PAYLOAD),any(LocalDateTime.class),eq(201))).thenReturn(0);

        IdempotencyStateTransitionException ex=assertThrows(IdempotencyStateTransitionException.class,()->idempotencyPersistenceService.markSuccess(new Order(),201,idempotency));

        verify(objectMapper).writeValueAsString(any());
        verify(idempotencyRepository).markSuccessIfInProgress(any(),any(),any(),anyInt());
        verify(idempotencyRepository).findById(anyLong());
        assertEquals(idempotency.getIdempotencyKey(),ex.getIdempotencyKey());
    }

    @Test
    public void shouldMarkSuccessSuccessfully() throws JsonProcessingException {
        Idempotency idempotency=buildInProgressRecord();
        when(idempotencyRepository.findById(idempotency.getId())).thenReturn(Optional.of(idempotency));
        when(objectMapper.writeValueAsString(any())).thenReturn(RESPONSE_PAYLOAD);
        when(idempotencyRepository.markSuccessIfInProgress(eq(idempotency.getId()),eq(RESPONSE_PAYLOAD),any(LocalDateTime.class),eq(201))).thenReturn(1);

        assertDoesNotThrow(()->idempotencyPersistenceService.markSuccess(new Order(),201,idempotency));

        verify(objectMapper).writeValueAsString(any());
        verify(idempotencyRepository).markSuccessIfInProgress(any(),any(),any(),anyInt());
        verify(idempotencyRepository).findById(anyLong());
    }

    @Test
    public void shouldThrowExceptionWhenRecordNotFoundDuringMarkSuccess() throws JsonProcessingException {
        Idempotency idempotency=buildCompletedRecord();
        when(idempotencyRepository.findById(idempotency.getId())).thenReturn(Optional.empty());

        assertThrows(IdempotencyRecordNotFoundException.class,()->idempotencyPersistenceService.markSuccess(new Order(),201,idempotency));

        verify(objectMapper,never()).writeValueAsString(any());
        verify(idempotencyRepository,never()).markSuccessIfInProgress(any(),any(),any(),anyInt());
    }

    @Test
    public void markSuccessShouldNotExecuteWhenStatusNotInProgress() throws JsonProcessingException {
        Idempotency idempotency=buildCompletedRecord();
        when(idempotencyRepository.findById(idempotency.getId())).thenReturn(Optional.of(idempotency));
        idempotencyPersistenceService.markSuccess(new Order(),201,idempotency);
        verify(objectMapper,never()).writeValueAsString(any());
        verify(idempotencyRepository,never()).markSuccessIfInProgress(any(),any(),any(),anyInt());
    }

    @Test
    public void shouldReturnFalseWhenRetryRowAcquired(){
        when(idempotencyRepository.tryAcquireRowForRetry(any(),any(LocalDateTime.class),any(LocalDateTime.class))).thenReturn(0);

        boolean isUpdatedAndRowAcquired=idempotencyPersistenceService.tryAcquireRowForRetry(1L,LocalDateTime.now(),LocalDateTime.now().plusHours(1));

        assertFalse(isUpdatedAndRowAcquired);
        verify(idempotencyRepository).tryAcquireRowForRetry(any(),any(LocalDateTime.class),any(LocalDateTime.class));
    }

    @Test
    public void shouldReturnTrueWhenRetryRowAcquired(){
        when(idempotencyRepository.tryAcquireRowForRetry(any(),any(LocalDateTime.class),any(LocalDateTime.class))).thenReturn(1);

        boolean isUpdatedAndRowAcquired=idempotencyPersistenceService.tryAcquireRowForRetry(1L,LocalDateTime.now(),LocalDateTime.now().plusHours(1));

        assertTrue(isUpdatedAndRowAcquired);
        verify(idempotencyRepository).tryAcquireRowForRetry(any(),any(LocalDateTime.class),any(LocalDateTime.class));
    }

    @Test
    public void shouldThrowExceptionWhenRecordNotFoundDuringMarkFailure(){
        Idempotency staleRecord=buildInProgressRecord();
        when(idempotencyRepository.findById(staleRecord.getId())).thenReturn(Optional.empty());
        assertThrows(IdempotencyRecordNotFoundException.class,()->idempotencyPersistenceService.markFailure(staleRecord));

        verify(idempotencyRepository,never()).save(any());
        verify(idempotencyRepository).findById(any());
    }

    @Test
    public void shouldNotMarkFailureWhenRecordAlreadyCompleted(){
        Idempotency staleRecord=buildInProgressRecord();
        Idempotency latestRecord=buildCompletedRecord();
        when(idempotencyRepository.findById(any())).thenReturn(Optional.of(latestRecord));

        idempotencyPersistenceService.markFailure(staleRecord);

        verify(idempotencyRepository,never()).save(any());
        verify(idempotencyRepository).findById(any());
    }

    @Test
    public void shouldMarkFailureSuccessfully(){
        Idempotency idempotency=buildInProgressRecord();
        ArgumentCaptor<Idempotency> captor=ArgumentCaptor.forClass(Idempotency.class);

        when(idempotencyRepository.findById(any())).thenReturn(Optional.of(idempotency));

        idempotencyPersistenceService.markFailure(idempotency);

        verify(idempotencyRepository).save(captor.capture());
        Idempotency saved=captor.getValue();
        assertEquals(Idempotency.Status.FAILED,saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
        verify(idempotencyRepository).findById(any());
    }

    private Idempotency buildExpiredFailedRecord(){
        Idempotency record=new Idempotency();
        record.setExpireAt(LocalDateTime.now().minusHours(1));
        return record;
    }
    private Idempotency buildInProgressRecord(){
        Idempotency record=new Idempotency();
        record.setId(1L);
        record.setIdempotencyKey(IDEMPOTENCY_KEY);
        record.setStatus(Idempotency.Status.IN_PROGRESS);
        record.setExpireAt(LocalDateTime.now().plusHours(1));
        record.setRequestPayloadHash(FINGERPRINT);
        return record;
    }
    private Idempotency buildFailedResponse(){
        Idempotency record=new Idempotency();
        record.setId(1L);
        record.setIdempotencyKey(IDEMPOTENCY_KEY);
        record.setStatusCode(500);
        record.setRequestPayloadHash(FINGERPRINT);
        record.setStatus(Idempotency.Status.FAILED);
        record.setExpireAt(LocalDateTime.now().minusHours(1));
        return record;
    }
    private Idempotency buildCompletedRecord(){
        Idempotency record=new Idempotency();
        record.setId(1L);
        record.setIdempotencyKey(IDEMPOTENCY_KEY);
        record.setStatusCode(201);
        record.setStatus(Idempotency.Status.COMPLETED);
        record.setResponsePayload(RESPONSE_PAYLOAD);
        record.setRequestPayloadHash(FINGERPRINT);
        record.setExpireAt(LocalDateTime.now().plusHours(1));
        return record;
    }
}
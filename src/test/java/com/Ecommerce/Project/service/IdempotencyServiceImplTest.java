package com.Ecommerce.Project.service;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.exception.IdempotencyJsonProcessingFailureException;
import com.Ecommerce.Project.exception.IdempotencyRetryFailedException;
import com.Ecommerce.Project.infrastructure.hashing.HashService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {
    private static final String IDEMPOTENCY_KEY="KEY-12345";
    private static final String FINGERPRINT = "POST:/orders:hash123";
    private static final int USER_ID =1;
    private static final String MAPPED_OBJECT ="obj123";
    private static final String HASHED_OBJECT ="hash123";
    private static final String HASHED_Diff_OBJECT ="hash123";
    private static final String RESPONSE_PAYLOAD="payload";

    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private IdempotencyPersistenceService idempotencyPersistenceService;
    @Mock
    private HashService hashService;
    @InjectMocks
    private IdempotencyServiceImpl idempotencyServiceImpl;

    Supplier<ResponseEntity<Order>> supplier;
    HttpServletRequest servletRequest;
    @BeforeEach
    void setup() {
        supplier = mock(Supplier.class);
        servletRequest = mock(HttpServletRequest.class);

        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getRequestURI()).thenReturn("/orders");
    }

    @Test
    public void shouldThrowExceptionWhenReplayDeserializationFails() throws JsonProcessingException {
        Idempotency completedIdempotency=buildCompletedRecord();

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(eq(FINGERPRINT),eq(IDEMPOTENCY_KEY),eq(USER_ID),any(LocalDateTime.class))).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already Existing Record"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(completedIdempotency));
        when(objectMapper.readValue(RESPONSE_PAYLOAD,Order.class)).thenThrow(new JsonMappingException("Exception Occurred While Processing Data"));

        IdempotencyJsonProcessingFailureException ex= assertThrows(IdempotencyJsonProcessingFailureException.class,()->idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY, USER_ID, new OrderDTO(), supplier, Order.class, servletRequest));

        assertEquals(IDEMPOTENCY_KEY,ex.getIdempotencyKey());
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(anyInt(),anyString());
        verify(supplier,never()).get();
        verify(objectMapper).readValue(RESPONSE_PAYLOAD, Order.class);
        verify(idempotencyPersistenceService,never()).markSuccess(any(),anyInt(),any());
        verify(idempotencyPersistenceService,never()).markFailure(any());
    }

    @Test
    public void shouldMarkFailureWhenBusinessLogicThrowsDuringRetry() throws JsonProcessingException {
        Idempotency idempotency=buildFailedResponse();
        Idempotency inProgressIdempotency=buildInProgressRecord();

        when(supplier.get()).thenThrow(new RuntimeException("UnHandled Exception Occurred While Executing Business Logic After Acquiring Record For Retry"));
        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any())).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already Existing Record"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));
        when(idempotencyPersistenceService.tryAcquireRowForRetry(anyLong(),any(),any())).thenReturn(true);
        when(idempotencyRepository.findById(anyLong())).thenReturn(Optional.of(inProgressIdempotency));

        IdempotencyRetryFailedException ex=assertThrows(IdempotencyRetryFailedException.class,()->idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY, USER_ID, new OrderDTO(), supplier, Order.class, servletRequest));

        verify(idempotencyPersistenceService,times(1)).markFailure(inProgressIdempotency);
        verify(supplier).get();
        verify(idempotencyPersistenceService,never()).markSuccess(any(),anyInt(),any());
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        verify(idempotencyRepository).findById(anyLong());
        assertEquals(IDEMPOTENCY_KEY,ex.getIdempotencyKey());
    }

    @Test
    public void shouldReturnInProgressWhenRetryLockAcquisitionFailsAndAnotherTransactionIsProcessing() throws JsonProcessingException {
        Idempotency idempotency=buildFailedResponse();
        Idempotency inProgressIdempotency=buildInProgressRecord();

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any())).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already Existing Record"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency))
                        .thenReturn(Optional.of(inProgressIdempotency));
        when(idempotencyPersistenceService.tryAcquireRowForRetry(anyLong(),any(),any())).thenReturn(false);

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID,new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier,never()).get();
        verify(idempotencyRepository,times(2)).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        verify(idempotencyPersistenceService).markInProgress(eq(FINGERPRINT),eq(IDEMPOTENCY_KEY),eq(USER_ID),any(LocalDateTime.class));
        verify(objectMapper,never()).readValue(RESPONSE_PAYLOAD,Order.class);
        verify(idempotencyPersistenceService,never()).markSuccess( any(),anyInt(),any());
        verify(idempotencyPersistenceService).tryAcquireRowForRetry(anyLong(),any(),any());
        assertNotNull(response);
        assertNull(response.response);
        assertEquals(202,response.statusCode);
        assertEquals(Idempotency.Status.IN_PROGRESS,response.status);
    }

    @Test
    public void shouldReturnFailedWhenRetryLockAcquisitionFailsAndLatestStatusIsFailed() throws JsonProcessingException {
        Idempotency idempotency=buildFailedResponse();

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any())).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already Existing Record"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));
        when(idempotencyPersistenceService.tryAcquireRowForRetry(anyLong(),any(),any())).thenReturn(false);

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID,new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier,never()).get();
        verify(idempotencyRepository,times(2)).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        verify(idempotencyPersistenceService).markInProgress(eq(FINGERPRINT),eq(IDEMPOTENCY_KEY),eq(USER_ID),any(LocalDateTime.class));
        verify(objectMapper,never()).readValue(RESPONSE_PAYLOAD,Order.class);
        verify(idempotencyPersistenceService,never()).markSuccess( any(),anyInt(),any());
        verify(idempotencyPersistenceService).tryAcquireRowForRetry(anyLong(),any(),any());
        assertNotNull(response);
        assertNull(response.response);
        assertEquals(Idempotency.Status.FAILED,response.status);
    }

    @Test
    public void shouldReturnCompletedWhenRetryLockAcquisitionFailsButAnotherTransactionCompleted() throws JsonProcessingException {
        Idempotency idempotency=buildFailedResponse();
        Idempotency completedIdempotencyRecord=buildCompletedRecord();

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any())).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already Existing Record"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency))
                        .thenReturn(Optional.of(completedIdempotencyRecord));
        when(idempotencyPersistenceService.tryAcquireRowForRetry(anyLong(),any(),any())).thenReturn(false);
        when(objectMapper.readValue(RESPONSE_PAYLOAD, Order.class)).thenReturn(new Order());

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID,new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier,never()).get();
        verify(idempotencyRepository,times(2)).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        verify(idempotencyPersistenceService).markInProgress(eq(FINGERPRINT),eq(IDEMPOTENCY_KEY),eq(USER_ID),any(LocalDateTime.class));
        verify(objectMapper).readValue(RESPONSE_PAYLOAD,Order.class);
        verify(idempotencyPersistenceService,never()).markSuccess( any(),eq(201),any());
        verify(idempotencyPersistenceService).tryAcquireRowForRetry(anyLong(),any(),any());
        assertNotNull(response);
        assertEquals(200,response.statusCode);
        assertEquals(Idempotency.Status.COMPLETED,response.status);
    }

    @Test
    public void shouldRetryExpiredFailedRecord() throws JsonProcessingException {
        Idempotency idempotency=buildFailedResponse();
        Idempotency idempotency_record_for_retry=new Idempotency();
        idempotency_record_for_retry.setIdempotencyKey(IDEMPOTENCY_KEY);
        idempotency_record_for_retry.setUpdatedAt(LocalDateTime.now());
        idempotency_record_for_retry.setExpireAt(LocalDateTime.now().plusHours(1));
        idempotency_record_for_retry.setStatus(Idempotency.Status.IN_PROGRESS);
        idempotency_record_for_retry.setRequestPayloadHash(FINGERPRINT);

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any(LocalDateTime.class))).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already ExistingRecord"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));
        when(idempotencyPersistenceService.tryAcquireRowForRetry(anyLong(),any(LocalDateTime.class),any(LocalDateTime.class))).thenReturn(true);
        when(idempotencyRepository.findById(anyLong())).thenReturn(Optional.of(idempotency_record_for_retry));
        when(supplier.get())
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(new Order()));

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID,new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier).get();
        verify(idempotencyPersistenceService).tryAcquireRowForRetry(anyLong(),any(LocalDateTime.class),any(LocalDateTime.class));
        verify(idempotencyRepository).findById(anyLong());
        verify(idempotencyPersistenceService).markSuccess(any(),eq(201),any());
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        assertEquals(Idempotency.Status.COMPLETED,response.status);
        assertEquals(201,response.statusCode);
    }

    @Test
    public void shouldReturnInProgressWhenRequestAlreadyProcessing() throws JsonProcessingException {
        Idempotency idempotency=buildInProgressRecord();

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any(LocalDateTime.class))).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict with Already ExistingRecord"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID, new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier,never()).get();
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        assertEquals(Idempotency.Status.IN_PROGRESS,response.status);
        assertEquals(202,response.statusCode);
        assertEquals("Request Is In Progress",response.message);
        verify(objectMapper,never()).readValue(anyString(),eq(Order.class));

    }
    @Test
    public void shouldReturnConflictWhenFingerprintMismatch() throws JsonProcessingException {
        Idempotency idempotency=new Idempotency();
        idempotency.setRequestPayloadHash(HASHED_OBJECT);

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_Diff_OBJECT);
        when(idempotencyPersistenceService.markInProgress(any(),eq(IDEMPOTENCY_KEY),eq(USER_ID),any(LocalDateTime.class))).thenThrow(new DataIntegrityViolationException("Idempotency Key is in Conflict"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID, new OrderDTO(),supplier,Order.class,servletRequest);

        verify(supplier,never()).get();
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(anyInt(),anyString());
        verify(objectMapper,never()).readValue(anyString(),eq(Order.class));
        assertEquals(409,response.statusCode);
        assertEquals(Idempotency.Status.FAILED,response.status);
    }

    @Test
    public void shouldReplayResponseWhenRequestAlreadyCompleted() throws JsonProcessingException {
        Order order=new Order();
        Idempotency idempotency=buildCompletedRecord();
        idempotency.setResponsePayload(RESPONSE_PAYLOAD);
        idempotency.setRequestPayloadHash(FINGERPRINT);
        idempotency.setExpireAt(LocalDateTime.now().plusHours(1));

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(any(),eq(IDEMPOTENCY_KEY),eq(1),any(LocalDateTime.class))).thenThrow(new DataIntegrityViolationException("Idempotency key is in conflict"));
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY)).thenReturn(Optional.of(idempotency));
        when(objectMapper.readValue(RESPONSE_PAYLOAD,Order.class)).thenReturn(order);

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,USER_ID, new OrderDTO(),supplier,Order.class,servletRequest);

        assertEquals(201,response.statusCode);
        assertEquals(Idempotency.Status.COMPLETED,response.status);
        assertEquals(order,response.response);
        verify(idempotencyPersistenceService,never()).markSuccess(any(),anyInt(),any());
        verify(idempotencyPersistenceService,never()).markFailure(any());
        verify(supplier,never()).get();
        verify(idempotencyRepository).findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        verify(idempotencyPersistenceService).markInProgress(anyString(),anyString(),anyInt(),any());
        verify(objectMapper).readValue(RESPONSE_PAYLOAD,Order.class);
    }

    @Test
    public void shouldProcessRequestSuccessfullyWhenNoExistingIdempotencyRecordExists() throws JsonProcessingException {
        Order order=new Order();
        OrderDTO orderDTO=new OrderDTO();
        orderDTO.setUserId(USER_ID);
        ArgumentCaptor<String> fingerPrintCaptor=ArgumentCaptor.forClass(String.class);

        when(objectMapper.writeValueAsString(any())).thenReturn(MAPPED_OBJECT);
        when(hashService.generateHash(any())).thenReturn(HASHED_OBJECT);
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any(LocalDateTime.class))).thenReturn(buildInProgressRecord());
        when(supplier.get()).thenReturn(ResponseEntity.ok(order));
        doNothing().when(idempotencyPersistenceService).markSuccess(any(),anyInt(),any());

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY, USER_ID,orderDTO,supplier,Order.class,servletRequest);

        assertNotNull(response);
        assertEquals(Idempotency.Status.COMPLETED, response.status);
        assertEquals(200,response.statusCode);
        assertEquals(order,response.response);
        verify(idempotencyPersistenceService).markInProgress(any(),anyString(),anyInt(),any(LocalDateTime.class));
        verify(idempotencyPersistenceService).markSuccess(any(),anyInt(),any());
        verify(hashService).generateHash(MAPPED_OBJECT);
        verify(objectMapper).writeValueAsString(any());
        verify(supplier).get();
        verify(idempotencyRepository,never()).findByUserIdAndIdempotencyKey(anyInt(),anyString());
        verify(idempotencyPersistenceService).markInProgress(fingerPrintCaptor.capture(),eq(IDEMPOTENCY_KEY),anyInt(),any(LocalDateTime.class));
        assertEquals(FINGERPRINT,fingerPrintCaptor.getValue());
    }

    private Idempotency buildCompletedResponsePayload(){
        Idempotency record=new Idempotency();
        record.setResponsePayload("{\"id\":1}");
        return record;
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
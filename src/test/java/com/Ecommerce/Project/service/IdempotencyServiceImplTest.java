package com.Ecommerce.Project.service;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.infrastructure.hashing.HashService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {
    private static final String IDEMPOTENCY_KEY="KEY-12345";
    private static final String FINGERPRINT = "POST:/orders:hash123";

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

    @Test
    public void shouldProcessRequestSuccessfullyWhenNoExistingIdempotencyRecordExists() throws JsonProcessingException {
        Order order=new Order();
        Supplier<ResponseEntity<Order>> supplier=mock(Supplier.class);
        OrderDTO orderDTO=new OrderDTO();
        orderDTO.setUserId(1);
        ArgumentCaptor<String> fingerPrintCaptor=ArgumentCaptor.forClass(String.class);

        HttpServletRequest servletRequest=mock(HttpServletRequest.class);
        when(objectMapper.writeValueAsString(any())).thenReturn("obj123");
        when(hashService.generateHash(any())).thenReturn("hash123");
        when(idempotencyPersistenceService.markInProgress(anyString(),anyString(),anyInt(),any(LocalDateTime.class))).thenReturn(buildInProgressRecord());
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getRequestURI()).thenReturn("/orders");
        when(supplier.get()).thenReturn(ResponseEntity.ok(order));
        doNothing().when(idempotencyPersistenceService).markSuccess(any(),anyInt(),any());

        IdempotencyAPIResponse<Order> response=idempotencyServiceImpl.handleIdempotentRequest(IDEMPOTENCY_KEY,1,orderDTO,supplier,Order.class,servletRequest);

        assertNotNull(response);
        assertEquals(Idempotency.Status.COMPLETED, response.success);
        assertEquals(200,response.statusCode);
        assertEquals(order,response.response);
        verify(idempotencyPersistenceService).markInProgress(any(),anyString(),anyInt(),any(LocalDateTime.class));
        verify(idempotencyPersistenceService).markSuccess(any(),anyInt(),any());
        verify(hashService).generateHash("obj123");
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
        return record;
    }
    private Idempotency buildFailedResponse(){
        Idempotency record=new Idempotency();
        record.setId(1L);
        record.setIdempotencyKey(IDEMPOTENCY_KEY);
        record.setStatusCode(500);
        record.setStatus(Idempotency.Status.FAILED);
        return record;
    }
    private Idempotency buildCompletedRecord(){
        Idempotency record=new Idempotency();
        record.setId(1L);
        record.setIdempotencyKey(IDEMPOTENCY_KEY);
        record.setStatusCode(201);
        record.setStatus(Idempotency.Status.COMPLETED);
        return record;
    }
}
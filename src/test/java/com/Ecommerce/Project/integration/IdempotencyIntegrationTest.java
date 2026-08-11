package com.Ecommerce.Project.integration;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import com.Ecommerce.Project.DAO.IdempotencyRepository;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Idempotency;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import com.Ecommerce.Project.config.AbstractIntegrationTest;
import com.Ecommerce.Project.exception.IdempotencyRecordNotFoundException;
import com.Ecommerce.Project.infrastructure.hashing.HashService;
import com.Ecommerce.Project.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;



@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private IdempotencyRepository idempotencyRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HashService hashService;

    private final String IDEMPOTENCY_KEY="key-123";
    private final int USER_ID=1;

    @BeforeEach
    void cleanDatabase() {
        idempotencyRepository.deleteAll();
        idempotencyRepository.flush();
    }

    @Test
    public void shouldExecuteBusinessLogicOnlyOnceForConcurrentRequests() throws Exception {

        OrderDTO orderDTO = buildOrderDTO();
        Order order = buildOrder();
        MockHttpServletRequest servletRequest = buildServletRequest();

        AtomicInteger executionCount = new AtomicInteger(0);

        Supplier<ResponseEntity<Order>> supplier = () -> {
            executionCount.incrementAndGet();
            return ResponseEntity.status(201).body(order);
        };

        final int THREAD_COUNT = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<IdempotencyAPIResponse<Order>> responses = new ConcurrentLinkedQueue<>();

        AtomicReference<IdempotencyAPIResponse<Order>> successResponse =
                new AtomicReference<>();

        try {

            for (int i = 0; i < THREAD_COUNT; i++) {

                executorService.submit(() -> {

                    try {

                        startLatch.await();

                        IdempotencyAPIResponse<Order> response =
                                idempotencyService.handleIdempotentRequest(
                                        IDEMPOTENCY_KEY,
                                        USER_ID,
                                        orderDTO,
                                        supplier,
                                        Order.class,
                                        servletRequest);

                        responses.add(response);

                        if (response.status == Idempotency.Status.COMPLETED) {
                            successResponse.compareAndSet(null, response);
                        }

                    } catch (Exception e) {

                        exceptions.add(e);

                    } finally {

                        doneLatch.countDown();

                    }

                });
            }

            // Release all workers together
            startLatch.countDown();

            // Wait until every worker finishes
            doneLatch.await();

        } finally {

            executorService.shutdown();

        }

        // -----------------------
        // Assertions
        // -----------------------

        assertTrue(exceptions.isEmpty(),
                "No worker thread should throw an exception");

        assertEquals(
                THREAD_COUNT,
                responses.size() + exceptions.size(),
                "Every submitted request should complete");

        assertEquals(
                1,
                executionCount.get(),
                "Business logic should execute only once");

        assertEquals(
                1,
                idempotencyRepository.count(),
                "Only one idempotency record should exist");

        Idempotency persisted =
                idempotencyRepository
                        .findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)
                        .orElseThrow(() ->
                                new IdempotencyRecordNotFoundException(
                                        "Idempotency Record Not Found",
                                        null,
                                        null));

        assertEquals(
                Idempotency.Status.COMPLETED,
                persisted.getStatus());

        assertNotNull(
                persisted.getResponsePayload());

        assertNotNull(
                persisted.getRequestPayloadHash());

        IdempotencyAPIResponse<Order> completedResponse =
                successResponse.get();

        assertNotNull(
                completedResponse,
                "One successful response should be captured");

        assertEquals(
                Idempotency.Status.COMPLETED,
                completedResponse.status);

        assertEquals(
                completedResponse.statusCode,
                persisted.getStatusCode());

        Order persistedOrder =
                objectMapper.readValue(
                        persisted.getResponsePayload(),
                        Order.class);

        assertEquals(
                completedResponse.response.getId(),
                persistedOrder.getId());

        assertEquals(
                completedResponse.response.getQuantity(),
                persistedOrder.getQuantity());

        assertEquals(
                completedResponse.response.getTotalPrice(),
                persistedOrder.getTotalPrice());

        assertEquals(
                completedResponse.response.getStatus(),
                persistedOrder.getStatus());
    }

    @Test
    public void testShouldReturnInProgressForInProgressRecord() throws JsonProcessingException {
        OrderDTO orderDTO=buildOrderDTO();
        Order order=buildOrder();

        Idempotency inProgressIdempotency= buildInProgressIdempotencyRecord(orderDTO);
        idempotencyRepository.save(inProgressIdempotency);

        AtomicInteger atomicInteger=new AtomicInteger(0);

        Supplier<ResponseEntity<Order>> supplier=()->{
            atomicInteger.incrementAndGet();
            return ResponseEntity.status(201).body(order);
        };

        MockHttpServletRequest servletRequest=buildServletRequest();

        IdempotencyAPIResponse<Order> response=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,supplier,Order.class,servletRequest);

        Optional<Idempotency> persistedIdempotencyOptional=idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);
        assertTrue(persistedIdempotencyOptional.isPresent());
        Idempotency persistedIdempotency=persistedIdempotencyOptional.get();

        assertEquals(Idempotency.Status.IN_PROGRESS,response.status);
        assertEquals(0,atomicInteger.get());
        assertEquals(Idempotency.Status.IN_PROGRESS,persistedIdempotency.getStatus());
        assertEquals(inProgressIdempotency.getRequestPayloadHash(),persistedIdempotency.getRequestPayloadHash());
        assertEquals(1,idempotencyRepository.count());
    }

    @Test
    public void testShouldAcquireExpiredInProgressRow() throws JsonProcessingException {
        OrderDTO orderDTO=buildOrderDTO();
        Order order=buildOrder();

        Idempotency expiredInProgressIdempotency= buildExpiredInProgressIdempotencyRecord(orderDTO);
        idempotencyRepository.save(expiredInProgressIdempotency);

        AtomicInteger atomicInteger=new AtomicInteger(0);

        Supplier<ResponseEntity<Order>> supplier=()->{
            atomicInteger.incrementAndGet();
            return ResponseEntity.status(201).body(order);
        };

        MockHttpServletRequest servletRequest=buildServletRequest();

        IdempotencyAPIResponse<Order> response=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,supplier,Order.class,servletRequest);
        Optional<Idempotency> getIdempotencyRecord=idempotencyRepository.findByUserIdAndIdempotencyKey(USER_ID,IDEMPOTENCY_KEY);

        assertTrue(getIdempotencyRecord.isPresent());

        Idempotency updatedIdempotencyRecord=getIdempotencyRecord.get();

        assertEquals(Idempotency.Status.COMPLETED,response.status);
        assertEquals(201,response.statusCode);
        assertNotEquals(expiredInProgressIdempotency.getExpireAt(),updatedIdempotencyRecord.getExpireAt());
        assertNotEquals(expiredInProgressIdempotency.getUpdatedAt(),updatedIdempotencyRecord.getUpdatedAt());
        assertEquals(expiredInProgressIdempotency.getRequestPayloadHash(),updatedIdempotencyRecord.getRequestPayloadHash());
        assertEquals(1,atomicInteger.get());
        assertEquals(Idempotency.Status.COMPLETED,updatedIdempotencyRecord.getStatus());
    }

    @Test
    public void testShouldRejectTheRequestWithSameKeyDifferentPayload(){
        OrderDTO orderDTO=buildOrderDTO();
        OrderDTO orderDTOWithDifferentPayload=buildOrderDTOWithDifferntPaylad();
        Order order=buildOrder();

        AtomicInteger countBusinessExecutionCount=new AtomicInteger(0);

        Supplier<ResponseEntity<Order>> supplier=()->{
            countBusinessExecutionCount.incrementAndGet();
           return ResponseEntity.status(201).body(order);
        };
        MockHttpServletRequest servletRequest=new MockHttpServletRequest();
        servletRequest.setRequestURI("/order");
        servletRequest.setMethod("POST");
        IdempotencyAPIResponse<Order> response=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,supplier,Order.class,servletRequest);
        //getting the idempotency record before sending conflict request
        Optional<Idempotency> beforeConflictIdempotency=idempotencyRepository.findByUserIdAndIdempotencyKey(orderDTO.getUserId(),IDEMPOTENCY_KEY);

        //sending conflict request with same idempotency key and different payload
        IdempotencyAPIResponse<Order> conflictResponse=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTOWithDifferentPayload,supplier,Order.class,servletRequest);

        //getting the idempotency record after sending conflict request
        Optional<Idempotency> afterConflictIdempotency =idempotencyRepository.findByUserIdAndIdempotencyKey(orderDTO.getUserId(),IDEMPOTENCY_KEY);

        //getting no of records present in db
        long count=idempotencyRepository.count();
        //assert to make sure only oe record present
        assertEquals(1L,count);

        //assert to check we request is rejected as conflict
        assertEquals(Idempotency.Status.FAILED,conflictResponse.status);
        assertEquals(409,conflictResponse.statusCode);
        assertEquals(1,countBusinessExecutionCount.get());

        //assert beforeConflictIdempotency to make sure record is present
        assertTrue(beforeConflictIdempotency.isPresent(),"beforeConflictIdempotency Should Present");


        //assert afterConflictIdempotency to make sure record is present
        assertTrue(afterConflictIdempotency.isPresent(),"afterConflictIdempotency Should Present");

        // extracting idempotency from before and after
        Idempotency before=beforeConflictIdempotency.get();
        Idempotency after=afterConflictIdempotency.get();
        //assert to check no accidental changes happened to original data
        assertEquals(before.getIdempotencyKey(),after.getIdempotencyKey());
        assertEquals(before.getId(),after.getId());
        assertEquals(before.getStatus(),after.getStatus());
        assertEquals(before.getUpdatedAt(),after.getUpdatedAt());
        assertEquals(before.getStatusCode(),after.getStatusCode());
        assertEquals(before.getExpireAt(),after.getExpireAt());
        assertEquals(before.getResponsePayload(),after.getResponsePayload());
        assertEquals(before.getRequestPayloadHash(),after.getRequestPayloadHash());
    }

    @Test
    public void testShouldReplayResponseWhenRequestAlreadyProcessed() throws JsonProcessingException {
        OrderDTO orderDTO=buildOrderDTO();
        Order order=buildOrder();
        MockHttpServletRequest servletRequest=new MockHttpServletRequest();
        servletRequest.setMethod("POST");
        servletRequest.setRequestURI("/order");

        AtomicInteger countBusinessExecutionCount=new AtomicInteger(0);

        Supplier<ResponseEntity<Order>> businessLogic=()->{
            countBusinessExecutionCount.incrementAndGet();
            return ResponseEntity.status(201).body(order);
        };

        IdempotencyAPIResponse<Order> response=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,businessLogic, Order.class,servletRequest);

        //calling second time
        IdempotencyAPIResponse<Order> replayedResponse=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,businessLogic, Order.class,servletRequest);


        //extracting the order from response
        Order originalRecord=response.response;

        Optional<Idempotency> persistedIdempotencyRecord=idempotencyRepository.findByUserIdAndIdempotencyKey(orderDTO.getUserId(),IDEMPOTENCY_KEY);
        long noOfRecords=idempotencyRepository.count();

        assertNotNull(originalRecord,"ordered Record Should be Present");
        assertTrue(persistedIdempotencyRecord.isPresent(),"Idempotency record should be persisted");
        assertEquals(Idempotency.Status.COMPLETED,persistedIdempotencyRecord.get().getStatus());
        assertEquals(1,noOfRecords,"Exactly one idempotency record should be persisted");

        assertEquals(response.status, replayedResponse.status);
        assertEquals(response.statusCode, replayedResponse.statusCode);

        //extracting replayed response
        Order replayedRecord=replayedResponse.response;
        //assert to check both second record was just replayed
        assertNotNull(replayedRecord,"Replayed Order Shouldn't be Null");
        assertEquals(originalRecord.getQuantity(),replayedRecord.getQuantity());
        assertEquals(originalRecord.getStatus(),replayedRecord.getStatus());
        assertEquals(originalRecord.getId(),replayedRecord.getId());
        assertEquals(originalRecord.getLocalDateTime(),replayedRecord.getLocalDateTime());
        assertEquals(originalRecord.getTotalPrice(),replayedRecord.getTotalPrice());

        //asserting to ensure business logic is executed only once
        assertEquals(1,countBusinessExecutionCount.get());
    }

    @Test
    public void testHandleIdempotencyForSuccess() throws JsonProcessingException {
        OrderDTO orderDTO=buildOrderDTO();
        Order order=buildOrder();
        MockHttpServletRequest servletRequest=new MockHttpServletRequest();
        servletRequest.setMethod("POST");
        servletRequest.setRequestURI("/order");
        Supplier<ResponseEntity<Order>> businessLogic=()->ResponseEntity.status(201).body(order);

        IdempotencyAPIResponse<Order> response=idempotencyService.handleIdempotentRequest(IDEMPOTENCY_KEY,orderDTO.getUserId(),orderDTO,businessLogic, Order.class,servletRequest);
        //extracting the order from response
        Order orderedRecord=response.response;

        Optional<Idempotency> persistedIdempotencyRecord=idempotencyRepository.findByUserIdAndIdempotencyKey(orderDTO.getUserId(),IDEMPOTENCY_KEY);
        long noOfRecords=idempotencyRepository.count();

        assertTrue(persistedIdempotencyRecord.isPresent(),"Idempotency record should be persisted");
        String persistedResponsePayload=persistedIdempotencyRecord.get().getResponsePayload();
        Order persistedOrderPayload=objectMapper.readValue(persistedResponsePayload,Order.class);

        assertEquals(Idempotency.Status.COMPLETED,response.status);
        assertEquals(201,response.statusCode);

        assertEquals(orderDTO.getQuantity(),orderedRecord.getQuantity());
        assertEquals(orderDTO.getTotalPrice(),orderedRecord.getTotalPrice());
        assertEquals(Order.Status.PLACED,orderedRecord.getStatus());

        assertEquals(1,noOfRecords,"Exactly one idempotency record should be persisted");

        assertEquals(orderDTO.getUserId(),persistedIdempotencyRecord.get().getUserId());
        assertEquals(IDEMPOTENCY_KEY,persistedIdempotencyRecord.get().getIdempotencyKey());
        assertNotNull(persistedIdempotencyRecord.get().getUpdatedAt());
        assertNotNull(persistedIdempotencyRecord.get().getCreatedAt());
        assertTrue(persistedIdempotencyRecord.get().getExpireAt().isAfter(persistedIdempotencyRecord.get().getUpdatedAt()));
        assertEquals(Idempotency.Status.COMPLETED,persistedIdempotencyRecord.get().getStatus());

        assertEquals(response.status,persistedIdempotencyRecord.get().getStatus());
        assertEquals(response.statusCode,persistedIdempotencyRecord.get().getStatusCode());

        assertEquals(order.getId(), persistedOrderPayload.getId());
        assertEquals(order.getQuantity(), persistedOrderPayload.getQuantity());
        assertEquals(order.getTotalPrice(), persistedOrderPayload.getTotalPrice());
        assertEquals(Order.Status.PLACED, persistedOrderPayload.getStatus());
}

    public OrderDTO buildOrderDTO(){
        OrderDTO orderDTO=new OrderDTO();
        orderDTO.setUserId(1);
        orderDTO.setProductId(100);
        orderDTO.setQuantity(1);
        orderDTO.setTotalPrice(100000);
        return orderDTO;
    }

    public Order buildOrder(){
        Order order=new Order();
        Product product=new Product();
        User user=new User();
        user.setId(USER_ID);
        product.setId(2);
        order.setStatus(Order.Status.PLACED);
        order.setId(1000);
        order.setQuantity(1);
        order.setTotalPrice(100000);
        order.setLocalDateTime(LocalDateTime.now().minusHours(1));
        order.setProduct(product);
        order.setUser(user);
        return order;
    }

    public OrderDTO buildOrderDTOWithDifferntPaylad(){
        OrderDTO orderDTO=new OrderDTO();
        orderDTO.setUserId(2);
        orderDTO.setProductId(200);
        orderDTO.setQuantity(2);
        orderDTO.setTotalPrice(200000);
        return orderDTO;
    }

    public Idempotency buildInProgressIdempotencyRecord(OrderDTO orderDTO) throws JsonProcessingException {
        String requestPayloadHash=buildRequestPayloadHash(orderDTO);
        Idempotency idempotency=new Idempotency();
        idempotency.setUserId(USER_ID);
        idempotency.setIdempotencyKey(IDEMPOTENCY_KEY);
        idempotency.setStatus(Idempotency.Status.IN_PROGRESS);
        idempotency.setRequestPayloadHash(requestPayloadHash);
        idempotency.setUpdatedAt(LocalDateTime.now().minusHours(6));
        idempotency.setCreatedAt(LocalDateTime.now().minusHours(6));
        idempotency.setExpireAt(LocalDateTime.now().plusHours(1));
        return idempotency;
    }

    public Idempotency buildExpiredInProgressIdempotencyRecord(OrderDTO orderDTO) throws JsonProcessingException {
        String requestPayloadHash=buildRequestPayloadHash(orderDTO);
        Idempotency idempotency=new Idempotency();
        idempotency.setUserId(USER_ID);
        idempotency.setIdempotencyKey(IDEMPOTENCY_KEY);
        idempotency.setStatus(Idempotency.Status.IN_PROGRESS);
        idempotency.setRequestPayloadHash(requestPayloadHash);
        idempotency.setUpdatedAt(LocalDateTime.now().minusHours(6));
        idempotency.setCreatedAt(LocalDateTime.now().minusHours(6));
        idempotency.setExpireAt(LocalDateTime.now().minusHours(1));
        return idempotency;
    }
    public String buildRequestPayloadHash(OrderDTO request) throws JsonProcessingException {
        MockHttpServletRequest servletRequest=buildServletRequest();
        String requestPayloadAsString = objectMapper.writeValueAsString(request);
        String hashedRequestPayload= hashService.generateHash(requestPayloadAsString);
        return generateFingerprint(hashedRequestPayload,servletRequest);
    }

    public String generateFingerprint(String hashPayload, HttpServletRequest servletRequest){
        return servletRequest.getMethod()+":"+servletRequest.getRequestURI()+":"+hashPayload;
    }
    public MockHttpServletRequest buildServletRequest(){
        MockHttpServletRequest servletRequest=new MockHttpServletRequest();
        servletRequest.setRequestURI("/order");
        servletRequest.setMethod("POST");
        return  servletRequest;
    }
}

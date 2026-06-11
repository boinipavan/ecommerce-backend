package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.APIResponse.IdempotencyAPIResponse;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.service.IdempotencyService;
import com.Ecommerce.Project.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OrderController {

    private OrderService orderService;

    private IdempotencyService idempotencyService;

    public OrderController(OrderService orderService,IdempotencyService idempotencyService) {

        this.orderService = orderService;
        this.idempotencyService=idempotencyService;
    }

    /* @PostMapping("/order")
    public ResponseEntity<?> saveOrder(@RequestBody List<OrderDTO> orderDTO)
    {
        try
        {
            orderService.saveOrder(orderDTO);
            return ResponseEntity.ok("order accepted");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
     */

    @PostMapping("/order")
    public IdempotencyAPIResponse placeOrder(@RequestHeader("idempotency-key") String idempotencyKey, @RequestBody OrderDTO orderDTO, HttpServletRequest servletRequest){
            log.info("order placement started");
            IdempotencyAPIResponse response=idempotencyService.handleIdempotentRequest(idempotencyKey,orderDTO.getUserId(),orderDTO,()->ResponseEntity.ok(orderService.placeOrder(orderDTO)), Order.class,servletRequest);
            log.info("order placed successfully");
            return response;
    }
    //in above method calling, after switching to JWT Authentication.get userid from security context holder
}

package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.service.OrderService;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private OrderService orderService;

    public OrderController(OrderService orderService) {

        this.orderService = orderService;
    }

    @PostMapping("/order")
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
    @PostMapping("/singleOrder")
    public ResponseEntity<String> placeOrder(@RequestBody OrderDTO orderDTO){
        try{
            orderService.placeOrder(orderDTO);
            return ResponseEntity.ok("Order Placed Successfully");
        } catch (OptimisticEntityLockException e) {
            throw new RuntimeException(e);
        }
    }
}

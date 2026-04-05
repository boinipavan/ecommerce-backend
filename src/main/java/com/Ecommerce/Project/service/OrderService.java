package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DTO.OrderDTO;

import java.util.List;

public interface OrderService {

    public void saveOrder(List<OrderDTO> orderDTO);
    public void deleteOrder(Integer id);
    public void placeOrder(OrderDTO orderDTO);
}

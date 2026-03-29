package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Order;

public interface OrderDAO {

    public void saveOrder(Order order);
    public OrderDAO getOrderByUserId(int id);
    public void deleteOrder(Integer id);
}

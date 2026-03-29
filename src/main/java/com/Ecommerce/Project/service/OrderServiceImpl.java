package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.OrderDAO;
import com.Ecommerce.Project.DAO.ProductDAO;
import com.Ecommerce.Project.DAO.UserDAO;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Order;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    private OrderDAO orderDAO;
    private UserDAO userDAO;
    private ProductDAO productDAO;

    @Autowired
    public OrderServiceImpl(OrderDAO orderDAO,UserDAO userDAO,ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.userDAO=userDAO;
        this.productDAO=productDAO;
    }

    @Override
    public void saveOrder(List<OrderDTO> orderDTO) {
        for(OrderDTO tempOrderDTO :orderDTO)
        {
            int price=productDAO.getProductPrice(tempOrderDTO.productId);
            if(price*tempOrderDTO.quantity!=tempOrderDTO.totalPrice)
            {
                throw new RuntimeException("phishing behavior detected request rejected");
            }
        }
        for(OrderDTO tempOrderDTO:orderDTO) {
            Order order = new Order();
            User user = userDAO.getUser(tempOrderDTO.getUserId());
            Product product = productDAO.getProductById(tempOrderDTO.productId);
            order.setUser(user);
            order.setProduct(product);
            order.setQuantity(tempOrderDTO.getQuantity());
            order.setTotalPrice(tempOrderDTO.getTotalPrice());
            order.setLocalDateTime(LocalDateTime.now());
            order.setStatus(Order.Status.PLACED);
            orderDAO.saveOrder(order);
        }
    }

    @Override
    @Transactional
    public void deleteOrder(Integer id) {
        orderDAO.deleteOrder(id);
    }
}

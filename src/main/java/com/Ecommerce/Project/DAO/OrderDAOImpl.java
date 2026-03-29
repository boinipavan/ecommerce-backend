package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderDAOImpl implements OrderDAO {

    private EntityManager entityManager;

    public OrderDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void saveOrder(Order order) {
        entityManager.persist(order);
    }

    @Override
    public OrderDAO getOrderByUserId(int id) {
        return null;
    }

    @Override
    public void deleteOrder(Integer id) {
        entityManager.createQuery("delete from Order where product.id= :id")
                .setParameter("id",id)
                .executeUpdate();
    }
}

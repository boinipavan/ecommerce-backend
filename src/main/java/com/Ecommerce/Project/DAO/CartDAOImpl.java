package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.Entity.Cart;
import com.Ecommerce.Project.Entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartDAOImpl implements CartDAO{

    private final EntityManager entityManager;

    public CartDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void addProductToCart(Cart cart) {
         entityManager.persist(cart);
    }

    /*
    @Override
    public List<Cart> getCartItems(int userId) {
        return entityManager.createQuery("select c from Cart c where c.user.id= :userId")
                .setParameter("userId",userId)
                .getResultList();
    }
    */


    @Override
    @Transactional
    public void deleteCartItem(Cart cart) {
        entityManager.remove(cart);
    }

    @Override
    public Cart findCartByUserIdAndProductId(int userId, int productId) {
        List<Cart> cart= entityManager.createQuery("select c from Cart c where c.user.id= :userId and c.product.id= :productId")
                .setParameter("userId",userId)
                .setParameter("productId",productId)
                .getResultList();
        return cart.isEmpty()?null:cart.get(0);
    }

    @Override
    public List<Product> getProductsInCartByUserId(int userId) {
        return entityManager.createQuery(
                        "SELECT c.product FROM Cart c WHERE c.user.id = :userId")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Transactional
    @Override
    public void deleteCartItemUsingProductId(Integer id) {
        System.out.println("cart dao called, In Product DAO to delete product");
        entityManager.createQuery("delete from Cart where product.id= :productId")
                .setParameter("productId",id)
                .executeUpdate();
        entityManager.flush();//commiting changes into db immediatey
    }

}

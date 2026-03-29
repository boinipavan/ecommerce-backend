package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.Entity.Cart;
import com.Ecommerce.Project.Entity.Product;

import java.util.List;

public interface CartDAO {

    public void addProductToCart(Cart cart);
   // public List<Cart> getCartItems(int userId);
    public void deleteCartItem(Cart cart);
    public Cart findCartByUserIdAndProductId(int userId, int productId);
    public List<Product> getProductsInCartByUserId(int userId);
    public void deleteCartItemUsingProductId(Integer id);
}

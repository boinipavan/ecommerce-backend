package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DTO.CartDTO;
import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.Entity.Cart;
import com.Ecommerce.Project.Entity.Product;

import java.util.List;


public interface CartService {

    public void addProductToCart(CartDTO cartDTO);
    //public List<Cart> getCartItems(int userId);
    public void deleteCartItem(CartDTO cartDTO);
    public List<ProductDTO> getProductsFromCartUsingUserId(int userId);
    public void deleteCartItemUsingProductId(Integer id);
}

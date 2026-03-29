package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.CartDAO;
import com.Ecommerce.Project.DAO.ProductDAO;
import com.Ecommerce.Project.DAO.UserDAO;
import com.Ecommerce.Project.DTO.CartDTO;
import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.Entity.Cart;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CartServiceImpl implements CartService{

    public CartDAO cartDAO;
    public UserDAO userDAO;
    public ProductDAO productDAO;


    public CartServiceImpl(CartDAO cartDAO, UserDAO userDAO, ProductDAO productDAO) {
        this.cartDAO = cartDAO;
        this.userDAO = userDAO;
        this.productDAO = productDAO;
    }

    @Override
    public void addProductToCart(CartDTO cartDTO) {
        User user=userDAO.getUser(cartDTO.userId);
        Product product=productDAO.getProductById(cartDTO.productId);
        Cart cart=new Cart(user,product);
        cartDAO.addProductToCart(cart);
    }

  /*
   @Override
    public List<Cart> getCartItems(int userId) {
        return cartDAO.getCartItems(userId);
    }
     */

    @Override
    public void deleteCartItem(CartDTO cartDTO)  {

            Cart cart=cartDAO.findCartByUserIdAndProductId(cartDTO.userId,cartDTO.productId);
            if(cart==null)
            {
                throw new RuntimeException("Cart Item Not Found");
            }
            cartDAO.deleteCartItem(cart);

    }

    @Override
    public List<ProductDTO> getProductsFromCartUsingUserId(int userId) {
        List<Product> products= cartDAO.getProductsInCartByUserId(userId);
        List<ProductDTO> productDTO=new ArrayList<>();
        for(int i=0;i<products.size();i++)
        {
            ProductDTO productDTO1=new ProductDTO();
            Product product=products.get(i);
            productDTO1.setProductId(product.getId());
            productDTO1.setTitle(product.getTitle());
            productDTO1.setCategory(product.getCategory());
            productDTO1.setImageData(product.getImageData());
            productDTO1.setImageName(product.getImageName());
            productDTO1.setImageType(product.getImageType());
            productDTO1.setAverageRating(product.getAverageRating());
            productDTO1.setPrice(product.getPrice());
            productDTO1.setTotalReviews(product.getTotalReviews());
            productDTO.add(productDTO1);
        }
        return productDTO;
    }

    @Override
    public void deleteCartItemUsingProductId(Integer id) {
            System.out.println("called me i am cartservice impl which further call cart dao");
            cartDAO.deleteCartItemUsingProductId(id);
    }


}

package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.DTO.CartDTO;
import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.Entity.Cart;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartController {

    public CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/cart")
    public ResponseEntity<?> addProductToCart(@RequestBody CartDTO cartDTO)
    {
        try
        {
            cartService.addProductToCart(cartDTO);
            return ResponseEntity.ok("Cart Added Successfully");
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/cart/{id}")
    public List<ProductDTO> getCartItems(@PathVariable int id)
    {
        return cartService.getProductsFromCartUsingUserId(id);
    }

    @DeleteMapping("/cart")
    public ResponseEntity<?> deleteCartItem(@RequestBody CartDTO cartDTO)
    {
        try
        {
            cartService.deleteCartItem(cartDTO);
            return ResponseEntity.ok("Cart Item Deletion Successfull");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}

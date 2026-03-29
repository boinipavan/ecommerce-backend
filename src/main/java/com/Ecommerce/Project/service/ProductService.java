package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.DTO.ProductFilter;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;

import java.util.List;

public interface ProductService {
    public Product saveProduct(Product product);
    public List<ProductDTO> getProducts();
    public Product getProductById(int id);
    public void deleteProduct(Product product);
    public Product updateProduct(Product product);
    public User getUserForProducts(int id);
    public List<Product> getManagerProducts(int id);
    public List<ProductDTO> getFilteredProducts(ProductFilter productFilter);
    public List<ProductDTO> findNextProducts(Integer cursor,int size);
    public ProductDTO getProductDTOById(int id);
}

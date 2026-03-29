package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.ProductFilter;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;

import java.util.List;


public interface ProductDAO {
    public Product saveProduct(Product product);
    public List<Product> getProducts();
    public Product getProductById(int id);
    public void deleteProduct(Product product);
    public Product updateProduct(Product product);
    public List<Product> getManagerProducts(int id);
    public List<Product> getFilteredProducts(String jpql, ProductFilter productFilter);
    public List<Product> findNextProducts(Integer cursor,int size);
    public int getProductPrice(int id);

}

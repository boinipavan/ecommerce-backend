package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.ProductFilter;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductDAOImpl implements ProductDAO{

    private final EntityManager entityManager;
    @Autowired
    public ProductDAOImpl(EntityManager entityManager) {
        this.entityManager=entityManager;
    }

    @Override
    @Transactional
    public Product saveProduct(Product product) {
       entityManager.persist(product);
        return product;
    }

    @Override
    public List<Product> getProducts() {
        List<Product> products=entityManager.createQuery("From Product", Product.class).getResultList();
        return products;
    }

    @Override
    public Product getProductById(int id) {
        Product product=entityManager.find(Product.class,id);
        return product;
    }

    @Override
    @Transactional
    public void deleteProduct(Product product) {
        entityManager.remove(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Product product) {
        return entityManager.merge(product);
    }

    @Override
    public List<Product> getManagerProducts(int id) {
        String jpql="select p from Product p where p.user.id= :id";
        List<Product> products= entityManager.createQuery(jpql,Product.class).
                setParameter("id",id).
                getResultList();
        return products;
    }

    @Override
    public List<Product> getFilteredProducts(String jpql, ProductFilter productFilter) {
        TypedQuery<Product> query=entityManager.createQuery(jpql, Product.class);

        if(productFilter.getCategory()!=null)
            query.setParameter("category",productFilter.getCategory());

        if(productFilter.getTitle()!=null)
            query.setParameter("title",productFilter.getTitle());

        if(productFilter.getMinPrice()!=null)
            query.setParameter("minPrice",productFilter.getMinPrice());

        if(productFilter.getMaxPrice()!=null)
            query.setParameter("maxPrice",productFilter.getMaxPrice());

        return query.getResultList();
    }

    @Override
    public List<Product> findNextProducts(Integer cursor, int size) {
        String jpql = "SELECT p FROM Product p " +
                (cursor != null ? "WHERE p.id < :cursor " : "") +
                "ORDER BY p.id DESC";

        TypedQuery<Product> query = entityManager.createQuery(jpql, Product.class);

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public int getProductPrice(int id) {
        String jpql="select p.price from Product p where p.id= :id";
        TypedQuery<Integer> query=entityManager.createQuery(jpql,Integer.class);
        query.setParameter("id",id);
        return query.getSingleResult();
    }


}

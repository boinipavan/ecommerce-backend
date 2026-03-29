package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.ProductDAO;
import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.DTO.ProductFilter;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService{

    private ProductDAO productDAO;
    private UserService userService;
    private  CartService cartService;
    private OrderService orderService;
    @Autowired
    public ProductServiceImpl(ProductDAO productDAO,UserService userService,CartService cartService,OrderService orderService) {
        this.productDAO = productDAO;
        this.userService=userService;
        this.cartService=cartService;
        this.orderService=orderService;
    }

    @Override
    public Product saveProduct(Product product) {
       return productDAO.saveProduct(product);
    }

    @Override
    public List<ProductDTO> getProducts() {
        List<ProductDTO> productDTO=new ArrayList<>();
        List<Product> products= productDAO.getProducts();
        for(Product product:products)
        {
            productDTO.add(productToProductDTO(product));
        }
        return productDTO;
    }

    @Override
    public Product getProductById(int id) {
        return productDAO.getProductById(id);
    }

    @Override
    public ProductDTO getProductDTOById(int id) {
        Product product= productDAO.getProductById(id);
        return productToProductDTO(product);

    }


    @Override
    public void deleteProduct(Product product) {
        System.out.println("calling cart delete");
        // here manually calling delete product from cart just to handle foreign key reference elements are removed first
        cartService.deleteCartItemUsingProductId(product.getId());

        //manually delete products with product id
         orderService.deleteOrder(product.getId());

        productDAO.deleteProduct(product);
    }

    @Override
    public Product updateProduct(Product product) {
        return productDAO.updateProduct(product);
    }

    @Override
    public User getUserForProducts(int id) {
        return userService.getUserForProduct(id);
    }

    @Override
    public List<Product> getManagerProducts(int id) {
        return productDAO.getManagerProducts(id);
    }

    @Override
    public List<ProductDTO> getFilteredProducts(ProductFilter productFilter) {
        String jpql="select p from Product p where 1=1";


        if(productFilter.getCategory()!=null)
        {
            jpql+=" and p.category like(:category)";
        }
        if(productFilter.getTitle()!=null)
        {
            jpql +=" and p.title like (:title)";
        }
        if(productFilter.getMinPrice()!=null)
        {
            jpql+=" and p.price>= :minPrice";
        }
        if(productFilter.getMaxPrice()!=null)
        {
            jpql+=" and p.price<= :maxPrice";
        }
        List<Product> products= productDAO.getFilteredProducts(jpql,productFilter);
        List<ProductDTO> productDTO=new ArrayList<>();
        for(Product product:products)
        {
            productDTO.add(productToProductDTO(product));
        }
        return productDTO;
    }
//load more products
    @Override
    public List<ProductDTO> findNextProducts(Integer cursor, int size) {

        List<ProductDTO> productDTO=new ArrayList<>();
        List<Product> products= productDAO.findNextProducts(cursor,size);
        for(Product product:products)
        {
            productDTO.add(productToProductDTO(product));
        }
        return productDTO;
    }

    public ProductDTO productToProductDTO(Product product)
    {
        ProductDTO productDTO=new ProductDTO();
        productDTO.setProductId(product.getId());
        productDTO.setTitle(product.getTitle());
        productDTO.setCategory(product.getCategory());
        productDTO.setImageData(product.getImageData());
        productDTO.setImageName(product.getImageName());
        productDTO.setImageType(product.getImageType());
        productDTO.setAverageRating(product.getAverageRating());
        productDTO.setDescription(product.getDescription());
        productDTO.setPrice(product.getPrice());
        productDTO.setTotalReviews(product.getTotalReviews());
        productDTO.setStockAvailable(product.getStockAvailable());
        return productDTO;
    }
}

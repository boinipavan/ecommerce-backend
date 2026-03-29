package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.DTO.ProductDTO;
import com.Ecommerce.Project.DTO.ProductFilter;
import com.Ecommerce.Project.Entity.Product;
import com.Ecommerce.Project.Entity.User;
import com.Ecommerce.Project.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(value = "/product", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadProduct(
            @RequestPart("product") String productJson,
            @RequestParam("userId") int userId,
            @RequestPart("productImage") MultipartFile productImage) {

        try {
            // Deserialize product JSON manually
            ObjectMapper mapper = new ObjectMapper();
            Product product = mapper.readValue(productJson, Product.class);

            // Set image info
            product.setImageName(productImage.getOriginalFilename());
            product.setImageType(productImage.getContentType());
            product.setImageData(productImage.getBytes());
            User user=productService.getUserForProducts(userId);
            product.setUser(user);
            productService.saveProduct(product);

            return new ResponseEntity<>(product, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products")
    public List<ProductDTO> getProducts()
    {
        return productService.getProducts();
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") int id)
    {
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    //for product full details display
    @GetMapping("/productDTO/{id}")
    public ResponseEntity<ProductDTO> getProductDTOById(@PathVariable("id") int id)
    {
        ProductDTO productDTO = productService.getProductDTOById(id);
        if (productDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productDTO);
    }

    @GetMapping("/products/manager")
    public List<Product> getManagerProducts(@RequestParam("id") int id)
    {
        return productService.getManagerProducts(id);
    }




    @DeleteMapping("/product/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable int id )
    {
        Product product=productService.getProductById(id);
        if(product!=null)
        {
            productService.deleteProduct(product);
            return ResponseEntity.ok("product successfully deleted");
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }
    }


    @PutMapping(value = "/product/" +
            "{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProduct(
            @PathVariable int id,
            @RequestPart("product") String productJson,
            @RequestPart(value="productImage",required = false) MultipartFile productImage) {
                System.out.println("update log");
                System.out.println("id: "+id);
        try {
            // Deserialize product JSON
            ObjectMapper mapper = new ObjectMapper();
            Product updatedProduct = mapper.readValue(productJson, Product.class);

            // Check if product exists
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
            }

            // Set ID to ensure correct record is updated
            updatedProduct.setId(id);
            updatedProduct.setUser(existingProduct.getUser());
            // Update image only if a new one is provided
            if (productImage != null && !productImage.isEmpty()) {
                updatedProduct.setImageName(productImage.getOriginalFilename());
                updatedProduct.setImageType(productImage.getContentType());
                updatedProduct.setImageData(productImage.getBytes());
            } else {
                // Retain existing image data
                updatedProduct.setImageName(existingProduct.getImageName());
                updatedProduct.setImageType(existingProduct.getImageType());
                updatedProduct.setImageData(existingProduct.getImageData());
            }

            productService.updateProduct(updatedProduct);

            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/filterProducts")
    public ResponseEntity<List<ProductDTO>> getFilteredProducts(@RequestBody ProductFilter productFilter)
    {
        System.out.println(productFilter.getCategory()+" "+productFilter.getTitle()+" "+productFilter.getMinPrice()+" "+productFilter.getMaxPrice());
        List<ProductDTO> products=productService.getFilteredProducts(productFilter);
        return ResponseEntity.ok(products);
    }

    @RequestMapping("/cursor")
    public ResponseEntity<List<ProductDTO>> findNextProducts(@RequestParam(required = false) Integer cursor,@RequestParam(defaultValue = "10") int size)
    {
        List<ProductDTO> productDTO= productService.findNextProducts(cursor,size);
        return ResponseEntity.ok(productDTO);
    }

}

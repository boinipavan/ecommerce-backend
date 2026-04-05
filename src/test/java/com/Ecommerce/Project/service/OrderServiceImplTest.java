package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.ProductDAO;
import com.Ecommerce.Project.DTO.OrderDTO;
import com.Ecommerce.Project.Entity.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@SpringBootTest
class OrderServiceImplTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductDAO productDAO;
    @Test
    public void ordersConcurrencyIssueTestingForOptimisticLocking(){
        int threads=100;
        int userId=1;
        ExecutorService executorService= Executors.newFixedThreadPool(threads);
        CountDownLatch launch=new CountDownLatch(1);
        CountDownLatch workers=new CountDownLatch(threads);
        AtomicInteger success=new AtomicInteger();
        AtomicInteger failed=new AtomicInteger();

        Product product=new Product();
        product.setStockAvailable(1);
        product.setTitle("Bike");
        product.setPrice(422);
        Product savedProduct=productDAO.saveProduct(product);
        for(int i=0;i<threads;i++){
            executorService.submit(()->{
                OrderDTO orderDTO=new OrderDTO();
                orderDTO.setProductId(savedProduct.getId());
                orderDTO.setUserId(1);
                orderDTO.setQuantity(1);
                orderDTO.setTotalPrice(savedProduct.getPrice());
                try {
                    launch.await();
                    orderService.placeOrder(orderDTO);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    //throw new RuntimeException(e);
                }
                finally {
                    workers.countDown();
                }

            });
        }
        launch.countDown();
        try {
            workers.await();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            System.out.println("Exception occured while workers await");
        }

        System.out.println("success "+ success.get());
        System.out.println("failed "+failed.get());
        Assertions.assertEquals(1,success.get());
        Assertions.assertEquals(99,failed.get());
    }

}
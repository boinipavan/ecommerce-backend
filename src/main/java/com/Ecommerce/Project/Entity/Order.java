package com.Ecommerce.Project.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    //  User relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // FK column
    private User user;

    //  Product relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // FK column
    private Product product;

    @Column(name="quantity")
    private int quantity;

    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "order_time")
    private LocalDateTime localDateTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Enum for order status
    public enum Status {
        PLACED,
        SHIPPED,
        CANCELLED
    }

    //  Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

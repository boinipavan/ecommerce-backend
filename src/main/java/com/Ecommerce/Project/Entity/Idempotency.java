package com.Ecommerce.Project.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Table(name="idempotency_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","idempotency_key"}),
        indexes = @Index(name="idx_expire",columnList = "expire_at")
)
@NoArgsConstructor
@Getter
@Setter
public class Idempotency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id",nullable = false)
    private Integer userId;

    @Column(name = "idempotency_key",nullable = false,length = 100,updatable = false)
    private String idempotencyKey;

    @Column(name = "request_payload_hash",nullable = false,updatable = false)
    private String requestPayloadHash;

    @Column(name = "response_payload",columnDefinition ="json")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private Status status;

    @Column(name="status_code")
    private Integer statusCode;

    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="expire_at",nullable = false)
    private LocalDateTime expireAt;

    public enum Status {
        IN_PROGRESS, COMPLETED, FAILED
    }

    @PrePersist
    public void onCreate(){
        LocalDateTime now=LocalDateTime.now();
        createdAt=now;
        updatedAt=now;
        if(expireAt==null){
            expireAt=now.plusHours(24);
        }
        if(status==null){
            status=Status.IN_PROGRESS;
        }
    }

    @PreUpdate
    public void onUpdate(){
        updatedAt=LocalDateTime.now();
    }

}

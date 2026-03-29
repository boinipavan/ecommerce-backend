package com.Ecommerce.Project.Entity;

import jakarta.persistence.*;

@Entity
public class RoleUpgrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int id;


    @ManyToOne()
    @JoinColumn(name="user_id")
    private User user;

    @Column(name="status")
    private Status status=Status.PENDING;

    @Column(name="requested_role")
    private String requestedRole="MANAGER";


    @JoinColumn(name = "reviewer_id")
    @ManyToOne
    private User reviewer;

    public enum Status
    {
        PENDING,APPROVED,REJECTED
    }

    public RoleUpgrade() {
    }

    public RoleUpgrade(User user, Status status, String requestedRole, User reviewer) {
        this.user = user;
        this.status = status;
        this.requestedRole = requestedRole;
        this.reviewer = reviewer;
    }

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    @Override
    public String toString() {
        return "RoleUpgradeRequest{" +
                "id=" + id +
                ", user=" + user +
                ", status=" + status +
                ", requestedRole='" + requestedRole + '\'' +
                ", reviewer=" + reviewer +
                '}';
    }
}

package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.Role;
import com.Ecommerce.Project.Entity.RoleUpgrade;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public class RoleUpgradeDAOImpl implements RoleUpgradeDAO{

    private EntityManager entityManager;

    public RoleUpgradeDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<RoleUpgrade> getPendingRoleUpgradeRequest() {
        return entityManager.createQuery("select r from RoleUpgrade r where r.status= :status",RoleUpgrade.class).
                setParameter("status", RoleUpgrade.Status.PENDING).
                getResultList();

    }

    @Override
    @Transactional
    public void saveRoleRequest(RoleUpgrade role) {
        entityManager.persist(role);
    }

    @Override
    public boolean hasPendingRequest(int userId) {
        Long count=(Long) entityManager.createQuery("select count(r) from RoleUpgrade r where r.user.id= :userId and r.status= :status").
                setParameter("userId",userId)
                .setParameter("status",RoleUpgrade.Status.PENDING).
                getSingleResult();
        return count>0;
    }

    @Override
    @Transactional
    public void updateRoleRequest(RoleUpgrade role) {
        entityManager.merge(role);
    }

    @Override
    public RoleUpgrade getRoleUpgradeusiingUserId(int userId) {
        RoleUpgrade role= entityManager.createQuery("select r from RoleUpgrade r where r.user.id= :userId and r.status= :status",RoleUpgrade.class)
                .setParameter("userId",userId)
                .setParameter("status",RoleUpgrade.Status.PENDING)
                .getSingleResult();
        System.out.print(role);
        return role;
    }
}

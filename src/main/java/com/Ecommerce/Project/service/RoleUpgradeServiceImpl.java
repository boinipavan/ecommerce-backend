package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.RoleUpgradeDAO;
import com.Ecommerce.Project.DAO.RoleUpgradeDAOImpl;
import com.Ecommerce.Project.DAO.UserDAO;
import com.Ecommerce.Project.DTO.Role;
import com.Ecommerce.Project.Entity.RoleUpgrade;
import com.Ecommerce.Project.Entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleUpgradeServiceImpl implements RoleUpgradeService{

    private RoleUpgradeDAO roleUpgradeDAO;
    private UserDAO userDAO;
    public RoleUpgradeServiceImpl(RoleUpgradeDAO roleUpgradeDAO,UserDAO userDAO) {
        this.roleUpgradeDAO = roleUpgradeDAO;
        this.userDAO=userDAO;
    }

    @Override
    public List<RoleUpgrade> getRoleUpgradeRequest() {
        return roleUpgradeDAO.getPendingRoleUpgradeRequest();
    }

    @Override
    public String saveRoleRequest(int userId) {
            User user=userDAO.getUser(userId);
            if(user==null)
            {
                return "user not found";
            }
            if(!user.getRole().equals( Role.USER))
            {
                return "sorry only user can request for role upgrade";
            }
            if(roleUpgradeDAO.hasPendingRequest(userId))
            {
                return "you already have pending request";
            }
            RoleUpgrade role=new RoleUpgrade();
            role.setUser(user);
            role.setRequestedRole("MANAGER");
            role.setStatus(RoleUpgrade.Status.PENDING);


            roleUpgradeDAO.saveRoleRequest(role);
            return "request sent to admin";
    }

    @Override
    public String reviewRoleRequest(int userId, int adminId, boolean approve) {
        RoleUpgrade role=roleUpgradeDAO.getRoleUpgradeusiingUserId(userId);
        User user=userDAO.getUser(userId);
        if(role==null || role.getUser().getRole()!=Role.USER)
        {
            return "user not exist or request already handled";
        }
        User admin=userDAO.getUser(adminId);
        if(admin==null || admin.getRole()!=Role.ADMIN)
        {
            return "only admin can upgrade request";
        }
        RoleUpgrade roleUpgrade=new RoleUpgrade();
        roleUpgrade.setId(role.getId());
        roleUpgrade.setStatus(approve ? RoleUpgrade.Status.APPROVED : RoleUpgrade.Status.REJECTED);
        roleUpgrade.setReviewer(admin);
        roleUpgrade.setUser(user);
        roleUpgradeDAO.updateRoleRequest(roleUpgrade);
        if(approve)
        {

            user.setRole(Role.MANAGER);
            userDAO.updateUserRole(user);
        }
        return approve?"approved":"rejected";
    }
}

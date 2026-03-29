package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.Entity.RoleUpgrade;

import java.util.List;

public interface RoleUpgradeDAO {
    public List<RoleUpgrade> getPendingRoleUpgradeRequest();
    public void saveRoleRequest(RoleUpgrade role);
    public boolean hasPendingRequest(int userId);
    public void updateRoleRequest(RoleUpgrade role);
    public RoleUpgrade getRoleUpgradeusiingUserId(int userId);
}

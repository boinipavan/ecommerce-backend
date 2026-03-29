package com.Ecommerce.Project.service;


import com.Ecommerce.Project.Entity.RoleUpgrade;

import java.util.List;

public interface RoleUpgradeService {
    public List<RoleUpgrade> getRoleUpgradeRequest();
    public String saveRoleRequest(int userId);
    public String reviewRoleRequest(int userId,int adminId,boolean approve);
}

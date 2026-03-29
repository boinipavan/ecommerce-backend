package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.LoginUser;
import com.Ecommerce.Project.DTO.RegisterUser;
import com.Ecommerce.Project.Entity.User;



public interface UserDAO {
    public User registerUser(RegisterUser registerUser);
    public User loginUser(LoginUser loginUser);
    public User getUserForProducts(int id);
    public  User getUser(int id);
    public User updateUserRole(User user);
    public User loadUserByUserName(String userName);
}

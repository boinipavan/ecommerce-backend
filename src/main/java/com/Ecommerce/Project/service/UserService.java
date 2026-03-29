package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DTO.LoginUser;
import com.Ecommerce.Project.DTO.RegisterUser;
import com.Ecommerce.Project.DTO.UserDetails;
import com.Ecommerce.Project.Entity.User;

public interface UserService {
    public User registerUser(RegisterUser registerUser);
    public User LoginUser(LoginUser loginUser);
    public UserDetails convertUserToUserDetails(User user);
    public User getUserForProduct(int id);
    public User getUser(int id);
   // public  User updateUserRole(User user);
    public User loadUserByUserName(String userName);

}

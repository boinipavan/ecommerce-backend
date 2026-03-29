package com.Ecommerce.Project.service;

import com.Ecommerce.Project.DAO.UserDAO;
import com.Ecommerce.Project.DTO.LoginUser;
import com.Ecommerce.Project.DTO.RegisterUser;
import com.Ecommerce.Project.DTO.UserDetails;
import com.Ecommerce.Project.Entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{

    private UserDAO userDAO;
    UserServiceImpl(UserDAO userDAO)
    {
        this.userDAO=userDAO;
    }
    @Override
    public User registerUser(RegisterUser registerUser) {

        return userDAO.registerUser(registerUser);
    }

    @Override
    public User LoginUser(LoginUser loginUser) {
        return userDAO.loginUser(loginUser);
    }

    @Override
    public UserDetails convertUserToUserDetails(User user) {
         UserDetails userDetails=new UserDetails(user.getId(),user.getUserName(),user.getRole());
        return userDetails;
    }

    @Override
    public User getUserForProduct(int id) {
        return userDAO.getUserForProducts(id);
    }

    @Override
    public User getUser(int id) {
        User user=userDAO.getUser(id);
        return user;
    }

  /*  @Override
    public User updateUserRole(User user) {
        return userDAO.updateUserRole(user);
    }
   */

    @Override
    public User loadUserByUserName(String userName) {
        return userDAO.loadUserByUserName(userName);
    }
}

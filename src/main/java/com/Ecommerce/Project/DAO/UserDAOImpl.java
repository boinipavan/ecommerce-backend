package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.DTO.LoginUser;
import com.Ecommerce.Project.DTO.RegisterUser;
import com.Ecommerce.Project.DTO.Role;
import com.Ecommerce.Project.Entity.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDAOImpl implements UserDAO{

    private EntityManager entityManager;
    private  PasswordEncoder passwordEncoder;

    @Autowired
    public UserDAOImpl(EntityManager entityManager, PasswordEncoder passwordEncoder) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegisterUser registerUser) {
        if(isUserNameTaken(registerUser.getUsername()))
        {
            throw new RuntimeException("user already taken");
        }
        User user=new User();
        user.setUserName(registerUser.getUsername());
        user.setPassWord(passwordEncoder.encode(registerUser.getPassword()));
        user.setRole(Role.USER);
        entityManager.persist(user);
        return user;
    }

    @Override
    public User loginUser(LoginUser loginUser) {

        String jpql="select u from User u where u.userName= :userName";
        List<User> users=entityManager.createQuery(jpql,User.class)


                .setParameter("userName",loginUser.getUsername())
                .getResultList();
        if(users.isEmpty())
        {
            throw new RuntimeException("user not registered");
        }
        User user=users.get(0);
        if(!passwordEncoder.matches(loginUser.getPassword(),user.getPassWord()))
        {
            throw new RuntimeException("Invalid Credentials");
        }
        return user;

    }

    private boolean isUserNameTaken(String userName)
    {
        String jpql="select count(u) from User u where u.userName= :userName";
        Long count=entityManager.createQuery(jpql,Long.class)
                .setParameter("userName",userName)
                .getSingleResult();
         return  count>0;
    }

    @Override
    public User getUserForProducts(int id) {
        User user=  entityManager.find(User.class,id);
        return user;
    }

    @Override
    public User getUser(int id) {
        User user=entityManager.find(User.class,id);
        return user;
    }

    @Override
    @Transactional
    public User updateUserRole(User user) {
        return entityManager.merge(user);

    }


    @Override
    public User loadUserByUserName(String userName) {
        return entityManager.createQuery("FROM User WHERE userName = :uname", User.class)
                .setParameter("uname", userName)
                .getSingleResult();
    }
}

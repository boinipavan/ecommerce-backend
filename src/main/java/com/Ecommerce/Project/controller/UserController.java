package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.DTO.LoginUser;
import com.Ecommerce.Project.DTO.RegisterUser;
import com.Ecommerce.Project.DTO.Role;
import com.Ecommerce.Project.DTO.UserDetails;
import com.Ecommerce.Project.Entity.User;
import com.Ecommerce.Project.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterUser registerUser)
    {
        System.out.println("register method");
        try
        {
            User user=userService.registerUser(registerUser);
            System.out.println("password"+user.getPassWord());
            UserDetails userDetails=userService.convertUserToUserDetails(user);
            return ResponseEntity.ok(userDetails);
        } catch (RuntimeException e) {
            return   ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginUser loginuser)
    {
        try
        {
            User user=userService.LoginUser(loginuser);
            UserDetails userDetails=userService.convertUserToUserDetails(user);
            return ResponseEntity.ok(userDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

  /*  @PostMapping("/updateRole")
    public ResponseEntity<?> upgradeUsertoManager(@RequestParam("userId") int userId)
    {
        try {
            User user = userService.getUser(userId);
            user.setRole(Role.MANAGER);
            userService.updateUserRole(user);
            return  ResponseEntity.ok("user role updated to manager");
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

   */
}

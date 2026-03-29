package com.Ecommerce.Project.security;

import com.Ecommerce.Project.Entity.User;
import com.Ecommerce.Project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomUserDetailsService implements UserDetailsService {

    private UserService userService;

    @Autowired
    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userService.loadUserByUserName(username);
            return new CustomUserDetails(user);
        } catch (RuntimeException e) {
            throw new UsernameNotFoundException("user not found");
        }
                //.orElseThrow(() -> new UsernameNotFoundException("user not found"));

    }
}

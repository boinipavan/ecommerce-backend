package com.Ecommerce.Project.config;

import com.Ecommerce.Project.filter.LoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.sql.DataSource;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public LoggingFilter loggingFilter(){
        return new LoggingFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,LoggingFilter loggingFilter) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login","/products","/filterProducts","/cursor").permitAll()
                        .requestMatchers("/products/manager").hasRole("MANAGER")
                        .requestMatchers("/order").hasAnyRole("USER","MANAGER","ADMIN")
                        // create product
                        .requestMatchers("/product/{id}","/productDTO/{id}").permitAll()//to display project details
                        .requestMatchers("/product/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/cart/**").permitAll()// update/delete product by id
                        .requestMatchers("/roleUpgrade/**").permitAll()

                              // view manager's products
                        .anyRequest().authenticated()
                )
                .addFilterAfter(loggingFilter, BasicAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    @Bean
    public UserDetailsManager userDetailsManager(DataSource dataSource)
    {
        JdbcUserDetailsManager manager=new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery("select username,password,enabled from users  where username= ?");
        manager.setAuthoritiesByUsernameQuery("select username,concat('ROLE_',role) from users where username= ?");
        return manager;
    }
   /*
    @Bean
    public AuthenticationManager authManager(HttpSecurity http,CustomUserDetailsService customUserDetailsService) throws Exception{
        AuthenticationManagerBuilder auth= http.getSharedObject(AuthenticationManagerBuilder.class);
               auth .userDetailsService(customUserDetailsService)
               .passwordEncoder(passwordEncoder());
               return auth .build();
    }

    @Bean
    public CustomUserDetailsService userDetailsService(UserService userService) {
        return new CustomUserDetailsService(userService);
    }

    */


    @Bean
    public  PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}

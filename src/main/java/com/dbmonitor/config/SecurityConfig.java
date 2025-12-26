package com.dbmonitor.config;

import com.dbmonitor.security.CustomAuthenticationFailureHandler;
import com.dbmonitor.security.CustomAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            // Security disabled - allow all requests
            http
                .csrf().disable()
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll()
                );
        } else {
            // Security enabled - require authentication and authorization
            http
                .csrf().disable()
                .authorizeHttpRequests(authz -> authz
                    // Public endpoints
                    .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                    // Admin endpoints
                    .requestMatchers("/api/users/**", "/api/roles/**", "/api/privileges/**", 
                                   "/api/audit-logs/**", "/api/security/**").hasAuthority("ADMIN_USERS")
                    // Management endpoints
                    .requestMatchers("/api/connections/**").hasAuthority("MANAGE_CONNECTIONS")
                    .requestMatchers("/api/alerts/**").hasAuthority("MANAGE_ALERTS")
                    // View endpoints
                    .requestMatchers("/api/metrics/**", "/api/queries/**").hasAnyAuthority("VIEW_METRICS", "VIEW_QUERIES")
                    // All other requests require authentication
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                    .loginPage("/login")
                    .successHandler(authenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler)
                    .permitAll()
                )
                .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout=true")
                    .permitAll()
                )
                .userDetailsService(userDetailsService);
        }
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

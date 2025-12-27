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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    @Value("${security.remember-me.key:default-remember-me-key}")
    private String rememberMeKey;

    @Value("${security.remember-me.token-validity-seconds:2592000}")
    private int rememberMeTokenValidity;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private DataSource dataSource;

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
                    .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                   "/forgot-password", "/reset-password").permitAll()
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
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID", "remember-me")
                    .permitAll()
                )
                .rememberMe(remember -> remember
                    .key(rememberMeKey)
                    .tokenRepository(persistentTokenRepository())
                    .tokenValiditySeconds(rememberMeTokenValidity)
                    .userDetailsService(userDetailsService)
                )
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .invalidSessionUrl("/login?expired=true")
                    .maximumSessions(1)
                    .expiredUrl("/login?expired=true")
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

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        // Note: The table will be created automatically by Hibernate or you can create it manually:
        // CREATE TABLE persistent_logins (
        //     username VARCHAR(64) NOT NULL,
        //     series VARCHAR(64) PRIMARY KEY,
        //     token VARCHAR(64) NOT NULL,
        //     last_used TIMESTAMP NOT NULL
        // );
        return tokenRepository;
    }
}

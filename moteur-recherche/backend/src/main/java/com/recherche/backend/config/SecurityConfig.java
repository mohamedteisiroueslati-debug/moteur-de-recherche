package com.recherche.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORAIRE : ouvre /api/index/** pour pouvoir tester l'indexation.
 * Sans cette classe, Spring Security bloque toutes les URL (login par défaut, et CSRF refuse les POST).
 * À remplacer par la vraie configuration (JWT, rôles USER/ADMIN) quand l'authentification sera faite.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/index/**").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}

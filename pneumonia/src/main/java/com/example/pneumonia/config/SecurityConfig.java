package com.example.pneumonia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey key = JwtSecrets.key(secret);
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // Transforme la claim "role" du token (USER / ADMIN) en autorité ROLE_USER / ROLE_ADMIN
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean 
    CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (!origins.isEmpty()) {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(origins);
            cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
            cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            cfg.setMaxAge(3600L);
            source.registerCorsConfiguration("/api/**", cfg);
        }
        return source;
    }



}
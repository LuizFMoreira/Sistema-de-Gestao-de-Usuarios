package com.luizfernando.gestaousuarios.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Habilita a malha CORS na segurança
                .csrf(csrf -> csrf.disable()) // Desativa proteção CSRF, pois usaremos tokens JWT
                .authorizeHttpRequests(auth -> auth
                        // Libera a rota de CADASTRO
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Qualquer outra requisição precisará de autenticação
                        .anyRequest().authenticated()
                )
                .build();
    }

    // A matriz absoluta de permissões CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Define a origem exata do seu Frontend React
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        // Permite os métodos de tráfego necessários
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite o trânsito de todos os cabeçalhos (incluindo futuros tokens JWT)
        configuration.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
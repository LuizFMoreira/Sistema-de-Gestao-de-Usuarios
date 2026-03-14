package com.luizfernando.gestaousuarios.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // import do filtro pra não quebrar
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // criei essa variável pra puxar o filtro de segurança (meu porteiro) que eu fiz
    private final SecurityFilter securityFilter;

    // injetando via construtor (anotei que o professor disse que é melhor que usar @Autowired)
    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean // tinha um @Bean duplicado aqui antes, já tirei pra não dar erro de compilação
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // desativei porque vou usar JWT
                .authorizeHttpRequests(auth -> auth
                        // liberando o tal do OPTIONS pro React não tomar aquele erro 403 fantasma de CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                        
                        // rotas que deixei públicas pro pessoal conseguir cadastrar e logar no app
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        
                        // o resto todo tem que ter o token JWT pra acessar
                        .anyRequest().authenticated()
                )
                // coloquei meu filtro pra rodar ANTES do filtro padrão do spring pra checar o token logo de cara
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // config manual do CORS pra deixar meu frontend (vite na porta 5173) bater aqui no backend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // só aceita requisição do meu react
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // instanciando o encriptador pra não salvar senha em texto puro no banco de jeito nenhum
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
package com.luizfernando.gestaousuarios.core.security;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository repository) {
        this.tokenService = tokenService;
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // pega o token
        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            // puxa o email
            var email = tokenService.getSubject(tokenJWT);
            
            // busca o usuario no bd, com base no tempo de vida do token
            Usuario usuario = repository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // força o Spring a considerar este usuário como logado nesta requisição
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // continuação do fluxo normal da requisição
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}
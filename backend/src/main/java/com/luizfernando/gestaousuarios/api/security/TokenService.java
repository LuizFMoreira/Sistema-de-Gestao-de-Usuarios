package com.luizfernando.gestaousuarios.core.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    // Essa chave deve ser secreta. No futuro, colocaremos no application.properties
    @Value("${api.security.token.secret:minha-chave-secreta-super-protegida}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("gestao-usuarios-api") // Quem emitiu o token
                    .withSubject(usuario.getEmail())   // De quem é o token
                    .withExpiresAt(dataExpiracao())    // Quando ele vence (segurança!)
                    .sign(algoritmo);                  // Assinatura digital
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    private Instant dataExpiracao() {
        // O token vai valer por 2 horas
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
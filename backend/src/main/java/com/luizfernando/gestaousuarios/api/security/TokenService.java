package com.luizfernando.gestaousuarios.core.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 🔐 ANÁLISE DE SEGURANÇA
 *
 * ⚠️ CRÍTICO: Secret Key com valor padrão inseguro na aplicação
 *
 * 💡 SUGESTÃO: Usar variável de ambiente ou Secret Manager
 * PROBLEMA ATUAL:
 * @Value("${api.security.token.secret:minha-chave-secreta-super-protegida}")
 *
 * PROBLEMAS:
 * 1. Default value exposto no código-fonte
 * 2. Chave fraca não é criptograficamente segura
 * 3. Mesmo valor em todo lugar (dev, test, prod)
 * 4. Se repositório vazar, a chave está exposta
 * 5. Impossível rotacionar a chave sem redeploy
 *
 * REFATORAÇÃO SUGERIDA:
 * Em application.properties (NUNCA commitar):
 * api.security.token.secret=${SECURITY_TOKEN_SECRET}
 * api.security.token.expiration=7200
 *
 * Ou usar Spring Cloud Config / HashiCorp Vault
 *
 * NO DOCKERFILE/DEPLOY:
 * ENV SECURITY_TOKEN_SECRET="sua-chave-de-256-bits-aleatoria-gerada-criptograficamente"
 *
 * BENEFÍCIOS:
 * - Chave segura gerada criptograficamente
 * - Não exposta no repositório
 * - Fácil rotacionar em produção
 * - Diferentes valores por ambiente
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    /**
     * ⚠️ SEGURANÇA: Secret key com valor padrão fraco
     */
    @Value("${api.security.token.secret:minha-chave-secreta-super-protegida}")
    private String secret;

    /**
     * ✅ BOM: Tempo de expiração configurável
     * Permite diferentes tempos para diferentes ambientes
     */
    @Value("${api.security.token.expiration:7200}")
    private long expirationSeconds;

    /**
     * ✅ BOM: Separação entre geração e verificação de token
     * Facilita reutilização e testes
     */
    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withIssuer("gestao-usuarios-api")
                    .withSubject(usuario.getEmail())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);

            // ✅ BOM: Logging de geração de token (auditoria)
            logger.info("Token JWT gerado para usuário: {}", usuario.getEmail());

            return token;
        } catch (JWTCreationException exception) {
            /**
             * ⚠️ ERROR HANDLING: RuntimeException genérica
             *
             * 💡 SUGESTÃO: Criar exceção específica TokenGerationException
             * RuntimeException é muito genérica. Melhor criar exceção de domínio.
             *
             * REFATORAÇÃO:
             * public class TokenGenerationException extends RuntimeException {
             *     public TokenGenerationException(String message, Throwable cause) {
             *         super(message, cause);
             *     }
             * }
             *
             * throw new TokenGenerationException("Erro ao gerar token JWT", exception);
             */
            logger.error("Erro ao gerar token JWT para usuário: {}", usuario.getEmail(), exception);
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    /**
     * ⚠️ PROBLEMA: Não diferencia entre tipos de erro JWT
     *
     * 💡 SUGESTÃO: Separar exceções por tipo
     * Atualmente qualquer erro JWTVerificationException é tratado igual.
     * Não consegue diferenciar:
     * - Token expirado (requer refresh_token)
     * - Token corrompido (requer novo login)
     * - Token de outro issuer (segurança)
     *
     * REFATORAÇÃO SUGERIDA:
     * public class TokenExpiredException extends RuntimeException {
     *     public TokenExpiredException() {
     *         super("Token JWT expirado");
     *     }
     * }
     *
     * public class TokenInvalidoException extends RuntimeException {
     *     public TokenInvalidoException(String motivo) {
     *         super("Token JWT inválido: " + motivo);
     *     }
     * }
     *
     * NO MÉTODO:
     * public String getSubject(String tokenJWT) {
     *     try {
     *         Algorithm algoritmo = Algorithm.HMAC256(secret);
     *         var decodedJWT = JWT.require(algoritmo)
     *             .withIssuer("gestao-usuarios-api")
     *             .build()
     *             .verify(tokenJWT);
     *         return decodedJWT.getSubject();
     *     } catch (JWTVerificationException ex) {
     *         if (ex.getMessage().contains("expired")) {
     *             throw new TokenExpiredException();
     *         }
     *         logger.warn("Tentativa de acesso com token inválido");
     *         throw new TokenInvalidoException(ex.getMessage());
     *     }
     * }
     *
     * NO HANDLER (GlobalExceptionHandler):
     * @ExceptionHandler(TokenExpiredException.class)
     * public ResponseEntity<ErrorResponse> handleTokenExpirado(TokenExpiredException ex) {
     *     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
     *         .body(new ErrorResponse("TOKEN_EXPIRADO", ex.getMessage()));
     * }
     *
     * @ExceptionHandler(TokenInvalidoException.class)
     * public ResponseEntity<ErrorResponse> handleTokenInvalido(TokenInvalidoException ex) {
     *     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
     *         .body(new ErrorResponse("TOKEN_INVALIDO", ex.getMessage()));
     * }
     *
     * BENEFÍCIOS:
     * - Frontend consegue fazer refresh_token quando expira
     * - Frontend redireciona para login quando token inválido
     * - Auditoria clara de diferentes tipos de erro
     * - Melhor experiência de usuário
     */
    public String getSubject(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            var decodedJWT = JWT.require(algoritmo)
                    .withIssuer("gestao-usuarios-api")
                    .build()
                    .verify(tokenJWT);

            /**
             * ⚠️ SEGURANÇA: Sem validação do algoritmo
             *
             * 💡 SUGESTÃO: Validar explicitamente o algoritmo
             * Vulnerabilidade conhecida: token com algoritmo "none" é aceito
             *
             * REFATORAÇÃO:
             * String algorithm = decodedJWT.getAlgorithm();
             * if (!"HS256".equals(algorithm)) {
             *     logger.warn("Token com algoritmo não permitido: {}", algorithm);
             *     throw new TokenInvalidoException("Algoritmo não permitido: " + algorithm);
             * }
             *
             * BENEFÍCIO:
             * - Proteção contra algorithm substitution attacks
             * - Conformidade com OWASP top 10
             */

            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            logger.warn("Erro ao verificar token JWT: {}", exception.getMessage());
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    /**
     * ⚠️ PROBLEMA: Timezone hardcoded
     *
     * 💡 SUGESTÃO: Usar configuração do sistema
     * "-03:00" (horário de Brasília) está hardcoded.
     * Se aplicação rodar em outro servidor, pode gerar timestamps inconsistentes.
     *
     * REFATORAÇÃO SUGERIDA:
     * @Value("${app.timezone:UTC}")
     * private String timezone;
     *
     * private Instant dataExpiracao() {
     *     ZoneId zone = ZoneId.of(timezone);
     *     return LocalDateTime.now(zone)
     *         .plusSeconds(expirationSeconds)
     *         .toInstant(zone.getRules().getOffset(LocalDateTime.now()));
     * }
     *
     * BENEFÍCIO:
     * - Configurável por ambiente
     * - Sem problemas de timezone
     * - Fácil adaptar para outras regiões
     */
    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
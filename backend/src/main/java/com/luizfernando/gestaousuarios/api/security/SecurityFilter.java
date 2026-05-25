package com.luizfernando.gestaousuarios.core.security;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * ⚠️ PROBLEMA: Sem tratamento de exceções em filter
 *
 * 💡 SUGESTÃO: Implementar try-catch com tratamento apropriado
 * Se tokenService.getSubject() lançar exceção, o Spring retorna 500.
 * Deveria retornar 401 Unauthorized.
 *
 * REFATORAÇÃO SUGERIDA:
 * @Override
 * protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
 *                                FilterChain filterChain) throws ServletException, IOException {
 *     try {
 *         var tokenJWT = recuperarToken(request);
 *         if (tokenJWT != null) {
 *             var email = tokenService.getSubject(tokenJWT);
 *             Usuario usuario = repository.findByEmail(email)
 *                 .orElseThrow(() -> new UsuarioNaoEncontradoException(email));
 *             var authentication = new UsernamePasswordAuthenticationToken(
 *                 usuario, null, Collections.emptyList());
 *             SecurityContextHolder.getContext().setAuthentication(authentication);
 *         }
 *         filterChain.doFilter(request, response);
 *     } catch (TokenExpiredException ex) {
 *         logger.warn("Token expirado para requisição: {}", request.getRequestURI());
 *         responderComErro(response, HttpStatus.UNAUTHORIZED, "Token expirado");
 *     } catch (TokenInvalidoException ex) {
 *         logger.warn("Token inválido detectado");
 *         responderComErro(response, HttpStatus.UNAUTHORIZED, "Token inválido");
 *     } catch (UsuarioNaoEncontradoException ex) {
 *         logger.warn("Usuário do token não encontrado no banco");
 *         responderComErro(response, HttpStatus.UNAUTHORIZED, "Usuário não encontrado");
 *     }
 * }
 *
 * private void responderComErro(HttpServletResponse response, HttpStatus status, String msg)
 *         throws IOException {
 *     response.setStatus(status.value());
 *     response.setContentType("application/json");
 *     response.getWriter().write("{\"erro\": \"" + msg + "\"}");
 * }
 *
 * BENEFÍCIOS:
 * - HTTP status code correto (401 ao invés de 500)
 * - Frontend consegue tomar ações apropriadas
 * - Auditoria clara de falhas de autenticação
 * - Melhor experiência de usuário
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository repository) {
        this.tokenService = tokenService;
        this.repository = repository;
    }

    /**
     * ✅ BOM: Usa OncePerRequestFilter para garantir execução única por requisição
     * Evita múltiplas execuções do mesmo filtro
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // ⚠️ SEM TRY-CATCH: Se uma exceção é lançada, retorna HTTP 500
        // Deveria retornar 401 Unauthorized para erros de autenticação

        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            /**
             * ⚠️ EXCEPTION HANDLING: Sem tratamento se getSubject() falhar
             *
             * 💡 SUGESTÃO: Adicionar try-catch com logging estruturado
             * Se token expirado, deveria retornar 401
             * Se token corrompido, deveria retornar 401
             * Atualmente ambos retornam 500 (erro interno)
             */
            var email = tokenService.getSubject(tokenJWT);

            /**
             * ⚠️ EXCEPTION HANDLING: RuntimeException genérica sem logging
             *
             * 💡 SUGESTÃO: Exceção específica UsuarioNaoEncontradoException
             * Se usuário foi deletado depois que token foi gerado,
             * deveria logar e retornar 401 apropriadamente.
             */
            Usuario usuario = repository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            /**
             * ✅ BOM: Usa UsernamePasswordAuthenticationToken corretamente
             * Força o Spring a considerar este usuário autenticado para a requisição
             */
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ✅ BOM: Logging de acesso autenticado
            logger.debug("Usuário autenticado via token: {}", email);
        }

        // continuação do fluxo normal da requisição
        filterChain.doFilter(request, response);
    }

    /**
     * ⚠️ VALIDAÇÃO INSUFICIENTE: Não valida formato do header
     *
     * 💡 SUGESTÃO: Validar formato "Bearer <token>"
     * Problemas atuais:
     * - Se header for "Bearer", retorna string vazia
     * - Se for "BearerXXX" (sem espaço), não detecta erro
     * - Sem mensagens de erro claras
     *
     * REFATORAÇÃO SUGERIDA:
     * private String recuperarToken(HttpServletRequest request) {
     *     var authHeader = request.getHeader("Authorization");
     *     if (authHeader == null || authHeader.isBlank()) {
     *         return null;
     *     }
     *
     *     if (!authHeader.startsWith("Bearer ")) {
     *         logger.warn("Authorization header com formato inválido detectado");
     *         throw new TokenInvalidoException("Authorization header deve estar no formato 'Bearer <token>'");
     *     }
     *
     *     String token = authHeader.substring(7);
     *     if (token.isBlank()) {
     *         logger.warn("Token vazio no header Authorization");
     *         throw new TokenInvalidoException("Token não pode estar vazio");
     *     }
     *
     *     return token;
     * }
     *
     * BENEFÍCIOS:
     * - Validação clara do formato
     * - Erros mais informativos
     * - Evita bugs silenciosos
     * - Melhor auditoria
     */
    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            // ⚠️ PROBLEMA: Simples replace sem validação
            // Se for "Bearer" ou "BearerXXX", retorna string inválida silenciosamente
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}
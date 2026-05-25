# 🔧 Exemplos Práticos de Implementação das Melhorias Sugeridas

## 1️⃣ Criar Exceções Customizadas de Domínio

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/domain/exception/EmailJaCadastradoException.java`

```java
package com.luizfernando.gestaousuarios.domain.exception;

public class EmailJaCadastradoException extends RuntimeException {
    private final String email;

    public EmailJaCadastradoException(String email) {
        super("Email '" + email + "' já está cadastrado no sistema.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/domain/exception/UsuarioNaoEncontradoException.java`

```java
package com.luizfernando.gestaousuarios.domain.exception;

public class UsuarioNaoEncontradoException extends RuntimeException {
    private final Long id;

    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário com ID " + id + " não encontrado.");
        this.id = id;
    }

    public UsuarioNaoEncontradoException(String email) {
        super("Usuário com email '" + email + "' não encontrado.");
        this.id = null;
    }

    public Long getId() {
        return id;
    }
}
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/security/exception/TokenExpiredException.java`

```java
package com.luizfernando.gestaousuarios.api.security.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token JWT expirado. Faça login novamente.");
    }
}
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/security/exception/TokenInvalidoException.java`

```java
package com.luizfernando.gestaousuarios.api.security.exception;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String motivo) {
        super("Token JWT inválido: " + motivo);
    }
}
```

---

## 2️⃣ Criar AuthenticationService Separado

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/domain/service/AuthenticationService.java`

```java
package com.luizfernando.gestaousuarios.domain.service;

import com.luizfernando.gestaousuarios.domain.exception.EmailNaoEncontradoException;
import com.luizfernando.gestaousuarios.domain.exception.SenhaInvalidaException;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável APENAS pela autenticação
 * Separado de UsuarioService para SRP (Single Responsibility Principle)
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentica um usuário validando email e senha
     * 
     * @throws EmailNaoEncontradoException se email não existe
     * @throws SenhaInvalidaException se senha está incorreta
     */
    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Tentativa de login com email inexistente: {}", email);
                    return new EmailNaoEncontradoException(email);
                });

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            logger.warn("Falha de autenticação - senha incorreta para email: {}", email);
            throw new SenhaInvalidaException();
        }

        logger.info("Usuário autenticado com sucesso: {}", email);
        return usuario;
    }
}
```

---

## 3️⃣ Implementar Handlers de Exceção Específicos

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/dto/ErrorResponse.java`

```java
package com.luizfernando.gestaousuarios.api.dto;

import java.time.LocalDateTime;

/**
 * DTO padronizado para respostas de erro
 */
public record ErrorResponse(
    String codigo,           // Código estruturado: "EMAIL_JA_CADASTRADO"
    String mensagem,         // Mensagem legível para o usuário
    LocalDateTime timestamp  // Quando ocorreu o erro
) {}
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/exceptionhandler/GlobalExceptionHandler.java` (ATUALIZADO)

```java
package com.luizfernando.gestaousuarios.api.exceptionhandler;

import com.luizfernando.gestaousuarios.api.dto.ErrorResponse;
import com.luizfernando.gestaousuarios.api.security.exception.TokenExpiredException;
import com.luizfernando.gestaousuarios.api.security.exception.TokenInvalidoException;
import com.luizfernando.gestaousuarios.domain.exception.EmailJaCadastradoException;
import com.luizfernando.gestaousuarios.domain.exception.UsuarioNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(
            EmailJaCadastradoException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "EMAIL_JA_CADASTRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        logger.warn("Email duplicado detectado: {}", ex.getEmail());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontrado(
            UsuarioNaoEncontradoException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "USUARIO_NAO_ENCONTRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        logger.warn("Usuário não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpirado(
            TokenExpiredException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "TOKEN_EXPIRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        logger.debug("Token expirado detectado");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalido(
            TokenInvalidoException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "TOKEN_INVALIDO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        logger.warn("Token inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex, WebRequest request) {
        String mensagens = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
            "VALIDACAO_ERRO",
            mensagens,
            LocalDateTime.now()
        );
        logger.debug("Validação de entrada falhou: {}", mensagens);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "ERRO_INTERNO",
            "Ocorreu um erro interno no servidor",
            LocalDateTime.now()
        );
        logger.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 4️⃣ Criar SecurityContextHelper

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/security/SecurityContextHelper.java`

```java
package com.luizfernando.gestaousuarios.api.security;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper para acessar contexto de segurança
 * Desacopla controllers do Spring Security
 * Facilita testes (pode ser mockado)
 */
@Component
public class SecurityContextHelper {

    /**
     * Obtém o usuário atualmente autenticado
     * 
     * @return Usuario logado
     * @throws SecurityException se não há usuário autenticado
     */
    public Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Usuário não autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new SecurityException("Principal não é um usuário válido");
        }
        
        return (Usuario) principal;
    }

    /**
     * Obtém o email do usuário logado
     */
    public String getEmailUsuarioLogado() {
        return getUsuarioLogado().getEmail();
    }

    /**
     * Obtém o ID do usuário logado
     */
    public Long getIdUsuarioLogado() {
        return getUsuarioLogado().getId();
    }
}
```

---

## 5️⃣ Criar DTOs de Resposta

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/dto/UsuarioResponseDTO.java`

```java
package com.luizfernando.gestaousuarios.api.dto;

import com.luizfernando.gestaousuarios.domain.model.Usuario;

import java.time.LocalDateTime;

/**
 * DTO para resposta de usuário
 * NÃO expõe senha ou dados sensíveis
 */
public record UsuarioResponseDTO(
    Long id,
    String nome,
    String email,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao
) {
    /**
     * Factory method para converter entidade em DTO
     */
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getDataCriacao(),
            usuario.getDataAtualizacao()
        );
    }
}
```

---

## 6️⃣ Externalizar Configurações

### Arquivo: `backend/src/main/resources/application.properties` (ADICIONAR)

```properties
# ===== SEGURANÇA JWT =====
api.security.token.secret=${SECURITY_TOKEN_SECRET:change-me-in-production}
api.security.token.expiration=7200

# ===== CORS =====
app.cors.allowed-origins=http://localhost:5173,https://app.example.com
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.max-age=3600

# ===== BCRYPT =====
app.bcrypt.strength=12

# ===== TIMEZONE =====
app.timezone=America/Sao_Paulo

# ===== LOGGING =====
logging.level.com.luizfernando.gestaousuarios=INFO
logging.level.org.springframework.security=DEBUG
```

### Arquivo: `.env` (NUNCA COMMITAR)

```bash
SECURITY_TOKEN_SECRET=seu-token-secreto-de-256-bits-aqui-gerado-criptograficamente
```

---

## 7️⃣ Implementar Paginação

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/domain/service/UsuarioService.java` (ATUALIZAR)

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public Page<UsuarioResponseDTO> listarComPaginacao(Pageable pageable) {
    return usuarioRepository.findAll(pageable)
        .map(UsuarioResponseDTO::fromEntity);
}
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/controller/UsuarioController.java` (ATUALIZAR)

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@GetMapping
public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(Pageable pageable) {
    return ResponseEntity.ok(usuarioService.listarComPaginacao(pageable));
}
```

**Uso:**
```
GET /api/usuarios?page=0&size=10&sort=id,desc
GET /api/usuarios?page=1&size=20&sort=nome,asc
```

---

## 8️⃣ Adicionar Validações Jakarta

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/dto/UsuarioRequestDTO.java` (ATUALIZAR)

```java
package com.luizfernando.gestaousuarios.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
    @NotBlank(message = "Nome não pode estar vazio")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    String nome,
    
    @NotBlank(message = "Email não pode estar vazio")
    @Email(message = "Email deve ser um endereço válido")
    String email,
    
    @NotBlank(message = "Senha não pode estar vazia")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    String senha
) {}
```

---

## 9️⃣ Implementar Rate Limiting

### Adicionar Dependência em `pom.xml`

```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/api/security/RateLimitingFilter.java`

```java
package com.luizfernando.gestaousuarios.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de rate limiting para proteger contra brute force
 * 10 requisições por minuto por IP
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String clientId = getClientIdentifier(request);
        String path = request.getRequestURI();

        // Aplicar rate limiting apenas em endpoints sensíveis
        if (isProtectedEndpoint(path)) {
            Bucket bucket = buckets.computeIfAbsent(clientId, k -> createNewBucket());

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\": \"Muitas requisições. Tente novamente mais tarde.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private Bucket createNewBucket() {
        // 10 requisições a cada 1 minuto
        return Bucket4j.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
            .build();
    }

    private boolean isProtectedEndpoint(String path) {
        return path.contains("/login") || path.contains("/cadastro");
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
```

---

## 🔟 Adicionar Swagger/OpenAPI

### Adicionar Dependência em `pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Arquivo: `backend/src/main/java/com/luizfernando/gestaousuarios/config/OpenApiConfig.java`

```java
package com.luizfernando.gestaousuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Sistema de Gestão de Usuários")
                .version("1.0.0")
                .description("API para gerenciamento de usuários com autenticação JWT"));
    }
}
```

### Acessar em: `http://localhost:8080/swagger-ui.html`

---

## 📋 Checklist de Implementação

```
[ ] 1. Criar exceções customizadas
[ ] 2. Criar AuthenticationService
[ ] 3. Implementar handlers de exceção
[ ] 4. Criar SecurityContextHelper
[ ] 5. Criar DTOs de resposta
[ ] 6. Externalizar configurações
[ ] 7. Implementar paginação
[ ] 8. Adicionar validações Jakarta
[ ] 9. Implementar rate limiting
[ ] 10. Adicionar Swagger/OpenAPI

[ ] Testes unitários
[ ] Testes de integração
[ ] Testes de segurança
[ ] Code review das alterações
[ ] Deploy em staging
[ ] Deploy em produção
```

---

## 🧪 Exemplo de Teste Unitário

```java
@SpringBootTest
class AuthenticationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void deveAutenticarUsuarioComCredenciaisValidas() {
        // Arrange
        String email = "teste@example.com";
        String senha = "senha123";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha("hashedPassword");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senha, usuario.getSenha())).thenReturn(true);

        // Act
        Usuario resultado = authenticationService.autenticar(email, senha);

        // Assert
        assertEquals(email, resultado.getEmail());
        verify(usuarioRepository).findByEmail(email);
    }

    @Test
    void deveLancarEmailNaoEncontradoExceptionParaEmailInexistente() {
        // Arrange
        String email = "inexistente@example.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmailNaoEncontradoException.class, 
            () -> authenticationService.autenticar(email, "qualquersenha"));
    }
}
```

---

Este documento fornece exemplos práticos e prontos para implementação das melhorias sugeridas no code review!


# Code Review - Sistema de Gestão de Usuários

## 📋 Comentários Detalhados de Arquitetura e Padrões

---

## 1️⃣ UsuarioController.java

### Comentário 1: Lógica de Criação em Controller (Linha 28-35)

**🔍 Sugestão de melhoria: Padrão Builder e DTO Mapping**

Atualmente, a criação do objeto `Usuario` é feita diretamente no controller:
```java
Usuario novoUsuario = new Usuario();
novoUsuario.setNome(dto.nome());
novoUsuario.setEmail(dto.email());
novoUsuario.setSenha(dto.senha());
```

**Problemas:**
- Viola o princípio Single Responsibility Principle (SRP) - o controller está fazendo mapeamento de DTO
- Lógica de negócio espalhada entre controller e service
- Dificulta testes unitários

**Benefícios da mudança:**
- Centralizar a lógica de construção de entidades no service
- Facilitar validações na construção do usuário
- Melhorar testabilidade isolando a lógica de negócio

**📌 Sugestão de implementação:**

No `UsuarioService`, adicione um método de fábrica:
```java
public Usuario criarNovoUsuario(UsuarioRequestDTO dto) {
    // Validações aqui
    Usuario usuario = new Usuario();
    usuario.setNome(dto.nome());
    usuario.setEmail(dto.email());
    usuario.setSenha(dto.senha());
    return usuario;
}
```

No controller, simplificar para:
```java
@PostMapping
public ResponseEntity<Usuario> registrarUsuario(@RequestBody UsuarioRequestDTO dto) {
    Usuario usuarioSalvo = usuarioService.criarEsalvarUsuario(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
}
```

---

### Comentário 2: Falta de Validação de Input (Linha 28)

**🔍 Sugestão: Implementar Jakarta Validation**

O `@RequestBody UsuarioRequestDTO dto` não está validado. Se o cliente enviar dados inválidos (email vazio, nome nulo), o sistema não valida na entrada.

**Problemas:**
- Validações são responsabilidade da camada de controller/dto
- Dados inconsistentes chegam no service
- Mensagens de erro genéricas

**Benefícios:**
- Falha rápido (fail-fast) na entrada de dados
- Mensagens de erro claras e consistentes
- Reduz lógica de validação no service

**📌 Sugestão de implementação:**

Em `UsuarioRequestDTO`:
```java
public record UsuarioRequestDTO(
    @NotBlank(message = "Nome não pode estar vazio")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    String nome,
    
    @NotBlank(message = "Email não pode estar vazio")
    @Email(message = "Email deve ser válido")
    String email,
    
    @NotBlank(message = "Senha não pode estar vazia")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    String senha
) {}
```

No controller, adicionar `@Valid`:
```java
@PostMapping
public ResponseEntity<Usuario> registrarUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
    // ...
}
```

---

### Comentário 3: Responsabilidade do Controller no Endpoint PUT (Linha 46-61)

**🔍 Sugestão: Extrair Lógica de Contexto de Segurança**

Atualmente, o controller está acessando diretamente o `SecurityContextHolder`:
```java
Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext()
    .getAuthentication().getPrincipal();
```

**Problemas:**
- Controller acoplado ao framework de segurança do Spring
- Dificulta testes (precisa mockar SecurityContext)
- Lógica de segurança espalhada no código
- Casting sem segurança de tipo

**Benefícios da mudança:**
- Desacoplar lógica de segurança do controller
- Facilitar testes unitários
- Código mais legível e mantível

**📌 Sugestão de implementação:**

Criar um serviço helper:
```java
@Component
public class SecurityContextHelper {
    public Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        throw new SecurityException("Usuário não autenticado");
    }
    
    public String getEmailUsuarioLogado() {
        return getUsuarioLogado().getEmail();
    }
}
```

No controller, agora:
```java
@PutMapping("/perfil")
public ResponseEntity<Usuario> atualizarMeuPerfil(@Valid @RequestBody AtualizarUsuarioDTO dto) {
    String emailLogado = securityContextHelper.getEmailUsuarioLogado();
    Usuario usuarioAtualizado = usuarioService.atualizarPerfil(emailLogado, dto);
    return ResponseEntity.ok(usuarioAtualizado);
}
```

---

### Comentário 4: Falta de Paginação no Endpoint de Listagem (Linha 40-42)

**🔍 Sugestão: Implementar Paginação e Ordenação**

O endpoint `GET /api/usuarios` retorna TODOS os usuários sem paginação:
```java
@GetMapping
public ResponseEntity<List<Usuario>> listarUsuarios() {
    return ResponseEntity.ok(usuarioService.listarTodos());
}
```

**Problemas:**
- Se houver 10.000 usuários, o sistema retorna todos de uma vez (Out of Memory)
- Sem ordenação, os resultados são imprevisíveis
- Não segue padrões REST modernos
- Performance ruim em aplicações com muitos dados

**Benefícios:**
- Melhor performance em grandes volumes de dados
- Economia de memória no servidor e cliente
- Experiência melhor no frontend

**📌 Sugestão de implementação:**

```java
@GetMapping
public ResponseEntity<Page<UsuarioDTO>> listarUsuarios(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") Sort.Direction direction
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    Page<UsuarioDTO> usuarios = usuarioService.listarComPaginacao(pageable);
    return ResponseEntity.ok(usuarios);
}
```

No service:
```java
public Page<UsuarioDTO> listarComPaginacao(Pageable pageable) {
    return usuarioRepository.findAll(pageable)
        .map(this::converterParaDTO);
}
```

---

### Comentário 5: Falta de Tratamento de Erro Específico para Deleção (Linha 64-67)

**🔍 Sugestão: Criar Exceções Customizadas ao invés de RuntimeException**

Atualmente tudo usa `RuntimeException`:
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
    usuarioService.deletarUsuario(id);
    return ResponseEntity.noContent().build();
}
```

E no service:
```java
if (!usuarioRepository.existsById(id)) {
    throw new RuntimeException("Utilizador não encontrado.");
}
```

**Problemas:**
- `RuntimeException` é genérica demais
- Dificulta tratamento diferenciado de erros
- Mensagens de erro inconsistentes
- Não segue convenções de exception handling

**Benefícios:**
- Exceções específicas para cada tipo de erro
- Handler específico para cada exceção
- Melhor tratamento de erros no frontend
- Código mais limpo e legível

**📌 Sugestão de implementação:**

Criar exceções customizadas:
```java
public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário com ID " + id + " não encontrado.");
    }
}

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("Email " + email + " já está cadastrado no sistema.");
    }
}
```

Atualizar `GlobalExceptionHandler`:
```java
@ExceptionHandler(UsuarioNaoEncontradoException.class)
public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("USUARIO_NAO_ENCONTRADO", ex.getMessage()));
}

@ExceptionHandler(EmailJaCadastradoException.class)
public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("EMAIL_JA_CADASTRADO", ex.getMessage()));
}
```

---

## 2️⃣ UsuarioService.java

### Comentário 6: Lógica de Autenticação Deveria estar em Serviço Separado

**🔍 Sugestão: Padrão Strategy + Serviço de Autenticação**

Atualmente o serviço contém lógica de autenticação misturada com lógica de usuário:
```java
public Usuario autenticar(String email, String senha) {
    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos."));
    if (!passwordEncoder.matches(senha, usuario.getSenha())) {
        throw new RuntimeException("E-mail ou senha inválidos.");
    }
    return usuario;
}
```

**Problemas:**
- Viola SRP - o serviço faz autenticação E gerenciamento de usuários
- Dificulta testes (teste de autenticação acoplado a testes de usuário)
- Difícil reutilizar lógica de autenticação em outros contextos
- Se precisar adicionar autenticação por OAuth/LDAP, fica complicado

**Benefícios:**
- Separação de responsabilidades clara
- Facilita testes unitários
- Permite múltiplas estratégias de autenticação
- Código mais reutilizável

**📌 Sugestão de implementação:**

Criar `AuthenticationService`:
```java
@Service
public class AuthenticationService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new EmailNaoEncontradoException(email));
        
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new SenhaInvalidaException();
        }
        
        return usuario;
    }
}
```

Remover do `UsuarioService` e injetar o novo serviço no `AuthController`:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        Usuario usuario = authenticationService.autenticar(dto.email(), dto.senha());
        String token = tokenService.gerarToken(usuario);
        return ResponseEntity.ok(new LoginResponseDTO(usuario.getNome(), token));
    }
}
```

---

### Comentário 7: Método `atualizarPerfil` Deveria Receber DTO ao invés de Múltiplos Parâmetros

**🔍 Sugestão: Padrão DTO e Redução de Parâmetros**

Atualmente o método tem muitos parâmetros:
```java
public Usuario atualizarPerfil(String emailLogado, String novoNome, String novoEmail, String novaSenha) {
    // ...
}
```

**Problemas:**
- Method Signature muito longa (code smell)
- Dificulta manutenção - adicionar novo campo requer mudar assinatura
- Sem encapsulamento dos dados de atualização
- Difícil passar validações no DTO

**Benefícios:**
- Assinatura clara e concisa
- Fácil adicionar novos campos
- Validações no DTO
- Melhor documentação implícita

**📌 Sugestão de implementação:**

```java
// No controller:
@PutMapping("/perfil")
public ResponseEntity<Usuario> atualizarMeuPerfil(@Valid @RequestBody AtualizarUsuarioDTO dto) {
    String emailLogado = securityContextHelper.getEmailUsuarioLogado();
    Usuario usuarioAtualizado = usuarioService.atualizarPerfil(emailLogado, dto);
    return ResponseEntity.ok(usuarioAtualizado);
}

// No service:
public Usuario atualizarPerfil(String emailLogado, AtualizarUsuarioDTO dto) {
    Usuario usuario = usuarioRepository.findByEmail(emailLogado)
        .orElseThrow(() -> new UsuarioNaoEncontradoException(emailLogado));
    
    if (!usuario.getEmail().equals(dto.email()) && 
        usuarioRepository.findByEmail(dto.email()).isPresent()) {
        throw new EmailJaCadastradoException(dto.email());
    }
    
    usuario.setNome(dto.nome());
    usuario.setEmail(dto.email());
    
    if (dto.senha() != null && !dto.senha().trim().isEmpty()) {
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
    }
    
    return usuarioRepository.save(usuario);
}
```

---

### Comentário 8: Falta de Logging em Operações Críticas

**🔍 Sugestão: Adicionar Logs para Auditoria e Debug**

Não há logs nas operações críticas (login, criação, deleção de usuário).

**Problemas:**
- Impossível rastrear ações no sistema
- Sem informações para debug em produção
- Sem auditoria de operações sensíveis
- Difícil detectar tentativas de ataque

**Benefícios:**
- Rastreabilidade completa de operações
- Facilita debug e investigação de problemas
- Conformidade com regulamentações (LGPD, GDPR)
- Detecção de anomalias

**📌 Sugestão de implementação:**

```java
@Service
public class UsuarioService {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    
    public Usuario salvarUsuario(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            logger.warn("Tentativa de cadastro com email duplicado: {}", usuario.getEmail());
            throw new EmailJaCadastradoException(usuario.getEmail());
        }
        
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        logger.info("Novo usuário cadastrado: {} (ID: {})", usuario.getEmail(), usuarioSalvo.getId());
        
        return usuarioSalvo;
    }
    
    public void deletarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        
        usuarioRepository.deleteById(id);
        logger.warn("Usuário deletado: {} (ID: {})", usuario.getEmail(), id);
    }
}
```

---

### Comentário 9: Falta de Validação de Email Duplicate Check

**🔍 Sugestão: Reutilizar Método Existente no Repository**

O repository tem `existsByEmail()` mas não é usado:
```java
// No repository
boolean existsByEmail(String email);

// Mas no service, faz:
if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
```

**Problemas:**
- Duas queries diferentes para a mesma verificação
- Performance ruim (findByEmail carrega o objeto inteiro)
- Código inconsistente
- Pode gerar queries N+1 em cenários de bulk

**Benefícios:**
- Performance otimizada (SELECT COUNT(*) ao invés de SELECT *)
- Código mais limpo
- Query mais eficiente no banco de dados

**📌 Sugestão de implementação:**

```java
public Usuario salvarUsuario(Usuario usuario) {
    if (usuarioRepository.existsByEmail(usuario.getEmail())) {
        throw new EmailJaCadastradoException(usuario.getEmail());
    }
    
    usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
    return usuarioRepository.save(usuario);
}

public Usuario atualizarPerfil(String emailLogado, AtualizarUsuarioDTO dto) {
    Usuario usuario = usuarioRepository.findByEmail(emailLogado)
        .orElseThrow(() -> new UsuarioNaoEncontradoException(emailLogado));
    
    if (!usuario.getEmail().equals(dto.email()) && 
        usuarioRepository.existsByEmail(dto.email())) {
        throw new EmailJaCadastradoException(dto.email());
    }
    
    // ... resto do código
}
```

---

## 3️⃣ TokenService.java

### Comentário 10: Hardcoded Secret Key em Padrão Inseguro

**🔍 Sugestão: Usar Gerenciador de Secrets Externo**

Atualmente usa um valor padrão inseguro:
```java
@Value("${api.security.token.secret:minha-chave-secreta-super-protegida}")
private String secret;
```

**Problemas:**
- Default value exposto no código
- Chave fraca não é criptograficamente segura
- Risco de exposição em repositório público
- Não segue práticas de segurança

**Benefícios:**
- Chave segura gerada criptograficamente
- Não exposta no código-fonte
- Fácil rotacionar a chave em produção
- Conformidade com standards de segurança

**📌 Sugestão de implementação:**

Em `application.properties` (NÃO commitar):
```properties
api.security.token.secret=${SECURITY_TOKEN_SECRET}
api.security.token.expiration=7200
```

Ou usar Spring Cloud Config/Vault. No serviço:
```java
@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;
    
    @Value("${api.security.token.expiration:7200}")
    private long expirationSeconds;
    
    public String gerarToken(Usuario usuario) {
        // ... resto do código
    }
}
```

---

### Comentário 11: Tratamento Genérico de Exceções JWT

**🔍 Sugestão: Separar Exceções JWT Específicas**

Atualmente trata qualquer erro como "Token inválido ou expirado":
```java
public String getSubject(String tokenJWT) {
    try {
        // ...
    } catch (JWTVerificationException exception) {
        throw new RuntimeException("Token JWT inválido ou expirado!");
    }
}
```

**Problemas:**
- Não diferencia entre token expirado e token corrompido
- Frontend não consegue tomar ações diferentes
- Mensagens de erro genéricas
- Difícil debug

**Benefícios:**
- Trata diferentes cenários de forma apropriada
- Frontend pode fazer refresh token ou redirecionar login
- Melhor experiência de usuário
- Mais fácil debug

**📌 Sugestão de implementação:**

```java
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token JWT expirado");
    }
}

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String message) {
        super("Token JWT inválido: " + message);
    }
}

@Service
public class TokenService {
    public String getSubject(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                .withIssuer("gestao-usuarios-api")
                .build()
                .verify(tokenJWT)
                .getSubject();
        } catch (JWTVerificationException exception) {
            if (exception.getMessage().contains("Signature verification failed")) {
                throw new TokenInvalidoException("Assinatura inválida");
            } else if (exception.getMessage().contains("The token is expired")) {
                throw new TokenExpiredException();
            }
            throw new TokenInvalidoException(exception.getMessage());
        }
    }
}
```

---

### Comentário 12: Sem Validação de Algorithm no JWT

**🔍 Sugestão: Validar Algorithm explicitamente**

Não há validação se o algoritmo usado é o esperado (vulnerabilidade conhecida de JWT).

**Problemas:**
- Vulnerabilidade CVE - alguém pode mandar um token com algoritmo "none"
- Sem proteção contra algorithm substitution attacks
- Não segue OWASP top 10

**Benefícios:**
- Proteção contra conhecida vulnerabilidade de JWT
- Garantia que apenas HMAC256 é aceito
- Segurança em camadas

**📌 Sugestão de implementação:**

```java
public String getSubject(String tokenJWT) {
    try {
        Algorithm algoritmo = Algorithm.HMAC256(secret);
        
        DecodedJWT decodedJWT = JWT.require(algoritmo)
            .withIssuer("gestao-usuarios-api")
            .build()
            .verify(tokenJWT);
        
        // Verificar explicitamente o algoritmo
        String algorithmValue = decodedJWT.getAlgorithm();
        if (!"HS256".equals(algorithmValue)) {
            throw new TokenInvalidoException("Algoritmo não permitido: " + algorithmValue);
        }
        
        return decodedJWT.getSubject();
    } catch (JWTVerificationException exception) {
        // ... tratamento de erro
    }
}
```

---

## 4️⃣ SecurityFilter.java

### Comentário 13: Tratamento de Exceção Não Implementado no Filter

**🔍 Sugestão: Try-Catch com Tratamento Apropriado**

Se o token for inválido, lança RuntimeException que não é tratada:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    var tokenJWT = recuperarToken(request);
    if (tokenJWT != null) {
        var email = tokenService.getSubject(tokenJWT); // Pode falhar!
        Usuario usuario = repository.findByEmail(email).orElseThrow(...); // Pode falhar!
        // ...
    }
    filterChain.doFilter(request, response);
}
```

**Problemas:**
- Se token inválido, erro 500 é retornado
- Sem mensagem clara para o client
- Não segue padrão de erros da API

**Benefícios:**
- Tratamento limpo de erros de autenticação
- Response HTTP apropriada (401 Unauthorized)
- Consistência com GlobalExceptionHandler

**📌 Sugestão de implementação:**

```java
@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            var tokenJWT = recuperarToken(request);
            
            if (tokenJWT != null) {
                var email = tokenService.getSubject(tokenJWT);
                Usuario usuario = repository.findByEmail(email)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException(email));
                
                var authentication = new UsernamePasswordAuthenticationToken(
                    usuario, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
            filterChain.doFilter(request, response);
        } catch (TokenExpiredException ex) {
            logger.warn("Token expirado");
            responderComErro(response, HttpStatus.UNAUTHORIZED, "Token expirado");
        } catch (TokenInvalidoException ex) {
            logger.warn("Token inválido: {}", ex.getMessage());
            responderComErro(response, HttpStatus.UNAUTHORIZED, "Token inválido");
        } catch (UsuarioNaoEncontradoException ex) {
            logger.warn("Usuário não encontrado no token");
            responderComErro(response, HttpStatus.UNAUTHORIZED, "Usuário não encontrado");
        }
    }
    
    private void responderComErro(HttpServletResponse response, HttpStatus status, String mensagem) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(
            String.format("{\"erro\": \"%s\"}", mensagem)
        );
    }
    
    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}
```

---

### Comentário 14: Falta de Validação de Prefix "Bearer " no Token

**🔍 Sugestão: Validar Formato do Header Authorization**

O método `recuperarToken` simplesmente remove "Bearer " sem validar:
```java
private String recuperarToken(HttpServletRequest request) {
    var authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null) {
        return authorizationHeader.replace("Bearer ", ""); // Sem validação!
    }
    return null;
}
```

**Problemas:**
- Se o header for "Bearer", retorna string vazia
- Se for "BearerXXXX" (sem espaço), não é tratado
- Sem mensagens de erro claras

**Benefícios:**
- Validação clara do formato
- Erros mais informativos
- Evita bugs silenciosos

**📌 Sugestão de implementação:**

```java
private String recuperarToken(HttpServletRequest request) {
    var authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
        return null;
    }
    
    if (!authorizationHeader.startsWith("Bearer ")) {
        throw new TokenInvalidoException("Authorization header deve estar no formato 'Bearer <token>'");
    }
    
    String token = authorizationHeader.substring(7); // Remove "Bearer "
    if (token.isBlank()) {
        throw new TokenInvalidoException("Token não pode estar vazio");
    }
    
    return token;
}
```

---

## 5️⃣ GlobalExceptionHandler.java

### Comentário 15: Handler Genérico Trata Todas as RuntimeException Igualmente

**🔍 Sugestão: Handlers Específicos para Diferentes Tipos de Erro**

Atualmente um único handler trata tudo:
```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<Map<String, String>> handleRegraDeNegocioException(RuntimeException ex) {
    Map<String, String> resposta = new HashMap<>();
    resposta.put("erro", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
}
```

**Problemas:**
- Todas as exceções retornam 400 Bad Request
- Erro de usuário não encontrado deveria ser 404
- Erro de email duplicado deveria ser 409 Conflict
- Sem padronização de resposta

**Benefícios:**
- HTTP status codes corretos
- Mensagens de erro estruturadas
- Fácil tratamento no frontend
- Conformidade com REST

**📌 Sugestão de implementação:**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        ErrorResponse error = new ErrorResponse(
            "EMAIL_JA_CADASTRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        ErrorResponse error = new ErrorResponse(
            "USUARIO_NAO_ENCONTRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpirado(TokenExpiredException ex) {
        ErrorResponse error = new ErrorResponse(
            "TOKEN_EXPIRADO",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String messages = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            "VALIDACAO_ERRO",
            messages,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            "ERRO_INTERNO",
            "Ocorreu um erro interno no servidor",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Classe para padronizar resposta de erro
public record ErrorResponse(
    String codigo,
    String mensagem,
    LocalDateTime timestamp
) {}
```

---

### Comentário 16: Falta de Logging de Erros

**🔍 Sugestão: Adicionar Logging em GlobalExceptionHandler**

Não há logs quando exceções ocorrem:

**Problemas:**
- Impossível rastrear erros em produção
- Sem informações para debug
- Sem alertas para problemas críticos

**Benefícios:**
- Rastreamento completo de erros
- Facilita investigação de problemas
- Integração com ferramentas de monitoramento (Sentry, ELK)

**📌 Sugestão de implementação:**

Adicionar logging a cada handler:
```java
private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(EmailJaCadastradoException.class)
public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
    logger.warn("Tentativa de cadastro com email duplicado: {}", ex.getMessage());
    // ... resto do código
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    logger.error("Erro interno não tratado", ex);
    // ... resto do código
}
```

---

## 6️⃣ SecurityConfig.java

### Comentário 17: CORS Hardcoded em ConfiguraçãoJava

**🔍 Sugestão: Externalizar Configuração de CORS**

Atualmente hardcoded:
```java
configuration.setAllowedOrigins(List.of("http://localhost:5173"));
```

**Problemas:**
- Requer recompilação para mudar origem
- Diferentes valores para dev/prod
- Não segue 12-factor app
- Difícil variações por ambiente

**Benefícios:**
- Fácil configurar diferentes ambientes
- Sem recompilação necessária
- Segurança em produção

**📌 Sugestão de implementação:**

Em `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:5173,https://app.example.com
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.max-age=3600
```

No `SecurityConfig`:
```java
@Configuration
public class SecurityConfig {
    
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            Arrays.asList(allowedOrigins.split(","))
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(corsMaxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

### Comentário 18: CSRF Desativado Sem Alternativa Implementada

**🔍 Sugestão: Documentar a Decisão e Implementar Segurança Adicional**

```java
.csrf(csrf -> csrf.disable()) // Comentário genérico "porque vou usar JWT"
```

**Problemas:**
- CSRF ainda é risco mesmo com JWT
- Sem documentação clara da decisão arquitetural
- Sem proteção adicional implementada

**Benefícios:**
- Segurança em camadas (defense in depth)
- Documentação clara para futuros developers
- Proteção contra múltiplos vetores de ataque

**📌 Sugestão de implementação:**

```java
@Configuration
public class SecurityConfig {
    /**
     * CSRF foi desabilitado pois a aplicação usa JWT para autenticação.
     * JWT tokens são imunes a CSRF pois:
     * 1. Devem ser enviados no header Authorization (não em cookies)
     * 2. SameSite attribute protege cookies convencionais
     * 3. O cliente (React) controla o envio do token
     * 
     * Segurança adicional implementada:
     * - Token expira em 2 horas
     * - Rate limiting recomendado no proxy/load balancer
     * - HTTPS obrigatório em produção
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // JWT é imune a CSRF
            // ... resto do código
}
```

---

### Comentário 19: Falta de Rate Limiting

**🔍 Sugestão: Implementar Rate Limiting para Proteção contra Brute Force**

Sem proteção contra ataques de força bruta em endpoints de login/cadastro.

**Problemas:**
- Vulnerável a brute force attacks em login
- Sem proteção contra abuso de endpoints
- Possível comprometimento de contas

**Benefícios:**
- Proteção contra brute force
- Melhor segurança do sistema
- Compliance com boas práticas de segurança

**📌 Sugestão de implementação:**

Adicionar dependência:
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

Criar filtro de rate limiting:
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String clientId = getClientIdentifier(request);
        
        if (isProtectedEndpoint(request)) {
            Bucket bucket = buckets.computeIfAbsent(clientId, k -> createNewBucket());
            
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\": \"Muitas requisições. Tente novamente em 1 minuto.\"}");
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
    
    private boolean isProtectedEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/login") || path.contains("/cadastro");
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        return request.getHeader("X-Forwarded-For") != null 
            ? request.getHeader("X-Forwarded-For").split(",")[0]
            : request.getRemoteAddr();
    }
}
```

---

## 7️⃣ Usuario.java (Model)

### Comentário 20: Falta de Validação na Entidade

**🔍 Sugestão: Adicionar Validações Jakarta Validation**

A entidade não tem validações:
```java
@Column(nullable = false)
private String nome;
```

**Problemas:**
- Validações no nível de constraint SQL, não no código
- Sem mensagens de erro customizadas
- Sem validação em memória
- Difícil reutilizar validações em DTOs

**Benefícios:**
- Validações no modelo, no DTO e no banco
- Mensagens claras
- Fácil reutilização
- Fail-fast

**📌 Sugestão de implementação:**

```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome não pode estar vazio")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false)
    private String nome;
    
    @NotBlank(message = "Email não pode estar vazio")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true)
    private String email;
    
    @NotBlank(message = "Senha não pode estar vazia")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Column(nullable = false)
    private String senha;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;
    
    // ... resto do código
}
```

---

### Comentário 21: Falta de Auditoria (CreatedAt/UpdatedAt)

**🔍 Sugestão: Adicionar Campos de Auditoria**

Não há rastreamento de quando usuários foram criados/atualizados.

**Problemas:**
- Impossível rastrear histórico
- Sem informações de auditoria
- Difícil investigar quando dados mudaram

**Benefícios:**
- Auditoria completa
- Histórico de alterações
- Conformidade com regulamentações
- Investigação de problemas facilitada

**📌 Já implementado na sugestão anterior com `@CreationTimestamp` e `@UpdateTimestamp`**

---

### Comentário 22: Implementação de UserDetails para Segurança

**🔍 Sugestão: Implementar UserDetails do Spring Security**

Atualmente a entidade Usuario é usada como principal mas não implementa UserDetails:

**Problemas:**
- Mistura responsabilidades (entidade JPA + Spring Security)
- Sem autorizações/roles implementadas
- Casting sem segurança de tipo em SecurityFilter

**Benefícios:**
- Integração correta com Spring Security
- Suporte para autorização
- Código mais robusto
- Padrão esperado pelo framework

**📌 Sugestão de implementação:**

```java
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(min = 3, max = 100)
    @Column(nullable = false)
    private String nome;
    
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;
    
    @NotBlank
    @Column(nullable = false)
    private String senha;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;
    
    // Implementação de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        return this.senha;
    }
    
    @Override
    public String getUsername() {
        return this.email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    // ... getters e setters
}

// Enum para roles
public enum Role {
    ADMIN,
    USER
}
```

---

## 8️⃣ Recomendações Arquiteturais Gerais

### Comentário 23: Falta de DTOs de Resposta

**🔍 Sugestão: Usar DTOs Separados para Requests/Responses**

Atualmente o controller retorna a entidade Usuario diretamente:
```java
return ResponseEntity.ok(usuarioService.listarTodos());
```

**Problemas:**
- Expõe detalhes internos da entidade (pode incluir senha em futuro)
- Difícil versionar API (adicionar campo quebra clientes)
- Sem controle de quais campos expor
- Tight coupling com entidade

**Benefícios:**
- Controle total sobre o que é exposto
- Facilita versionamento
- Segurança (não expõe campos internos)
- Flexibilidade

**📌 Sugestão de implementação:**

```java
// UsuarioResponseDTO.java
public record UsuarioResponseDTO(
    Long id,
    String nome,
    String email,
    LocalDateTime dataCriacao
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getDataCriacao()
        );
    }
}

// No controller
@GetMapping
public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(Pageable pageable) {
    return ResponseEntity.ok(
        usuarioService.listarComPaginacao(pageable)
            .map(UsuarioResponseDTO::fromEntity)
    );
}

@PostMapping
public ResponseEntity<UsuarioResponseDTO> registrarUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
    Usuario usuarioSalvo = usuarioService.criarESalvarUsuario(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UsuarioResponseDTO.fromEntity(usuarioSalvo));
}
```

---

### Comentário 24: Falta de Documentação OpenAPI/Swagger

**🔍 Sugestão: Adicionar Swagger/OpenAPI para Documentação de API**

Sem documentação automática de endpoints:

**Problemas:**
- Frontend developer precisa adivinhar endpoints
- Sem documentação de parâmetros
- Sem documentação de respostas de erro
- Difficuldade para onboarding

**Benefícios:**
- Documentação automática e atualizada
- Frontend consegue testar endpoints facilmente
- Self-documenting API
- Integração com ferramentas

**📌 Sugestão de implementação:**

Adicionar dependência:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
```

Adicionar anotações:
```java
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários")
public class UsuarioController {
    
    @PostMapping
    @Operation(summary = "Cadastrar novo usuário")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(
        @Valid @RequestBody UsuarioRequestDTO dto) {
        // ...
    }
    
    @GetMapping
    @Operation(summary = "Listar todos os usuários com paginação")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(
        @ParameterObject Pageable pageable) {
        // ...
    }
}
```

---

### Comentário 25: Padrão CQRS/Event Sourcing não Implementado

**🔍 Sugestão: Considerar Event Sourcing para Auditoria Crítica (Futuro)**

Para aplicações com requisitos críticos de auditoria, considere Event Sourcing.

**Problemas Atuais:**
- Sem histórico completo de alterações
- Impossível saber exatamente o que mudou quando
- Dados deletados são perdidos

**Benefícios:**
- Auditoria completa e imutável
- Reconstrução do estado em qualquer ponto no tempo
- Rastreamento de quem fez o quê quando

**📌 Sugestão (para futuro):**

Usar bibliotecas como Axon Framework ou implementar padrão simples de event log.

---

## 📊 Resumo dos Comentários

| # | Arquivo | Tipo | Prioridade |
|---|---------|------|-----------|
| 1 | UsuarioController | Refatoração | Alta |
| 2 | UsuarioController | Validação | Alta |
| 3 | UsuarioController | Segurança | Alta |
| 4 | UsuarioController | Performance | Média |
| 5 | UsuarioController | Padrão | Média |
| 6 | UsuarioService | Refatoração | Alta |
| 7 | UsuarioService | Refatoração | Média |
| 8 | UsuarioService | Auditoria | Média |
| 9 | UsuarioService | Performance | Baixa |
| 10 | TokenService | Segurança | Alta |
| 11 | TokenService | Tratamento Erro | Média |
| 12 | TokenService | Segurança | Alta |
| 13 | SecurityFilter | Tratamento Erro | Média |
| 14 | SecurityFilter | Validação | Média |
| 15 | GlobalExceptionHandler | Refatoração | Alta |
| 16 | GlobalExceptionHandler | Auditoria | Média |
| 17 | SecurityConfig | Configuração | Média |
| 18 | SecurityConfig | Documentação | Baixa |
| 19 | SecurityConfig | Segurança | Alta |
| 20 | Usuario (Model) | Validação | Média |
| 21 | Usuario (Model) | Auditoria | Média |
| 22 | Usuario (Model) | Spring Integration | Média |
| 23 | Geral | Refatoração | Média |
| 24 | Geral | Documentação | Baixa |
| 25 | Geral | Arquitetura | Baixa |

---

## 🎯 Próximos Passos Recomendados

1. **Curto Prazo (Sprint Atual):**
   - Implementar exceções customizadas (#5, #11, #12)
   - Adicionar validações com Jakarta Validation (#2, #20)
   - Separar serviço de autenticação (#6)

2. **Médio Prazo (Próximas Sprints):**
   - Refatorar handlers de exceção (#15)
   - Implementar rate limiting (#19)
   - Adicionar DTOs de resposta (#23)
   - Adicionar logging (#8, #16)

3. **Longo Prazo:**
   - Adicionar Swagger/OpenAPI (#24)
   - Considerar Event Sourcing para auditoria (#25)
   - Melhorar segurança geral (configurações de produção)


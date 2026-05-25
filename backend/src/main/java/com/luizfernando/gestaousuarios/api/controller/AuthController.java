package com.luizfernando.gestaousuarios.api.controller;

import com.luizfernando.gestaousuarios.api.dto.LoginRequestDTO;
import com.luizfernando.gestaousuarios.api.dto.LoginResponseDTO;
import com.luizfernando.gestaousuarios.core.security.TokenService;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ BOM: Uso de injeção de dependências via construtor
 * Evita field injection (@Autowired) e facilita testes
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    // Injeção de dependências via construtor
    public AuthController(UsuarioService usuarioService, TokenService tokenService) {
        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
    }

    /**
     * ❌ PROBLEMA DE ARQUITETURA: UsuarioService contém lógica de autenticação
     *
     * 💡 SUGESTÃO: Criar AuthenticationService separado
     * O UsuarioService está fazendo duas coisas: gerenciar usuários E autenticar.
     * Isso viola o Single Responsibility Principle (SRP).
     *
     * REFATORAÇÃO SUGERIDA:
     * 1. Criar AuthenticationService que encapsula lógica de autenticação
     * 2. Criar exceções específicas: EmailNaoEncontradoException, SenhaInvalidaException
     * 3. Deixar UsuarioService apenas para operações CRUD
     *
     * NOVA ESTRUTURA:
     * @Service
     * public class AuthenticationService {
     *     public Usuario autenticar(String email, String senha) {
     *         Usuario usuario = usuarioRepository.findByEmail(email)
     *             .orElseThrow(() -> new EmailNaoEncontradoException(email));
     *         if (!passwordEncoder.matches(senha, usuario.getSenha())) {
     *             throw new SenhaInvalidaException();
     *         }
     *         return usuario;
     *     }
     * }
     *
     * NO CONTROLLER:
     * @PostMapping("/login")
     * public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
     *     Usuario usuario = authenticationService.autenticar(dto.email(), dto.senha());
     *     String token = tokenService.gerarToken(usuario);
     *     return ResponseEntity.ok(new LoginResponseDTO(usuario.getNome(), token));
     * }
     *
     * BENEFÍCIOS:
     * - Responsabilidades bem definidas
     * - Fácil testar autenticação isoladamente
     * - Preparado para adicionar OAuth, LDAP, etc no futuro
     * - Reutilizar AuthenticationService em outros contextos
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        // ⚠️ VALIDAÇÃO FALTANDO: @Valid não está sendo usado no parâmetro
        // LoginRequestDTO precisa ter validações: @NotBlank, @Email, etc
        // Adicionar: public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto)

        /**
         * ⚠️ TRATAMENTO DE ERRO: RuntimeException genérica será convertida para HTTP 400
         *
         * 💡 SUGESTÃO: Usar exceções específicas de domínio
         * Problemas:
         * - Impossível diferenciar "email não existe" de "senha incorreta"
         * - Frontend não consegue tomar ações diferentes
         * - Mensagens genéricas (não diz qual é o problema)
         *
         * REFATORAÇÃO:
         * try {
         *     Usuario usuario = authenticationService.autenticar(dto.email(), dto.senha());
         * } catch (EmailNaoEncontradoException e) {
         *     // Log para auditoria (tentativa de login com email inexistente)
         *     // Retornar mensagem genérica por segurança
         * } catch (SenhaInvalidaException e) {
         *     // Log para auditoria (falha de senha)
         *     // Incrementar contador de tentativas falhadas (rate limiting)
         * }
         *
         * BENEFÍCIOS:
         * - Tratamento diferenciado por tipo de erro
         * - Possibilidade de implementar rate limiting em falhas
         * - Logs estruturados para auditoria
         */
        Usuario usuarioAutenticado = usuarioService.autenticar(dto.email(), dto.senha());

        /**
         * ✅ BOM: Separação clara entre autenticação (UsuarioService)
         * e geração de token (TokenService)
         */
        String token = tokenService.gerarToken(usuarioAutenticado);

        /**
         * ⚠️ SEGURANÇA: Retorna nome do usuário junto com token
         *
         * 💡 SUGESTÃO: Considerar o que expor na resposta
         * - Nome é informação não-sensível, ok retornar
         * - Nunca retornar senha ou dados sensíveis
         * - Considerar adicionar expiração do token na resposta
         *
         * RESPONSE MELHORADO:
         * record LoginResponseDTO(
         *     String nome,
         *     String token,
         *     Long expiresIn,  // Tempo em segundos até expirar
         *     String tokenType // "Bearer"
         * ) {}
         */
        return ResponseEntity.ok(new LoginResponseDTO(usuarioAutenticado.getNome(), token));
    }
}
package com.luizfernando.gestaousuarios.domain.service;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ⚠️ PROBLEMA ARQUITETURAL: Este service tem MÚLTIPLAS RESPONSABILIDADES
 *
 * 💡 SUGESTÃO: Aplicar Single Responsibility Principle (SRP)
 * Atualmente UsuarioService faz:
 * 1. Gerenciamento de usuários (CRUD)
 * 2. Autenticação (validar email/senha)
 * 3. Atualização de perfil
 *
 * REFATORAÇÃO SUGERIDA:
 * Criar AuthenticationService separado que encapsule APENAS lógica de autenticação.
 * Deixar UsuarioService apenas para operações CRUD.
 *
 * Benefício: Mais fácil testar, reutilizar e manter.
 */
@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ❌ ANTI-PATTERN: RuntimeException genérica para caso de negócio
     *
     * 💡 SUGESTÃO: Criar exceção customizada EmailJaCadastradoException
     * RuntimeException é usada para erros inesperados, não para validações de negócio.
     * Quando o email já existe, isso é uma validação de regra de negócio, não um erro.
     *
     * REFATORAÇÃO SUGERIDA:
     * 1. Criar exception:
     *    public class EmailJaCadastradoException extends RuntimeException {
     *        public EmailJaCadastradoException(String email) {
     *            super("Email " + email + " já está cadastrado no sistema.");
     *        }
     *    }
     *
     * 2. Usar no service:
     *    throw new EmailJaCadastradoException(usuario.getEmail());
     *
     * 3. Handler em GlobalExceptionHandler:
     *    @ExceptionHandler(EmailJaCadastradoException.class)
     *    public ResponseEntity<ErrorResponse> handle(EmailJaCadastradoException ex) {
     *        return ResponseEntity.status(HttpStatus.CONFLICT)
     *            .body(new ErrorResponse("EMAIL_JA_CADASTRADO", ex.getMessage()));
     *    }
     *
     * BENEFÍCIOS:
     * - HTTP status 409 Conflict (semanticamente correto)
     * - Frontend consegue tomar ações específicas
     * - Logging estruturado por tipo de erro
     */
    public Usuario salvarUsuario(Usuario usuario) {
        /**
         * ⚠️ PERFORMANCE: existsByEmail() seria mais eficiente que findByEmail().isPresent()
         *
         * 💡 SUGESTÃO: Usar método específico do repository
         * O repository já tem existsByEmail(String email) mas está usando findByEmail().
         *
         * PROBLEMA:
         * - findByEmail() executa SELECT * (carrega objeto inteiro)
         * - existsByEmail() executa SELECT COUNT(*) ou SELECT 1 (mais rápido)
         *
         * REFATORAÇÃO:
         * if (usuarioRepository.existsByEmail(usuario.getEmail())) {
         *     throw new EmailJaCadastradoException(usuario.getEmail());
         * }
         *
         * BENEFÍCIO:
         * - Query 10x mais rápido em bases grandes
         * - Menos tráfego na rede
         */
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            logger.warn("Tentativa de cadastro com email duplicado: {}", usuario.getEmail());
            throw new RuntimeException("Este e-mail já está cadastrado no sistema.");
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        // ✅ BOM: Logging de operação crítica para auditoria
        logger.info("Novo usuário cadastrado: {} (ID: {})", usuario.getEmail(), usuarioSalvo.getId());

        return usuarioSalvo;
    }

    /**
     * ❌ RESPONSABILIDADE INCORRETA: Lógica de autenticação em serviço de usuário
     *
     * 💡 SUGESTÃO: Mover para AuthenticationService separado
     * Autenticação é conceito diferente de gerenciamento de usuário.
     * Um usuário pode autenticar via email/senha, OAuth, LDAP, etc.
     * Se misturar com UsuarioService, fica impossível suportar múltiplas estratégias.
     *
     * REFATORAÇÃO SUGERIDA:
     * Criar @Service AuthenticationService que:
     * - Valida credenciais
     * - Loga tentativas (auditoria)
     * - Implementa rate limiting
     * - Suporta múltiplas estratégias de auth
     *
     * @Service
     * public class AuthenticationService {
     *     public Usuario autenticar(String email, String senha) {
     *         try {
     *             Usuario usuario = usuarioRepository.findByEmail(email)
     *                 .orElseThrow(() -> new EmailNaoEncontradoException(email));
     *
     *             if (!passwordEncoder.matches(senha, usuario.getSenha())) {
     *                 logger.warn("Falha de autenticação: email={}, motivo=senha_invalida", email);
     *                 throw new SenhaInvalidaException();
     *             }
     *
     *             logger.info("Usuário autenticado com sucesso: {}", email);
     *             return usuario;
     *         } catch (EmailNaoEncontradoException e) {
     *             logger.warn("Falha de autenticação: email={}, motivo=nao_encontrado", email);
     *             throw e;
     *         }
     *     }
     * }
     *
     * BENEFÍCIOS:
     * - Separação clara de responsabilidades
     * - Reutilizável em múltiplos contextos
     * - Fácil adicionar rate limiting
     * - Melhor auditoria
     */
    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            // ⚠️ AUDITORIA: Não está logando falha de autenticação
            // Adicionar: logger.warn("Falha de autenticação para email: {}", email);
            throw new RuntimeException("E-mail ou senha inválidos.");
        }

        return usuario;
    }

    /**
     * ⚠️ ESCALABILIDADE: Sem paginação, retorna todos os usuários
     *
     * 💡 SUGESTÃO: Implementar paginação
     * Se houver 100.000 usuários, essa query trava a aplicação.
     *
     * REFATORAÇÃO SUGERIDA:
     * public Page<Usuario> listarTodos(Pageable pageable) {
     *     return usuarioRepository.findAll(pageable);
     * }
     *
     * No controller:
     * @GetMapping
     * public ResponseEntity<Page<UsuarioDTO>> listar(Pageable pageable) {
     *     return ResponseEntity.ok(
     *         usuarioService.listarTodos(pageable)
     *             .map(UsuarioDTO::fromEntity)
     *     );
     * }
     *
     * CLIENTE CHAMA: GET /api/usuarios?page=0&size=10&sort=id,desc
     *
     * BENEFÍCIO:
     * - Performance em grandes volumes
     * - Padrão REST moderno
     */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * ❌ METHOD SMELL: Muitos parâmetros (4 parâmetros)
     *
     * 💡 SUGESTÃO: Usar DTO para encapsular dados
     * Quando um método tem muitos parâmetros, é sinal que precisa refatorar.
     * Dificulta manutenção: adicionar campo novo requer mudar assinatura.
     *
     * REFATORAÇÃO SUGERIDA:
     * // Receber o DTO inteiro
     * public Usuario atualizarPerfil(String emailLogado, AtualizarUsuarioDTO dto) {
     *     Usuario usuario = usuarioRepository.findByEmail(emailLogado)
     *         .orElseThrow(() -> new UsuarioNaoEncontradoException(emailLogado));
     *
     *     if (!usuario.getEmail().equals(dto.email()) &&
     *         usuarioRepository.existsByEmail(dto.email())) {
     *         throw new EmailJaCadastradoException(dto.email());
     *     }
     *
     *     usuario.setNome(dto.nome());
     *     usuario.setEmail(dto.email());
     *     if (dto.senha() != null && !dto.senha().trim().isEmpty()) {
     *         usuario.setSenha(passwordEncoder.encode(dto.senha()));
     *     }
     *
     *     return usuarioRepository.save(usuario);
     * }
     *
     * VANTAGENS:
     * - Assinatura mais limpa
     * - Validações centralizadas no DTO
     * - Fácil adicionar novos campos
     */
    public Usuario atualizarPerfil(String emailLogado, String novoNome, String novoEmail, String novaSenha) {
        // ⚠️ EXCEPTION GENÉRICA: RuntimeException ao invés de UsuarioNaoEncontradoException
        Usuario usuario = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        /**
         * ✅ BOM: Validação de duplicate email antes de atualizar
         * Evita violar constraint unique do banco de dados
         */
        if (!usuario.getEmail().equals(novoEmail) && usuarioRepository.findByEmail(novoEmail).isPresent()) {
            logger.warn("Tentativa de atualizar para email já em uso: {}", novoEmail);
            throw new RuntimeException("Este novo e-mail já está em uso por outro usuário.");
        }

        usuario.setNome(novoNome);
        usuario.setEmail(novoEmail);

        /**
         * ✅ BOM: Validação de senha vazia antes de criptografar
         * Permite que usuário não altere senha se deixar em branco
         */
        if (novaSenha != null && !novaSenha.trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }

        // ⚠️ AUDITORIA FALTANDO: Não há log de quem atualizou o quê
        // Adicionar: logger.info("Usuário atualizado: {} (email alterado: {})", emailLogado, !usuario.getEmail().equals(novoEmail));
        return usuarioRepository.save(usuario);
    }

    /**
     * ⚠️ AUTORIZAÇÃO FALTANDO: Qualquer um consegue deletar qualquer usuário
     *
     * 💡 SUGESTÃO: Adicionar verificação de autorização
     * Atualmente, se sabe o ID de outro usuário, consegue deletá-lo.
     * Deveria validar se o usuário logado é admin ou é o próprio usuário.
     *
     * REFATORAÇÃO SUGERIDA:
     * public void deletarUsuario(Long id, Long usuarioLogadoId) {
     *     Usuario usuarioADeletar = usuarioRepository.findById(id)
     *         .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
     *
     *     // Validar: apenas admin ou o próprio usuário conseguem deletar
     *     if (!usuarioLogadoId.equals(id) && !isAdmin(usuarioLogadoId)) {
     *         throw new AcessoNegadoException("Não tem permissão para deletar este usuário");
     *     }
     *
     *     usuarioRepository.deleteById(id);
     *     logger.warn("Usuário deletado: {} por solicitação de: {}", id, usuarioLogadoId);
     * }
     *
     * NO CONTROLLER:
     * @DeleteMapping("/{id}")
     * public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
     *     Long usuarioLogadoId = securityHelper.getUsuarioLogado().getId();
     *     usuarioService.deletarUsuario(id, usuarioLogadoId);
     *     return ResponseEntity.noContent().build();
     * }
     *
     * BENEFÍCIO:
     * - Segurança: previne deleção não autorizada
     * - Auditoria: sabe quem deletou
     */
    public void deletarUsuario(Long id) {
        // ⚠️ EXCEPTION GENÉRICA: RuntimeException ao invés de UsuarioNaoEncontradoException
        if (!usuarioRepository.existsById(id)) {
            logger.warn("Tentativa de deletar usuário inexistente: ID={}", id);
            throw new RuntimeException("Utilizador não encontrado.");
        }

        usuarioRepository.deleteById(id);
        logger.warn("Usuário deletado: ID={}", id);
    }
}
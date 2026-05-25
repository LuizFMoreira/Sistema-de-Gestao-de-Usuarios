package com.luizfernando.gestaousuarios.api.controller;

import com.luizfernando.gestaousuarios.api.dto.AtualizarUsuarioDTO;
import com.luizfernando.gestaousuarios.api.dto.UsuarioRequestDTO;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * ❌ CODE SMELL: Lógica de mapeamento DTO→Entity no controller (linha 28-35)
     *
     * 💡 SUGESTÃO: Mover a lógica de construção de Usuario para o service.
     * Isso viola o Single Responsibility Principle - o controller não deve fazer
     * mapeamento de objetos. O service é responsável por lógica de negócio.
     *
     * REFATORAÇÃO SUGERIDA:
     * usuarioService.criarNovoUsuario(dto)
     *
     * BENEFÍCIOS:
     * - Centraliza lógica de negócio no service
     * - Facilita testes unitários
     * - Reutilização de código em outros controllers
     */
    @PostMapping
    public ResponseEntity<Usuario> registrarUsuario(@RequestBody UsuarioRequestDTO dto) {
        // ⚠️ VALIDAÇÃO FALTANDO: @RequestBody sem @Valid não valida o DTO
        // Adicionar @Valid no parâmetro e validações no DTO com @NotBlank, @Email, etc
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(dto.senha());

        Usuario usuarioSalvo = usuarioService.salvarUsuario(novoUsuario);

        // ✅ BOM: Retorna HTTP 201 Created (correto para POST que cria recurso)
        // 💡 MELHORIA: Considerar retornar DTO ao invés de Entity (expõe detalhes internos)
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }

    /**
     * ⚠️ PROBLEMA DE ESCALABILIDADE: Retorna TODOS os usuários sem paginação (linha 40-42)
     *
     * 💡 SUGESTÃO: Implementar Page<UsuarioDTO> com Pageable
     * Se tiver 10.000 usuários no banco, isso trava a aplicação (Out of Memory).
     *
     * REFATORAÇÃO SUGERIDA:
     * @GetMapping
     * public ResponseEntity<Page<UsuarioDTO>> listarUsuarios(Pageable pageable) {
     *     return ResponseEntity.ok(usuarioService.listarComPaginacao(pageable)
     *         .map(UsuarioDTO::fromEntity));
     * }
     *
     * PARÂMETROS DE QUERY ESPERADOS:
     * GET /api/usuarios?page=0&size=10&sort=id,desc
     *
     * BENEFÍCIOS:
     * - Performance em grandes volumes
     * - Padrão REST moderno
     * - Economia de memória
     */
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /**
     * ❌ CODE SMELL: Acesso direto ao SecurityContextHolder no controller (linha 49)
     *
     * 💡 SUGESTÃO: Extrair para serviço helper desacoplado
     * Isso acumula lógica de segurança no controller. Fica difícil testar
     * pois precisa mockar SecurityContext. Melhor padrão é usar um helper.
     *
     * REFATORAÇÃO SUGERIDA:
     * @Autowired
     * private SecurityContextHelper securityHelper;
     *
     * No controller:
     * String email = securityHelper.getEmailUsuarioLogado();
     *
     * Criar classe helper:
     * @Component
     * public class SecurityContextHelper {
     *     public Usuario getUsuarioLogado() {
     *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *         if (auth != null && auth.getPrincipal() instanceof Usuario) {
     *             return (Usuario) auth.getPrincipal();
     *         }
     *         throw new SecurityException("Usuário não autenticado");
     *     }
     * }
     *
     * BENEFÍCIOS:
     * - Desacoplamento do Spring Security
     * - Código mais testável
     * - Reutilizável em outros controllers
     * - Type-safe (sem casting)
     */
    @PutMapping("/perfil")
    public ResponseEntity<Usuario> atualizarMeuPerfil(@RequestBody AtualizarUsuarioDTO dto) {
        // ⚠️ ACOPLAMENTO: Cast sem segurança de tipo (getPrincipal())
        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String emailLogado = usuarioLogado.getEmail();

        /**
         * ❌ METHOD SMELL: Muitos parâmetros (4 parâmetros)
         *
         * 💡 SUGESTÃO: Passar o DTO inteiro ao invés de destructurizá-lo
         * Quando um método tem muitos parâmetros, é sinal que precisa refatorar.
         *
         * REFATORAÇÃO:
         * usuarioService.atualizarPerfil(emailLogado, dto)
         *
         * No service:
         * public Usuario atualizarPerfil(String email, AtualizarUsuarioDTO dto) {
         *     // ...
         * }
         *
         * VANTAGENS:
         * - Assinatura mais limpa
         * - Fácil adicionar novos campos
         * - Validações centralizadas no DTO
         */
        Usuario usuarioAtualizado = usuarioService.atualizarPerfil(
                emailLogado,
                dto.nome(),
                dto.email(),
                dto.senha()
        );

        return ResponseEntity.ok(usuarioAtualizado);
    }

    /**
     * ⚠️ TRATAMENTO DE ERRO GENÉRICO: Não há tratamento específico para usuário não encontrado
     *
     * 💡 SUGESTÃO: Implementar exceções customizadas
     * Atualmente se usuário não existir, vem RuntimeException padrão (500).
     * Deveria ser 404 Not Found com mensagem clara.
     *
     * REFATORAÇÃO SUGERIDA:
     * 1. Criar UsuarioNaoEncontradoException extends RuntimeException
     * 2. Adicionar handler específico em GlobalExceptionHandler
     * 3. Retornar HTTP 404 com mensagem de erro estruturada
     *
     * NO HANDLER:
     * @ExceptionHandler(UsuarioNaoEncontradoException.class)
     * public ResponseEntity<ErrorResponse> handle(UsuarioNaoEncontradoException ex) {
     *     ErrorResponse error = new ErrorResponse("USUARIO_NAO_ENCONTRADO", ex.getMessage());
     *     return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
     * }
     *
     * BENEFÍCIOS:
     * - HTTP status codes semanticamente corretos
     * - Melhor experiência no frontend
     * - Tratamento consistente de erros
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        // ⚠️ AUDITORIA: Não há logging de quem deletou qual usuário
        // Adicionar logger.warn("Usuário deletado: ID={}", id) para auditoria
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
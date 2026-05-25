package com.luizfernando.gestaousuarios.api.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ❌ PROBLEMA ARQUITETURAL: Um único handler para TODAS as RuntimeException
 *
 * 💡 SUGESTÃO: Criar handlers específicos por tipo de erro
 * Atualmente tudo retorna HTTP 400 Bad Request.
 * Mas semanticamente:
 * - Email duplicado deveria ser 409 Conflict
 * - Usuário não encontrado deveria ser 404 Not Found
 * - Erro interno deveria ser 500 Internal Server Error
 *
 * REFATORAÇÃO SUGERIDA:
 * Criar exceções customizadas:
 * 1. EmailJaCadastradoException → 409 Conflict
 * 2. UsuarioNaoEncontradoException → 404 Not Found
 * 3. SenhaInvalidaException → 401 Unauthorized
 * 4. TokenExpiredException → 401 Unauthorized
 * 5. TokenInvalidoException → 401 Unauthorized
 *
 * EXEMPLO DE HANDLER ESPECÍFICO:
 * @ExceptionHandler(EmailJaCadastradoException.class)
 * public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(
 *     EmailJaCadastradoException ex, WebRequest request) {
 *     ErrorResponse error = new ErrorResponse(
 *         "EMAIL_JA_CADASTRADO",
 *         ex.getMessage(),
 *         LocalDateTime.now()
 *     );
 *     logger.warn("Email duplicado: {}", ex.getMessage());
 *     return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
 * }
 *
 * @ExceptionHandler(UsuarioNaoEncontradoException.class)
 * public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontrado(
 *     UsuarioNaoEncontradoException ex, WebRequest request) {
 *     ErrorResponse error = new ErrorResponse(
 *         "USUARIO_NAO_ENCONTRADO",
 *         ex.getMessage(),
 *         LocalDateTime.now()
 *     );
 *     logger.warn("Usuário não encontrado: {}", ex.getMessage());
 *     return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
 * }
 *
 * BENEFÍCIOS:
 * - HTTP status codes semanticamente corretos
 * - Frontend consegue tomar ações diferentes por erro
 * - Melhor logging estruturado
 * - Conformidade com REST standards
 * - Mais fácil debugar problemas
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * ⚠️ ANTI-PATTERN: Handler genérico demais
     *
     * 💡 SUGESTÃO: Remover este handler e criar handlers específicos
     * RuntimeException é muito genérica. Impossível diferenciar entre:
     * - Email já cadastrado
     * - Usuário não encontrado
     * - Erro interno real
     *
     * RESPOSTA MELHORADA:
     * Retornar um objeto estruturado com código de erro
     * {
     *     "codigo": "EMAIL_JA_CADASTRADO",
     *     "mensagem": "Email xxxxx@xxx.com já está cadastrado no sistema",
     *     "timestamp": "2024-05-25T14:30:00"
     * }
     *
     * Ao invés de:
     * {
     *     "erro": "Este e-mail já está cadastrado no sistema."
     * }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRegraDeNegocioException(
            RuntimeException ex, WebRequest request) {
        /**
         * ⚠️ AUDITORIA FALTANDO: Não há logging do erro
         *
         * 💡 SUGESTÃO: Adicionar logger para auditoria e debug
         * Sem logs é impossível investigar problemas em produção.
         *
         * REFATORAÇÃO:
         * if (ex.getMessage().contains("email já está cadastrado")) {
         *     logger.warn("Tentativa de cadastro com email duplicado");
         * } else if (ex.getMessage().contains("não encontrado")) {
         *     logger.warn("Recurso não encontrado: {}", ex.getMessage());
         * } else {
         *     logger.error("RuntimeException genérica não tratada", ex);
         * }
         */

        Map<String, String> resposta = new HashMap<>();
        resposta.put("erro", ex.getMessage());

        // ⚠️ PROBLEMA: HTTP 400 Bad Request para TODOS os erros
        // Email duplicado deveria ser 409 Conflict
        // Usuário não encontrado deveria ser 404 Not Found

        logger.error("RuntimeException não tratada: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }

    /**
     * ⚠️ FALTANDO: Handler para MethodArgumentNotValidException
     *
     * 💡 SUGESTÃO: Adicionar validação de entrada
     * Quando @Valid falha em DTOs, o Spring retorna resposta padrão complexa.
     *
     * REFATORAÇÃO SUGERIDA:
     * @ExceptionHandler(MethodArgumentNotValidException.class)
     * public ResponseEntity<ErrorResponse> handleValidationError(
     *     MethodArgumentNotValidException ex, WebRequest request) {
     *
     *     String messages = ex.getBindingResult().getFieldErrors()
     *         .stream()
     *         .map(error -> error.getField() + ": " + error.getDefaultMessage())
     *         .collect(Collectors.joining(", "));
     *
     *     ErrorResponse error = new ErrorResponse(
     *         "VALIDACAO_ERRO",
     *         messages,
     *         LocalDateTime.now()
     *     );
     *
     *     logger.warn("Validação de entrada falhou: {}", messages);
     *
     *     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
     * }
     *
     * BENEFÍCIO:
     * - Mensagens de erro claras (qual campo, qual validação falhou)
     * - Frontend consegue marcar campo específico como erro
     * - Auditoria de validações
     */

    /**
     * ⚠️ FALTANDO: Handler genérico para Exception
     *
     * 💡 SUGESTÃO: Adicionar fallback para erros não tratados
     * Se um erro inesperado ocorrer, deveria retornar mensagem genérica
     * (não expor detalhes internos) e logar o erro real.
     *
     * REFATORAÇÃO SUGERIDA:
     * @ExceptionHandler(Exception.class)
     * public ResponseEntity<ErrorResponse> handleGenericException(
     *     Exception ex, WebRequest request) {
     *
     *     ErrorResponse error = new ErrorResponse(
     *         "ERRO_INTERNO",
     *         "Ocorreu um erro interno no servidor",
     *         LocalDateTime.now()
     *     );
     *
     *     logger.error("Erro interno não tratado", ex);
     *
     *     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
     * }
     *
     * BENEFÍCIO:
     * - Não expõe detalhes internos para cliente
     * - Erro é loggado para investigação
     * - Mensagem padronizada para usuário
     */
}

/**
 * ✅ SUGESTÃO: Criar record para resposta de erro padronizada
 *
 * public record ErrorResponse(
 *     String codigo,      // Código de erro estruturado
 *     String mensagem,    // Mensagem legível
 *     LocalDateTime timestamp  // Quando ocorreu
 * ) {}
 *
 * VANTAGENS:
 * - Estrutura consistente em TODAS as respostas de erro
 * - Fácil parear com código de erro no frontend
 * - Timestamp para auditoria
 * - Serialização JSON automática
 */
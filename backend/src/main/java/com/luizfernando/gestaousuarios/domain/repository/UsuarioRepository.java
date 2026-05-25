package com.luizfernando.gestaousuarios.domain.repository;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ✅ BOM: Repository bem estruturado
 * Segue padrão Spring Data JPA
 * Encapsula lógica de acesso a dados
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * ✅ BOM: Query method bem nomeado
     * Spring gera a query automaticamente
     *
     * 💡 SUGESTÃO: Usar @Query customizada se precisar de lógica complexa
     * Para buscas simples por atributo, query methods são ideais.
     * Se precisar de JOINs ou lógica complexa, usar @Query explicit.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * ✅ BOM: Usa existsByEmail ao invés de findByEmail
     * Mais eficiente (SELECT COUNT(*) ao invés de SELECT *)
     *
     * 💡 SUGESTÃO: Usar este método em validações de duplicate
     * No UsuarioService, preferir existsByEmail() para checar duplicatas.
     *
     * EXEMPLO DE USO:
     * if (usuarioRepository.existsByEmail(email)) {
     *     throw new EmailJaCadastradoException(email);
     * }
     *
     * BENEFÍCIO:
     * - Query mais eficiente (não carrega objeto inteiro)
     * - Menos tráfego na rede
     * - Performance melhor em bases grandes
     */
    boolean existsByEmail(String email);

    /**
     * 💡 SUGESTÃO: Adicionar query customizadas se precisar
     *
     * EXEMPLOS DE POSSÍVEIS QUERIES:
     *
     * 1. Buscar por padrão de email (para admin listar):
     * @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
     * Page<Usuario> buscarPorEmailLike(@Param("email") String email, Pageable pageable);
     *
     * 2. Contar usuários (para estatísticas):
     * @Query("SELECT COUNT(u) FROM Usuario u")
     * long contarTotaisUsuarios();
     *
     * 3. Buscar usuários criados após data (auditoria):
     * @Query("SELECT u FROM Usuario u WHERE u.dataCriacao >= :data")
     * List<Usuario> buscarPorDataCriacao(@Param("data") LocalDateTime data);
     *
     * BENEFÍCIOS:
     * - Consultas específicas de negócio
     * - Performance otimizada
     * - Lógica de filtro centralizada
     */
}
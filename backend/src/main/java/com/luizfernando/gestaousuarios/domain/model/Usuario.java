package com.luizfernando.gestaousuarios.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ⚠️ PROBLEMA: Falta de validações na entidade
 *
 * 💡 SUGESTÃO: Adicionar Jakarta Validation annotations
 * Validações apenas no banco de dados (nullable=false) não são suficientes.
 * Deveria haver validações na camada de aplicação também.
 *
 * REFATORAÇÃO SUGERIDA:
 * - @NotBlank para campos obrigatórios
 * - @Email para validar formato de email
 * - @Size para limitar comprimento
 *
 * BENEFÍCIOS:
 * - Validações em múltiplas camadas (defense in depth)
 * - Fail-fast (detecta erros cedo)
 * - Mensagens de erro claras
 * - Fácil reutilizar em DTOs
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ⚠️ FALTANDO VALIDAÇÃO: Sem constraints de tamanho
     *
     * 💡 SUGESTÃO: Adicionar @Size e @NotBlank
     * Nome pode ser vazio ou excessivamente longo sem validação.
     */
    @NotBlank(message = "Nome não pode estar vazio")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false)
    private String nome;

    /**
     * ✅ BOM: unique = true garante no banco de dados
     *
     * ⚠️ MAS: Deve também ter validação de email
     *
     * 💡 SUGESTÃO: Adicionar @Email e @NotBlank
     * Email deve ser validado quanto ao formato, não apenas unicidade.
     */
    @NotBlank(message = "Email não pode estar vazio")
    @Email(message = "Email deve ser um endereço válido")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * ⚠️ FALTANDO VALIDAÇÃO: Sem validação de tamanho mínimo
     *
     * 💡 SUGESTÃO: Adicionar @Size
     * Senhas fracas (1 caractere) podem ser armazenadas sem validação.
     */
    @NotBlank(message = "Senha não pode estar vazia")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Column(nullable = false)
    private String senha;

    /**
     * ⚠️ FALTANDO: Campos de auditoria (criação/atualização)
     *
     * 💡 SUGESTÃO: Adicionar @CreationTimestamp e @UpdateTimestamp
     * Importante para auditoria: saber quando registro foi criado/modificado.
     *
     * REFATORAÇÃO:
     * @CreationTimestamp
     * @Column(nullable = false, updatable = false)
     * private LocalDateTime dataCriacao;
     *
     * @UpdateTimestamp
     * @Column(nullable = false)
     * private LocalDateTime dataAtualizacao;
     *
     * BENEFÍCIOS:
     * - Auditoria completa
     * - Histórico de alterações
     * - Conformidade regulatória (LGPD, GDPR)
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    // Construtor vazio para JPA
    public Usuario() {
    }

    // ✅ BOM: Getters e setters bem implementados

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    /**
     * ⚠️ PROBLEMA ARQUITETURAL: Não implementa UserDetails do Spring Security
     *
     * 💡 SUGESTÃO: Implementar org.springframework.security.core.userdetails.UserDetails
     * Atualmente a entidade é usada como principal mas não segue o contrato do Spring.
     *
     * REFATORAÇÃO SUGERIDA:
     * public class Usuario implements UserDetails {
     *     // ... campos
     *
     *     @ElementCollection(fetch = FetchType.EAGER)
     *     @Enumerated(EnumType.STRING)
     *     private Set<Role> roles = new HashSet<>();
     *
     *     @Override
     *     public Collection<? extends GrantedAuthority> getAuthorities() {
     *         return roles.stream()
     *             .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
     *             .collect(Collectors.toList());
     *     }
     *
     *     @Override
     *     public String getPassword() {
     *         return this.senha;
     *     }
     *
     *     @Override
     *     public String getUsername() {
     *         return this.email;
     *     }
     *
     *     @Override
     *     public boolean isAccountNonExpired() {
     *         return true;
     *     }
     *
     *     @Override
     *     public boolean isAccountNonLocked() {
     *         return true;
     *     }
     *
     *     @Override
     *     public boolean isCredentialsNonExpired() {
     *         return true;
     *     }
     *
     *     @Override
     *     public boolean isEnabled() {
     *         return true;
     *     }
     * }
     *
     * ENUM Role:
     * public enum Role {
     *     ADMIN,
     *     USER
     * }
     *
     * BENEFÍCIOS:
     * - Suporte para autorização (roles/permissions)
     * - Integração mais clara com Spring Security
     * - Prepara para controle de acesso futuro
     * - Segue padrão Spring esperado
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
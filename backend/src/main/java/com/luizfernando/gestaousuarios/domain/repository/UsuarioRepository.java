package com.luizfernando.gestaousuarios.domain.repository;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Função vital para validação: Busca no banco se o e-mail já existe
    Optional<Usuario> findByEmail(String email);
    
    // Função para verificar existência rápida na Tela de Cadastro
    boolean existsByEmail(String email);
}
package com.luizfernando.gestaousuarios.api.dto;

// pode ser utilizado para na atualização da senha 
public record AtualizarUsuarioDTO(
        String nome,
        String email,
        String senha
) {
}
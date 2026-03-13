package com.luizfernando.gestaousuarios.api.dto;

//trnsferencia de dados
public record UsuarioRequestDTO(
        String nome,
        String email,
        String senha
) {
}
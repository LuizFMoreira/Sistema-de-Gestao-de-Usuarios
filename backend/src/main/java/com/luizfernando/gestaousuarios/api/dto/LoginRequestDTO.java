package com.luizfernando.gestaousuarios.api.dto;

//transferencias de dados para o login
public record LoginRequestDTO(
        String email,
        String senha
) {
}
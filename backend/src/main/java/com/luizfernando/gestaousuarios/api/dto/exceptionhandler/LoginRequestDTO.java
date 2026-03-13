package com.luizfernando.gestaousuarios.api.dto;

public record LoginRequestDTO(
        String email,
        String senha
) {
}
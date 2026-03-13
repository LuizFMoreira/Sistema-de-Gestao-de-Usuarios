package com.luizfernando.gestaousuarios.api.dto;

// O Record do Java gera automaticamente construtores, getters e métodos equals/hashCode.
// É a estrutura de dados mais limpa e eficiente para transferência.
public record UsuarioRequestDTO(
        String nome,
        String email,
        String senha
) {
}
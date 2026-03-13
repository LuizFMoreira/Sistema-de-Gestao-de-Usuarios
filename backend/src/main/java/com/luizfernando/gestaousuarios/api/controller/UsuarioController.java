package com.luizfernando.gestaousuarios.api.controller;

import com.luizfernando.gestaousuarios.api.dto.UsuarioRequestDTO;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Injeção de dependência pelo construtor (Melhor prática de Engenharia)
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Usuario> registrarUsuario(@RequestBody UsuarioRequestDTO dto) {
        // 1. Mapeamento: Converte o Vetor de Transferência (DTO) para a Entidade real
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(dto.senha());

        // 2. Processamento: Envia para o serviço aplicar o Hash e salvar no banco
        Usuario usuarioSalvo = usuarioService.salvarUsuario(novoUsuario);

        // 3. Retorno: Devolve o Status 201 (Created) e o objeto salvo (sem exibir a senha em texto puro)
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }
}
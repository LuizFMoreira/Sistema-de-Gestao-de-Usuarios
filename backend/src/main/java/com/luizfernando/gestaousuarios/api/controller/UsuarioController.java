package com.luizfernando.gestaousuarios.api.controller;

import com.luizfernando.gestaousuarios.api.dto.AtualizarUsuarioDTO; 
import com.luizfernando.gestaousuarios.api.dto.UsuarioRequestDTO;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Usuario> registrarUsuario(@RequestBody UsuarioRequestDTO dto) {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(dto.senha());

        Usuario usuarioSalvo = usuarioService.salvarUsuario(novoUsuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // Rota pra pessoa atualizar os PRÓPRIOS dados. (mapeamento)
    @PutMapping("/perfil")
    public ResponseEntity<Usuario> atualizarMeuPerfil(@RequestBody AtualizarUsuarioDTO dto) {
        
        // puxando os dados do usuário que o meu SecurityFilter validou no token JWT
        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String emailLogado = usuarioLogado.getEmail();

        // enviado para o service
        Usuario usuarioAtualizado = usuarioService.atualizarPerfil(
                emailLogado, 
                dto.nome(), 
                dto.email(), 
                dto.senha()
        );

        return ResponseEntity.ok(usuarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
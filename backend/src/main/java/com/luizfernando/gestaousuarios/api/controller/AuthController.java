package com.luizfernando.gestaousuarios.api.controller;

import com.luizfernando.gestaousuarios.api.dto.LoginRequestDTO;
import com.luizfernando.gestaousuarios.api.dto.LoginResponseDTO;
import com.luizfernando.gestaousuarios.core.security.TokenService;
import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    // Injeção de dependências via construtor
    public AuthController(UsuarioService usuarioService, TokenService tokenService) {
        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        // Valida e-mail e senha
        Usuario usuarioAutenticado = usuarioService.autenticar(dto.email(), dto.senha());
        
        // Gera o Token JWT
        String token = tokenService.gerarToken(usuarioAutenticado);
        
        // Devolve para o React
        return ResponseEntity.ok(new LoginResponseDTO(usuarioAutenticado.getNome(), token));
    }
}
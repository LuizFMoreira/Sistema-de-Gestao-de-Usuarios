package com.luizfernando.gestaousuarios.domain.service;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    // Injeção de dependências: Trazendo os operadores necessários para esta classe
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Função para salvar um novo usuário aplicando o Hash na senha
    public Usuario salvarUsuario(Usuario usuario) {
        // Validação de negócio: Não permitir e-mails duplicados
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Este e-mail já está cadastrado no sistema.");
        }

        // Aplica o Hash criptográfico na senha recebida em texto puro
        String senhaCriptografada = passwordEncoder.encode(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);

        // Salva a entidade no banco de dados e retorna o objeto persistido
        return usuarioRepository.save(usuario);
    }

    // Função auxiliar para buscar usuário pelo e-mail (será usada no Login)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}
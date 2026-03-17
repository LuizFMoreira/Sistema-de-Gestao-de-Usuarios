package com.luizfernando.gestaousuarios.domain.service;

import com.luizfernando.gestaousuarios.domain.model.Usuario;
import com.luizfernando.gestaousuarios.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario salvarUsuario(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("Este e-mail já está cadastrado no sistema.");
        }
        
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new RuntimeException("E-mail ou senha inválidos.");
        }

        return usuario;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // método para o usuário atualizar o próprio perfil (OBRIGATORIO)
    public Usuario atualizarPerfil(String emailLogado, String novoNome, String novoEmail, String novaSenha) {
        
        // busca que m é o dono do token no banco de dados (bd)
        Usuario usuario = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // trava: se ele quer mudar o e-mail, verifico se outra pessoa já não está usando o e-mail novo
        if (!usuario.getEmail().equals(novoEmail) && usuarioRepository.findByEmail(novoEmail).isPresent()) {
            throw new RuntimeException("Este novo e-mail já está em uso por outro usuário.");
        }

        
        usuario.setNome(novoNome);
        usuario.setEmail(novoEmail);

        // se ele digitou alguma coisa na senha nova, eu criptografo e slv. Caso o contrario, vai ficar a senha antiga.
        if (novaSenha != null && !novaSenha.trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }

        //Salva no bd e retorna o usuário atualizado
        return usuarioRepository.save(usuario);
    }
}
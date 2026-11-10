package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    @Transactional
    public Usuario salvarUsuario(Usuario usuario) {
        validar(usuario);
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }
        usuario.setId(null); // impede sobrescrever um usuário existente via corpo da requisição
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        Usuario salvo = usuarioRepository.save(usuario);
        auditoria.registrar("CRIACAO_USUARIO", "Usuario", salvo.getId(), Map.of(), campos(salvo), null);
        return salvo;
    }

    // Deleta um usuário pelo ID
    @Transactional
    public void deletarUsuario(String id) {
        usuarioRepository.findById(id).ifPresent(existente -> {
            if (existente.getUsername().equals(AuditoriaService.usuarioAtual())) {
                throw new IllegalStateException("Você não pode excluir o próprio usuário.");
            }
            if (ehUltimoAdmin(existente)) {
                throw new IllegalStateException("Não é possível excluir o último administrador do sistema.");
            }
            usuarioRepository.deleteById(id);
            auditoria.registrar("EXCLUSAO_USUARIO", "Usuario", id, campos(existente), Map.of(), null);
        });
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    @Transactional
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        validar(usuarioAtualizado);

        boolean proprioUsuario = existente.getUsername().equals(AuditoriaService.usuarioAtual());
        if (proprioUsuario && !usuarioAtualizado.getRole().equals(existente.getRole())) {
            throw new IllegalStateException("Você não pode alterar o próprio perfil.");
        }
        if (!Perfil.ADMIN.name().equals(usuarioAtualizado.getRole()) && ehUltimoAdmin(existente)) {
            throw new IllegalStateException("Não é possível rebaixar o último administrador do sistema.");
        }
        if (!existente.getUsername().equals(usuarioAtualizado.getUsername())
                && usuarioRepository.findByUsername(usuarioAtualizado.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }

        Map<String, Object> antes = campos(existente);
        existente.setUsername(usuarioAtualizado.getUsername());
        existente.setRole(usuarioAtualizado.getRole());

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        Usuario salvo = usuarioRepository.save(existente);
        auditoria.registrar("ALTERACAO_USUARIO", "Usuario", id, antes, campos(salvo), null);
        return salvo;
    }

    private void validar(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("O username é obrigatório.");
        }
        if (Arrays.stream(Perfil.values()).noneMatch(p -> p.name().equals(usuario.getRole()))) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
    }

    private boolean ehUltimoAdmin(Usuario usuario) {
        return Perfil.ADMIN.name().equals(usuario.getRole())
                && usuarioRepository.countByRole(Perfil.ADMIN.name()) <= 1;
    }

    // Nunca inclui a senha (nem o hash) na auditoria
    private Map<String, Object> campos(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u.getUsername());
        m.put("perfil", u.getRole());
        return m;
    }
}

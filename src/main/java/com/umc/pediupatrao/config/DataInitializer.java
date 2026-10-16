package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            // Usuários de demonstração, um por perfil (trocar as senhas em produção)
            criarSeAusente(repo, encoder, "admin", "teste123", Perfil.ADMIN);
            criarSeAusente(repo, encoder, "gerente", "gerente123", Perfil.GERENTE);
            criarSeAusente(repo, encoder, "atendente", "atendente123", Perfil.ATENDENTE);

            // O admin criado antes dos perfis existirem não tem role gravada
            repo.findByUsername("admin").filter(u -> u.getRole() == null).ifPresent(u -> {
                u.setRole(Perfil.ADMIN.name());
                repo.save(u);
            });
        };
    }

    private void criarSeAusente(UsuarioRepository repo, BCryptPasswordEncoder encoder,
            String username, String senha, Perfil perfil) {
        if (repo.findByUsername(username).isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setPassword(encoder.encode(senha));
            user.setRole(perfil.name());
            repo.save(user);
            System.out.println("Usuário " + username + " criado!");
        }
    }
}

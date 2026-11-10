package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditoriaService auditoria;
    @InjectMocks UsuarioService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.findByUsername(any())).thenReturn(Optional.empty());
    }

    private Usuario usuario(String id, String username, String role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private Usuario dados(String username, String role) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    @Test
    void naoExcluiUltimoAdmin() {
        usuario("u2", "admin2", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.deletarUsuario("u2"));
        verify(usuarioRepository, never()).deleteById("u2");
    }

    @Test
    void excluiAdminQuandoHaOutro() {
        usuario("u2", "admin2", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(2L);

        service.deletarUsuario("u2");
        verify(usuarioRepository).deleteById("u2");
    }

    @Test
    void naoRebaixaUltimoAdmin() {
        usuario("u2", "admin2", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.atualizarUsuario("u2", dados("admin2", "ATENDENTE")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rebaixaAdminQuandoHaOutro() {
        usuario("u2", "admin2", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(2L);

        Usuario salvo = service.atualizarUsuario("u2", dados("admin2", "GERENTE"));
        assertEquals("GERENTE", salvo.getRole());
    }

    @Test
    void naoExcluiOProprioUsuario() {
        usuario("u1", "admin", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> service.deletarUsuario("u1"));
    }

    @Test
    void naoAlteraOProprioPerfil() {
        usuario("u1", "admin", "ADMIN");
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(2L);

        assertThrows(IllegalStateException.class,
                () -> service.atualizarUsuario("u1", dados("admin", "GERENTE")));
    }
}

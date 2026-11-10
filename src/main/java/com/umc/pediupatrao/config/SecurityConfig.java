package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF ativo: formulários Thymeleaf recebem o token sozinhos; o layout injeta o
                // cabeçalho nas chamadas fetch() (ver layout.html)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        // Administração de usuários e perfis: somente ADMIN
                        .requestMatchers("/usuarios/**", "/api/usuarios/**", "/admin/**").hasRole("ADMIN")
                        // Auditoria: somente leitura, ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/auditoria", "/api/auditoria").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/auditoria/**", "/api/auditoria/**").denyAll()
                        // Dashboard: somente consulta, ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/dashboard/**").denyAll()
                        // Pedidos
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("ATENDENTE", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/api/pedidos").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        // Clientes: ATENDENTE cadastra/atualiza, GERENTE consulta (e exclui)
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes").hasAnyRole("ATENDENTE", "GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/*").hasRole("GERENTE")
                        .requestMatchers("/clientes/**", "/api/clientes/**").hasRole("ATENDENTE")
                        // Produtos: leitura para todos os perfis, escrita para ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/produtos/**", "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                        // Qualquer outra rota exige um dos três perfis (perfil desconhecido não acessa nada)
                        .anyRequest().hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, e) -> {
                    log.warn("Acesso negado: usuario={} metodo={} uri={}",
                            AuditoriaService.usuarioAtual(), request.getMethod(), request.getRequestURI());
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"erro\":\"Acesso negado: seu perfil não tem permissão para esta operação.\"}");
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN); // página error.html
                    }
                }))
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}

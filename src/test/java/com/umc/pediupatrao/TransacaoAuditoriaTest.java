package com.umc.pediupatrao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Simula falha ao gravar a auditoria e confere que a operação foi desfeita no banco.
 * Só roda com um MongoDB em replica set em 127.0.0.1:27017 (ver README).
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://127.0.0.1:27017/pizzaria_teste",
        "spring.data.mongodb.database=pizzaria_teste"})
@EnabledIf("replicaSetLocal")
class TransacaoAuditoriaTest {

    @MockitoBean AuditoriaRepository auditoriaRepository;

    @Autowired ClienteService clienteService;
    @Autowired PedidoService pedidoService;
    @Autowired ClienteRepository clienteRepository;
    @Autowired PedidoRepository pedidoRepository;

    static boolean replicaSetLocal() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:27017"))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build();
        try (MongoClient client = MongoClients.create(settings)) {
            Document hello = client.getDatabase("admin").runCommand(new Document("hello", 1));
            return hello.containsKey("setName");
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
        pedidoRepository.deleteAll();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("gerente", "x", "ROLE_GERENTE"));
        when(auditoriaRepository.save(any(AuditoriaEvento.class)))
                .thenThrow(new IllegalStateException("falha simulada na auditoria"));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cadastroDeClienteEDesfeito() {
        Cliente novo = new Cliente();
        novo.setNome("Ana Lima");
        novo.setTelefone("(11) 90000-0000");

        assertThrows(IllegalStateException.class, () -> clienteService.salvar(novo));
        assertEquals(0, clienteRepository.count());
    }

    @Test
    void alteracaoDeClienteEDesfeita() {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana Lima");
        cliente.setTelefone("(11) 90000-0000");
        cliente = clienteRepository.save(cliente);

        cliente.setTelefone("(11) 91111-1111");
        Cliente alterado = cliente;
        assertThrows(IllegalStateException.class, () -> clienteService.salvar(alterado));

        assertEquals("(11) 90000-0000", clienteRepository.findById(cliente.getId()).orElseThrow().getTelefone());
    }

    @Test
    void mudancaDeStatusDoPedidoEDesfeita() {
        Pedido pedido = new Pedido();
        pedido.setStatus("RECEBIDO");
        pedido = pedidoRepository.save(pedido);
        String id = pedido.getId();

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus(id, "EM_PREPARACAO"));

        assertEquals("RECEBIDO", pedidoRepository.findById(id).orElseThrow().getStatus());
    }
}

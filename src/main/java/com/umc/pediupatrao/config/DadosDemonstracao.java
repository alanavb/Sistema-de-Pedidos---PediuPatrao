package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Carga de dados para demonstrar o dashboard. Só roda com o perfil "demo" e só quando
 * não existe nenhum pedido, para nunca misturar dados fictícios com dados reais.
 * Grava direto nos repositórios: não é uma operação de usuário, então não gera auditoria.
 */
@Component
@Profile("demo")
public class DadosDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);
    private static final long SEMENTE = 20261110L;

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final int dias;

    public DadosDemonstracao(ProdutoRepository produtoRepository, ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository, @Value("${demo.dias:120}") int dias) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.dias = dias;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.count() > 0) {
            log.info("Perfil demo: já existem pedidos, a carga de demonstração não foi executada.");
            return;
        }
        List<Produto> produtos = produtosDisponiveis();
        List<Cliente> clientes = clienteRepository.count() > 0 ? clienteRepository.findAll() : criarClientes();

        List<Pedido> pedidos = new GeradorPedidosDemonstracao(SEMENTE, produtos, clientes)
                .gerar(LocalDateTime.now(), dias);
        pedidoRepository.saveAll(pedidos);
        log.info("Perfil demo: {} produtos, {} clientes e {} pedidos dos últimos {} dias.",
                produtos.size(), clientes.size(), pedidos.size(), dias);
    }

    private List<Produto> produtosDisponiveis() {
        List<Produto> ativos = produtoRepository.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getAtivo()) && p.getPreco() != null)
                .toList();
        if (!ativos.isEmpty()) {
            return ativos;
        }
        return produtoRepository.saveAll(List.of(
                produto("Pizza Calabresa", "Pizza", 52.90),
                produto("Pizza Margherita", "Pizza", 49.90),
                produto("Pizza Portuguesa", "Pizza", 56.90),
                produto("Pizza Quatro Queijos", "Pizza", 58.90),
                produto("Pizza Frango com Catupiry", "Pizza", 55.90),
                produto("Pizza de Chocolate", "Pizza", 46.90),
                produto("Esfiha de Carne", "Esfiha", 6.50),
                produto("Esfiha de Queijo", "Esfiha", 6.00),
                produto("Esfiha de Calabresa", "Esfiha", 6.50),
                produto("X-Burguer", "Lanche", 24.90),
                produto("X-Salada", "Lanche", 26.90),
                produto("Coca-Cola 2L", "Bebida", 14.00),
                produto("Guaraná 2L", "Bebida", 12.00),
                produto("Suco de Laranja 500ml", "Bebida", 9.00)));
    }

    private static Produto produto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    private List<Cliente> criarClientes() {
        String[][] dados = {
            {"Ana Paula Ribeiro", "(11) 98765-1001", "Rua das Flores", "120", "Centro"},
            {"Bruno Carvalho", "(11) 98765-1002", "Av. Brasil", "455", "Jardim Santista"},
            {"Carla Mendes", "(11) 98765-1003", "Rua Ipiranga", "78", "Vila Oliveira"},
            {"Diego Fernandes", "(11) 98765-1004", "Rua Navarro de Andrade", "310", "Centro"},
            {"Elaine Costa", "(11) 98765-1005", "Av. Voluntário Fernando Pinheiro Franco", "1500", "Centro"},
            {"Felipe Souza", "(11) 98765-1006", "Rua Dr. Deodato Wertheimer", "890", "Vila Industrial"},
            {"Gabriela Lima", "(11) 98765-1007", "Rua Cabo Diogo Oliver", "42", "Mogilar"},
            {"Henrique Alves", "(11) 98765-1008", "Av. Japão", "2100", "Vila Mogilar"},
            {"Isabela Rocha", "(11) 98765-1009", "Rua Francisco Martins", "15", "Vila Rubens"},
            {"João Pedro Nunes", "(11) 98765-1010", "Rua Professor Álvaro Pavan", "233", "Alto Ipiranga"},
            {"Larissa Martins", "(11) 98765-1011", "Rua José Urbano", "67", "Brás Cubas"},
            {"Marcos Vinícius", "(11) 98765-1012", "Av. Cívica", "540", "Centro Cívico"},
        };
        List<Cliente> clientes = new ArrayList<>();
        for (String[] d : dados) {
            Cliente c = new Cliente();
            c.setNome(d[0]);
            c.setTelefone(d[1]);
            c.setLogradouro(d[2]);
            c.setNumero(d[3]);
            c.setBairro(d[4]);
            c.setCidade("Mogi das Cruzes");
            c.setEstado("SP");
            c.setCep("08780-000");
            clientes.add(c);
        }
        return clienteRepository.saveAll(clientes);
    }
}

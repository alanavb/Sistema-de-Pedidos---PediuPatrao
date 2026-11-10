package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Service
public class ClienteService {

    // Logradouro, bairro, cidade e estado ficam abertos: sem número e complemento não identificam o cliente
    static final Map<String, UnaryOperator<String>> MASCARAS = Map.of(
            "nome", MascaraDados::nome,
            "telefone", MascaraDados::telefone,
            "cep", MascaraDados::cep,
            "numero", MascaraDados::oculto,
            "complemento", MascaraDados::oculto,
            "endereco", MascaraDados::digitos);

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoria;

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.findById(id).ifPresent(existente -> {
            clienteRepository.deleteById(id);
            auditoria.registrar("EXCLUSAO_CLIENTE", "Cliente", id, campos(existente), Map.of(), null, MASCARAS);
        });
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);
            auditoria.registrar("CRIACAO_CLIENTE", "Cliente", salvo.getId(), Map.of(), campos(salvo), null, MASCARAS);
            return salvo;
        }
        // Se já tem ID (cliente existente), atualiza
        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));
        Map<String, Object> antes = campos(anterior);
        Cliente salvo = clienteRepository.save(cliente);
        auditoria.registrar("ALTERACAO_CLIENTE", "Cliente", salvo.getId(), antes, campos(salvo), null, MASCARAS);
        return salvo;
    }

    private Map<String, Object> campos(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", c.getNome());
        m.put("telefone", c.getTelefone());
        m.put("cep", c.getCep());
        m.put("logradouro", c.getLogradouro());
        m.put("numero", c.getNumero());
        m.put("complemento", c.getComplemento());
        m.put("bairro", c.getBairro());
        m.put("cidade", c.getCidade());
        m.put("estado", c.getEstado());
        m.put("endereco", c.getEndereco());
        return m;
    }
}

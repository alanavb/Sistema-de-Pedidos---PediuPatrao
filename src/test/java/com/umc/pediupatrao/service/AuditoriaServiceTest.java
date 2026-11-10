package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock AuditoriaRepository auditoriaRepository;
    @InjectMocks AuditoriaService service;

    private Map<String, Object> cliente(String nome, String telefone, String numero, String logradouro) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", nome);
        m.put("telefone", telefone);
        m.put("numero", numero);
        m.put("logradouro", logradouro);
        return m;
    }

    private List<Alteracao> registrado() {
        ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(auditoriaRepository).save(captor.capture());
        return captor.getValue().alteracoes();
    }

    @Test
    void gravaDadosPessoaisMascarados() {
        service.registrar("ALTERACAO_CLIENTE", "Cliente", "c1",
                cliente("Maria Souza", "(11) 98765-4321", "100", "Rua das Flores"),
                cliente("Maria Aparecida Souza", "(11) 91234-5678", "200", "Av. Brasil"),
                null, ClienteService.MASCARAS);

        List<Alteracao> alteracoes = registrado();
        assertEquals(new Alteracao("nome", "Maria S.", "Maria A. S."), alteracoes.get(0));
        assertEquals(new Alteracao("telefone", "(**) *****-4321", "(**) *****-5678"), alteracoes.get(1));
        assertEquals(new Alteracao("numero", "***", "***"), alteracoes.get(2));
        assertEquals(new Alteracao("logradouro", "Rua das Flores", "Av. Brasil"), alteracoes.get(3));
    }

    @Test
    void trocaQueFicaIgualMascaradaAindaERegistrada() {
        service.registrar("ALTERACAO_CLIENTE", "Cliente", "c1",
                cliente("Ana", "(11) 99999-1234", "1", "Rua A"),
                cliente("Ana", "(11) 98888-1234", "1", "Rua A"),
                null, ClienteService.MASCARAS);

        List<Alteracao> alteracoes = registrado();
        assertEquals(1, alteracoes.size());
        assertEquals("telefone", alteracoes.get(0).campo());
    }

    @Test
    void semMudancaNaoGravaNada() {
        Map<String, Object> dados = cliente("Ana", "(11) 99999-1234", "1", "Rua A");
        service.registrar("ALTERACAO_CLIENTE", "Cliente", "c1", dados, dados, null, ClienteService.MASCARAS);

        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void mascaras() {
        assertEquals("08780-***", MascaraDados.cep("08780-120"));
        assertEquals("Rua X, *** - Centro", MascaraDados.digitos("Rua X, 123 - Centro"));
        assertEquals("Ana", MascaraDados.nome("Ana"));
        assertEquals("", MascaraDados.oculto(""));
    }
}

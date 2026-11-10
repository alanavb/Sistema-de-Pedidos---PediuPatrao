package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public static String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "sistema" : auth.getName();
    }

    public static String perfilAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "-";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("-");
    }

    /** Campos cujo valor mudou entre os dois mapas (chave ausente conta como nulo). */
    public static List<Alteracao> diff(Map<String, ?> antes, Map<String, ?> depois) {
        Set<String> campos = new LinkedHashSet<>(antes.keySet());
        campos.addAll(depois.keySet());
        List<Alteracao> alteracoes = new ArrayList<>();
        for (String campo : campos) {
            Object anterior = antes.get(campo);
            Object novo = depois.get(campo);
            if (!Objects.equals(anterior, novo)) {
                alteracoes.add(new Alteracao(campo,
                        anterior == null ? null : anterior.toString(),
                        novo == null ? null : novo.toString()));
            }
        }
        return alteracoes;
    }

    /** Persiste o evento; não grava nada se nenhum campo mudou. Nunca passe senhas em antes/depois. */
    public void registrar(String operacao, String entidade, String entidadeId,
            Map<String, ?> antes, Map<String, ?> depois, String justificativa) {
        registrar(operacao, entidade, entidadeId, antes, depois, justificativa, Map.of());
    }

    // Compara os valores reais e grava só a versão mascarada
    // (trocar 9999-1234 por 8888-1234 continua gerando registro)
    public void registrar(String operacao, String entidade, String entidadeId,
            Map<String, ?> antes, Map<String, ?> depois, String justificativa,
            Map<String, UnaryOperator<String>> mascaras) {
        List<Alteracao> alteracoes = diff(antes, depois).stream()
                .map(a -> mascarar(a, mascaras.get(a.campo())))
                .toList();
        if (alteracoes.isEmpty()) {
            return;
        }
        auditoriaRepository.save(new AuditoriaEvento(null, usuarioAtual(), perfilAtual(),
                LocalDateTime.now(), operacao, entidade, entidadeId, alteracoes, justificativa));
    }

    private static Alteracao mascarar(Alteracao a, UnaryOperator<String> mascara) {
        if (mascara == null) {
            return a;
        }
        return new Alteracao(a.campo(),
                a.valorAnterior() == null ? null : mascara.apply(a.valorAnterior()),
                a.valorNovo() == null ? null : mascara.apply(a.valorNovo()));
    }

    public List<AuditoriaEvento> listar() {
        return auditoriaRepository.findTop500ByOrderByDataHoraDesc();
    }
}

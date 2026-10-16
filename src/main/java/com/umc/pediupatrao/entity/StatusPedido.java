package com.umc.pediupatrao.entity;

import java.util.EnumSet;
import java.util.Set;

public enum StatusPedido {
    RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO;

    /** Próximos estados permitidos no fluxo normal (o cancelamento é tratado à parte). */
    public Set<StatusPedido> proximos() {
        return switch (this) {
            case RECEBIDO -> EnumSet.of(EM_PREPARACAO);
            case EM_PREPARACAO -> EnumSet.of(PRONTO);
            case PRONTO -> EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> EnumSet.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> EnumSet.noneOf(StatusPedido.class);
        };
    }

    /** Pedido que ainda não saiu nem foi cancelado: aceita desconto e cancelamento. */
    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    public boolean saida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }
}

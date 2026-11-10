package com.umc.pediupatrao.dto.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FiltroDashboardTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Test
    void semDatasUsaOsUltimos30Dias() {
        FiltroDashboard f = FiltroDashboard.criar(null, null, null, null, HOJE);
        assertEquals(LocalDate.of(2026, 10, 12), f.inicio());
        assertEquals(HOJE, f.fim());
        assertEquals(30, f.dias());
        assertTrue(f.status().isEmpty());
        assertNull(f.tipoProduto());
    }

    @Test
    void soDataInicialVaiAteHoje() {
        FiltroDashboard f = FiltroDashboard.criar(LocalDate.of(2026, 11, 1), null, null, null, HOJE);
        assertEquals(HOJE, f.fim());
    }

    @Test
    void dataInicialDepoisDaFinalEInvalida() {
        assertThrows(IllegalArgumentException.class, () -> FiltroDashboard.criar(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null, null, HOJE));
    }

    @Test
    void periodoMaximoDe366Dias() {
        LocalDate inicio = LocalDate.of(2025, 11, 10);
        assertEquals(366, FiltroDashboard.criar(inicio, inicio.plusDays(365), null, null, HOJE).dias());
        assertThrows(IllegalArgumentException.class,
                () -> FiltroDashboard.criar(inicio, inicio.plusDays(366), null, null, HOJE));
    }

    @Test
    void periodoAnteriorTemOMesmoTamanho() {
        FiltroDashboard f = FiltroDashboard.criar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7),
                List.of(StatusPedido.FINALIZADO), "Pizza", HOJE);
        FiltroDashboard anterior = f.periodoAnterior();
        assertEquals(LocalDate.of(2026, 10, 25), anterior.inicio());
        assertEquals(LocalDate.of(2026, 10, 31), anterior.fim());
        assertEquals(Set.of(StatusPedido.FINALIZADO), anterior.status());
        assertEquals("Pizza", anterior.tipoProduto());
    }

    @Test
    void intervaloIncluiODiaFinalInteiro() {
        FiltroDashboard f = FiltroDashboard.criar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7),
                null, "  ", HOJE);
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), f.desde());
        assertEquals(LocalDateTime.of(2026, 11, 8, 0, 0), f.ate());
        assertNull(f.tipoProduto());
    }
}

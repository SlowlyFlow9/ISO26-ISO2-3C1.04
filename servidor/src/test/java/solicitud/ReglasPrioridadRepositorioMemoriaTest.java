package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReglasPrioridadRepositorioMemoriaTest {

    private ReglaPrioridad regla(long id, String nombre, int orden) {
        return new ReglaPrioridad(
                id, nombre, "", orden, true, "admin",
                LocalDateTime.now());
    }

    @Test
    void asignaIdentificadoresYRespetaIdsExplicitos() {
        ReglasPrioridadRepositorioMemoria repositorio =
                new ReglasPrioridadRepositorioMemoria();

        assertEquals(1, repositorio.guardar(regla(0, "Urgente", 1))
                .getId());
        assertEquals(7, repositorio.guardar(regla(7, "Preferente", 2))
                .getId());
        assertEquals(8, repositorio.guardar(regla(0, "Ordinaria", 3))
                .getId());
    }

    @Test
    void consultaPorIdNombreYOrdenSinDistinguirMayusculas() {
        ReglasPrioridadRepositorioMemoria repositorio =
                new ReglasPrioridadRepositorioMemoria();
        ReglaPrioridad guardada =
                repositorio.guardar(regla(0, "Urgente", 1));

        assertTrue(repositorio.buscarPorId(guardada.getId()).isPresent());
        assertEquals(guardada,
                repositorio.buscarPorNombre(" urgente ").orElseThrow());
        assertEquals(guardada,
                repositorio.buscarPorOrden(1).orElseThrow());
        assertTrue(repositorio.buscarPorNombre(null).isEmpty());
        assertTrue(repositorio.buscarPorOrden(99).isEmpty());
        assertTrue(repositorio.buscarPorId(99).isEmpty());
    }

    @Test
    void guardarActualizaYTodasDevuelveInstantanea() {
        ReglasPrioridadRepositorioMemoria repositorio =
                new ReglasPrioridadRepositorioMemoria();
        ReglaPrioridad guardada =
                repositorio.guardar(regla(0, "Urgente", 1));
        List<ReglaPrioridad> instantanea = repositorio.todas();
        ReglaPrioridad actualizada = regla(
                guardada.getId(), "Urgente", 2);

        repositorio.guardar(actualizada);

        assertEquals(1, instantanea.size());
        assertEquals(2, repositorio.todas().get(0).getOrden());
        instantanea.clear();
        assertEquals(1, repositorio.todas().size());
    }

    @Test
    void guardarRechazaReglaNulaOIdNegativo() {
        ReglasPrioridadRepositorioMemoria repositorio =
                new ReglasPrioridadRepositorioMemoria();

        assertThrows(IllegalArgumentException.class,
                () -> repositorio.guardar(null));
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.guardar(regla(-1, "Urgente", 1)));
        assertFalse(repositorio.todas().iterator().hasNext());
    }
}

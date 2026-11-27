package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class IncidenciasRepositorioMemoriaTest {

    @Test
    void asignaIdentificadoresYFiltraPorCita() {
        IncidenciasRepositorioMemoria repositorio =
                new IncidenciasRepositorioMemoria();
        LocalDateTime fecha =
                LocalDateTime.of(2026, 11, 27, 12, 0);

        IncidenciaCita primera = repositorio.guardar(
                new IncidenciaCita(
                        null, 1, 7, fecha, "Primera"));
        IncidenciaCita segunda = repositorio.guardar(
                new IncidenciaCita(
                        null, 1, 7, fecha, "Segunda"));
        repositorio.guardar(new IncidenciaCita(
                null, 2, 8, fecha, "Otra cita"));

        assertNotEquals(primera.getId(), segunda.getId());
        assertEquals(2, repositorio.listarPorCita(1).size());
        assertEquals(1, repositorio.listarPorCita(2).size());
    }
}

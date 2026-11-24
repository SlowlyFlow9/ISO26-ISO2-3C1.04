package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ValidadorHorarioServicioTest {

    @Test
    void parsearVariosIntervalos() throws Exception {
        var intervalos =
                ValidadorHorarioServicio.parsearIntervalos(
                        "08:00-12:00, 13:00-17:30");

        assertEquals(2, intervalos.size());
        assertEquals(LocalTime.of(13, 0),
                intervalos.get(1).getHoraInicio());
    }

    @Test
    void textoVacioRepresentaDiaCerrado() throws Exception {
        assertEquals(0,
                ValidadorHorarioServicio
                        .parsearIntervalos("  ").size());
    }

    @Test
    void rechazarFormatoIncorrecto() {
        assertThrows(FormatoHorarioServicioException.class,
                () -> ValidadorHorarioServicio
                        .parsearIntervalos("8:00-12:00"));
        assertThrows(FormatoHorarioServicioException.class,
                () -> ValidadorHorarioServicio
                        .parsearIntervalos("08:00"));
    }

    @Test
    void elClienteNoDuplicaValidacionDeNegocio() throws Exception {
        var intervalos =
                ValidadorHorarioServicio.parsearIntervalos(
                        "12:00-08:00");

        assertEquals(LocalTime.of(12, 0),
                intervalos.get(0).getHoraInicio());
    }
}

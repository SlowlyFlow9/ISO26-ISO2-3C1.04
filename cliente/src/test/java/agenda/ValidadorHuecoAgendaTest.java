package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/**
 * CU29 - Pruebas del validador de huecos.
 */

public class ValidadorHuecoAgendaTest {

    private HuecoAgenda datosValidos() {

        HuecoAgenda datos = new HuecoAgenda();
        datos.setAgendaId(1L);
        datos.setHoraInicio(LocalTime.of(9, 0));
        datos.setHoraFin(LocalTime.of(9, 30));

        return datos;
    }

    @Test
    void aceptarHuecoValido() {

        assertTrue(
                ValidadorHuecoAgenda
                        .validar(datosValidos())
                        .isEmpty());
    }

    @Test
    void rechazarDatosNulos() {

        assertEquals(1,
                ValidadorHuecoAgenda.validar(null).size());
    }

    @Test
    void rechazarIdentificadorAgendaInvalido() {

        HuecoAgenda datos = datosValidos();
        datos.setAgendaId(0L);

        assertFalse(
                ValidadorHuecoAgenda
                        .validar(datos)
                        .isEmpty());
    }

    @Test
    void rechazarHoraInicioNula() {

        HuecoAgenda datos = datosValidos();
        datos.setHoraInicio(null);

        assertEquals(1,
                ValidadorHuecoAgenda.validar(datos).size());
    }

    @Test
    void rechazarHoraFinNula() {

        HuecoAgenda datos = datosValidos();
        datos.setHoraFin(null);

        assertEquals(1,
                ValidadorHuecoAgenda.validar(datos).size());
    }

    @Test
    void rechazarHoraFinAnterior() {

        HuecoAgenda datos = datosValidos();
        datos.setHoraFin(LocalTime.of(8, 0));

        assertEquals(1,
                ValidadorHuecoAgenda.validar(datos).size());
    }

    @Test
    void rechazarHorasIguales() {

        HuecoAgenda datos = datosValidos();
        datos.setHoraFin(LocalTime.of(9, 0));

        assertEquals(1,
                ValidadorHuecoAgenda.validar(datos).size());
    }

    @Test
    void acumularVariosErrores() {

        HuecoAgenda datos = new HuecoAgenda();

        //Agenda 0 y ambas horas nulas
        assertEquals(3,
                ValidadorHuecoAgenda.validar(datos).size());
    }

}

package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CitaTest {

    @Test
    @DisplayName(
            "Una cita conserva todos sus datos")
    void conservaDatos() {

        Cita cita = new Cita();

        LocalDateTime fecha =
                LocalDateTime.of(
                        2026,
                        12,
                        1,
                        10,
                        30);

        cita.setId(1L);
        cita.setPacienteId(10L);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(fecha);
        cita.setEstado(
                EstadoCita.CONFIRMADA);

        assertAll(
                () -> assertEquals(
                        1L,
                        cita.getId()),

                () -> assertEquals(
                        10L,
                        cita.getPacienteId()),

                () -> assertEquals(
                        20L,
                        cita.getMedicoId()),

                () -> assertEquals(
                        30L,
                        cita.getTipoPruebaId()),

                () -> assertEquals(
                        fecha,
                        cita.getFechaHora()),

                () -> assertEquals(
                        EstadoCita.CONFIRMADA,
                        cita.getEstado())
        );
    }

    @Test
    @DisplayName(
            "La copia contiene los mismos datos pero es independiente")
    void copiaEsIndependiente() {

        Cita original = new Cita();

        original.setId(1L);
        original.setPacienteId(10L);
        original.setMedicoId(20L);
        original.setTipoPruebaId(30L);

        original.setFechaHora(
                LocalDateTime.of(
                        2026,
                        12,
                        1,
                        10,
                        30));

        original.setEstado(
                EstadoCita.PROGRAMADA);

        Cita copia =
                original.copia();

        assertNotSame(
                original,
                copia);

        assertEquals(
                original.getId(),
                copia.getId());

        copia.setEstado(
                EstadoCita.CANCELADA);

        assertEquals(
                EstadoCita.PROGRAMADA,
                original.getEstado());
    }

    @Test
    @DisplayName("La copia de una cita conserva el hueco y la solicitud (CU25)")
    void copiaConservaHuecoYSolicitud() {

        Cita original = new Cita();
        original.setHuecoId(5L);
        original.setSolicitudId(8L);

        Cita copia = original.copia();

        assertEquals(5L, copia.getHuecoId());
        assertEquals(8L, copia.getSolicitudId());
    }
}

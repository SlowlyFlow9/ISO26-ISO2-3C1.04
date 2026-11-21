package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CitasRepositorioMemoriaTest {

    private CitasRepositorioMemoria repositorio;

    @BeforeEach
    void setUp() {
        repositorio =
                new CitasRepositorioMemoria();
    }

    private Cita cita(long pacienteId) {

        Cita cita = new Cita();

        cita.setPacienteId(pacienteId);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);

        cita.setFechaHora(
                LocalDateTime.of(
                        2026,
                        12,
                        1,
                        10,
                        30));

        cita.setEstado(
                EstadoCita.PROGRAMADA);

        return cita;
    }

    @Test
    @DisplayName(
            "Guardar una cita nueva asigna un identificador")
    void guardarAsignaId() {

        Cita guardada =
                repositorio.guardar(
                        cita(1L));

        assertNotNull(
                guardada.getId());
    }

    @Test
    @DisplayName(
            "Buscar por ID devuelve la cita guardada")
    void buscarPorId() {

        Cita guardada =
                repositorio.guardar(
                        cita(1L));

        var resultado =
                repositorio.buscarPorId(
                        guardada.getId());

        assertTrue(resultado.isPresent());

        assertEquals(
                guardada.getId(),
                resultado.get().getId());
    }

    @Test
    @DisplayName(
            "Buscar un ID inexistente devuelve vacío")
    void buscarInexistente() {

        assertTrue(
                repositorio.buscarPorId(
                        999L)
                        .isEmpty());
    }

    @Test
    @DisplayName(
            "Listar por paciente devuelve solo sus citas")
    void listarPorPaciente() {

        repositorio.guardar(
                cita(1L));

        repositorio.guardar(
                cita(1L));

        repositorio.guardar(
                cita(2L));

        var resultado =
                repositorio.listarPorPaciente(
                        1L);

        assertEquals(
                2,
                resultado.size());

        assertTrue(
                resultado.stream()
                        .allMatch(c ->
                                c.getPacienteId()
                                        == 1L));
    }

    // ---------- CU25: cita activa de un hueco ----------

    private Cita citaEnHueco(Long huecoId, EstadoCita estado) {

        Cita cita = cita(10L);
        cita.setHuecoId(huecoId);
        cita.setEstado(estado);

        return repositorio.guardar(cita);
    }

    @Test
    @DisplayName("Una cita programada ocupa su hueco")
    void citaProgramadaOcupaElHueco() {

        Cita guardada = citaEnHueco(5L, EstadoCita.PROGRAMADA);

        assertEquals(
                guardada.getId(),
                repositorio.buscarActivaPorHueco(5L)
                        .orElseThrow().getId());
    }

    @Test
    @DisplayName("Una cita confirmada ocupa su hueco")
    void citaConfirmadaOcupaElHueco() {

        Cita guardada = citaEnHueco(5L, EstadoCita.CONFIRMADA);

        assertEquals(
                guardada.getId(),
                repositorio.buscarActivaPorHueco(5L)
                        .orElseThrow().getId());
    }

    @Test
    @DisplayName("Una cita cancelada no ocupa su hueco")
    void citaCanceladaNoOcupaElHueco() {

        citaEnHueco(5L, EstadoCita.CANCELADA);

        assertTrue(repositorio.buscarActivaPorHueco(5L).isEmpty());
    }

    @Test
    @DisplayName("Una cita realizada no ocupa su hueco")
    void citaRealizadaNoOcupaElHueco() {

        citaEnHueco(5L, EstadoCita.REALIZADA);

        assertTrue(repositorio.buscarActivaPorHueco(5L).isEmpty());
    }

    @Test
    @DisplayName("Un hueco sin citas no tiene cita activa")
    void huecoSinCitas() {

        citaEnHueco(5L, EstadoCita.PROGRAMADA);

        assertTrue(repositorio.buscarActivaPorHueco(6L).isEmpty());
    }

    @Test
    @DisplayName("Una cita sin hueco no se encuentra por ningún hueco")
    void citaSinHuecoNoSeEncuentra() {

        citaEnHueco(null, EstadoCita.PROGRAMADA);

        assertTrue(repositorio.buscarActivaPorHueco(5L).isEmpty());
    }

    @Test
    @DisplayName("La copia de una cita conserva el hueco y la solicitud")
    void copiaConservaHuecoYSolicitud() {

        Cita original = cita(10L);
        original.setHuecoId(5L);
        original.setSolicitudId(8L);

        Cita copia = original.copia();

        assertAll(
                () -> assertEquals(5L, copia.getHuecoId()),
                () -> assertEquals(8L, copia.getSolicitudId()));
    }
}

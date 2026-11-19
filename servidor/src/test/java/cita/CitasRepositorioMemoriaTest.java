package cita;

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
}
package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorAgendaMedicaTest {

    private AgendaMedica valida() {

        AgendaMedica agenda =
                new AgendaMedica();

        agenda.setMedicoId(1L);
        agenda.setFecha(
                LocalDate.of(2026, 11, 17));
        agenda.setHoraInicio(
                LocalTime.of(8, 0));
        agenda.setHoraFin(
                LocalTime.of(15, 0));

        return agenda;
    }

    @Test
    @DisplayName(
            "Una agenda válida no genera errores")
    void agendaValidaNoGeneraErrores() {

        assertTrue(
                ValidadorAgendaMedica
                        .validar(valida())
                        .isEmpty());
    }

    @Test
    @DisplayName(
            "Una agenda nula genera un error")
    void agendaNulaGeneraError() {

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(null)
                        .size());
    }

    @Test
    @DisplayName(
            "El médico es obligatorio")
    void medicoObligatorio() {

        AgendaMedica agenda = valida();
        agenda.setMedicoId(0);

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(agenda)
                        .size());
    }

    @Test
    @DisplayName(
            "La fecha es obligatoria")
    void fechaObligatoria() {

        AgendaMedica agenda = valida();
        agenda.setFecha(null);

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(agenda)
                        .size());
    }

    @Test
    @DisplayName(
            "La hora inicial es obligatoria")
    void horaInicioObligatoria() {

        AgendaMedica agenda = valida();
        agenda.setHoraInicio(null);

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(agenda)
                        .size());
    }

    @Test
    @DisplayName(
            "La hora final es obligatoria")
    void horaFinObligatoria() {

        AgendaMedica agenda = valida();
        agenda.setHoraFin(null);

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(agenda)
                        .size());
    }

    @Test
    @DisplayName(
            "La hora final debe ser posterior a la inicial")
    void horarioInvalidoGeneraError() {

        AgendaMedica agenda = valida();

        agenda.setHoraInicio(
                LocalTime.of(15, 0));

        agenda.setHoraFin(
                LocalTime.of(8, 0));

        assertEquals(
                1,
                ValidadorAgendaMedica
                        .validar(agenda)
                        .size());
    }
}
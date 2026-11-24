package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class HorarioServicioServiceImplTest {

    private HorarioServicioRepositorioMemoria repositorio;
    private HorarioServicioServiceImpl servicio;
    private Usuario administrador;

    @BeforeEach
    void preparar() {
        repositorio = new HorarioServicioRepositorioMemoria();
        servicio = new HorarioServicioServiceImpl(repositorio);
        administrador = new Usuario("admin", Rol.ADMINISTRADOR);
    }

    private HorarioServicio horarioBase() {
        HorarioServicio horario = HorarioServicio.vacio();
        horario.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0)));
        return horario;
    }

    @Test
    void consultarHorarioInicialIncluyeTodosLosDias() throws Exception {
        HorarioServicio horario = servicio.consultar(administrador);

        assertEquals(7, horario.getDias().size());
        assertTrue(horario.getDias().stream()
                .allMatch(dia -> dia.getIntervalos().isEmpty()));
    }

    @Test
    void modificarGuardaHorarioYAuditoria() throws Exception {
        HorarioServicio guardado =
                servicio.modificar(administrador, horarioBase());

        assertEquals("admin", guardado.getModificadoPor());
        assertNotNull(guardado.getModificadoEn());
        assertEquals(LocalTime.of(8, 0),
                guardado.getDias().get(0).getIntervalos()
                        .get(0).getHoraInicio());
        assertEquals(guardado.getModificadoPor(),
                servicio.consultar(administrador).getModificadoPor());
    }

    @Test
    void rechazarUsuarioSinPermisoAlConsultarOModificar() {
        Usuario medico = new Usuario("medico", Rol.MEDICO);

        assertThrows(SinPermisoException.class,
                () -> servicio.consultar(medico));
        assertThrows(SinPermisoException.class,
                () -> servicio.modificar(medico, horarioBase()));
    }

    @Test
    void rechazarDiaDuplicado() {
        HorarioServicio horario = horarioBase();
        horario.getDias().add(horario.getDias().get(0).copia());

        assertThrows(DatosHorarioServicioInvalidosException.class,
                () -> servicio.modificar(administrador, horario));
    }

    @Test
    void rechazarDiaFaltante() {
        HorarioServicio horario = horarioBase();
        horario.getDias().remove(0);

        assertThrows(DatosHorarioServicioInvalidosException.class,
                () -> servicio.modificar(administrador, horario));
    }

    @Test
    void rechazarIntervaloInvertido() {
        HorarioServicio horario = HorarioServicio.vacio();
        horario.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(12, 0),
                        LocalTime.of(8, 0)));

        assertThrows(DatosHorarioServicioInvalidosException.class,
                () -> servicio.modificar(administrador, horario));
    }

    @Test
    void rechazarIntervalosSolapadosPeroPermitirConsecutivos()
            throws Exception {
        HorarioServicio consecutivos = HorarioServicio.vacio();
        consecutivos.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0)));
        consecutivos.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)));
        servicio.modificar(administrador, consecutivos);

        HorarioServicio solapados = HorarioServicio.vacio();
        solapados.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0)));
        solapados.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0)));

        assertThrows(DatosHorarioServicioInvalidosException.class,
                () -> servicio.modificar(administrador, solapados));
    }

    @Test
    void rechazarListaDeIntervalosNula() {
        HorarioServicio horario = horarioBase();
        horario.getDias().get(0).setIntervalos(null);

        assertThrows(DatosHorarioServicioInvalidosException.class,
                () -> servicio.modificar(administrador, horario));
    }

    @Test
    void horarioVacioPermiteUnDiaCerrado() throws Exception {
        HorarioServicio horario = HorarioServicio.vacio();
        horario.getDias().stream()
                .filter(dia -> dia.getDia() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow()
                .setIntervalos(new ArrayList<>());

        assertEquals(7,
                servicio.modificar(administrador, horario)
                        .getDias().size());
    }
}

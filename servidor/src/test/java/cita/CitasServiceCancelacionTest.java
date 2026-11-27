package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agenda.AgendasRepositorioMemoria;
import agenda.HuecosAgendaRepositorioMemoria;
import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

/** CU04: cancelación de citas por el paciente (D-038). */
class CitasServiceCancelacionTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);
    private static final Usuario OTRO_PACIENTE =
            new Usuario("paciente2", Rol.PACIENTE);
    private static final Usuario MEDICO =
            new Usuario("medico1", Rol.MEDICO);

    private static final LocalDateTime CITA =
            LocalDateTime.of(2026, 12, 10, 10, 0);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private CitasRepositorioMemoria citas;
    private SolicitudesRepositorioMemoria solicitudes;
    private PoliticaCitaRepositorioMemoria politicas;
    private IdentidadPacienteMemoria identidad;

    private long solicitudId;
    private long citaId;

    @BeforeEach
    void preparar() {

        citas = new CitasRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();
        politicas = new PoliticaCitaRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();

        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        solicitudId = solicitudes.guardar(new SolicitudPrueba(
                0, 10L, 30L, 40L, "Dolor", "Control",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 20, 10, 0),
                EstadoSolicitud.CITADA)).getId();

        citaId = guardarCita(10L, EstadoCita.PROGRAMADA,
                solicitudId).getId();
    }

    private Cita guardarCita(
            long pacienteId,
            EstadoCita estado,
            Long solicitud) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(CITA);
        cita.setEstado(estado);
        cita.setHuecoId(5L);
        cita.setSolicitudId(solicitud);

        return citas.guardar(cita);
    }

    /** Servicio con el reloj fijado a las horas indicadas antes de la cita. */
    private CitasServiceImpl servicioA(long horasAntes) {

        Clock reloj = Clock.fixed(
                CITA.minusHours(horasAntes).atZone(ZONA).toInstant(),
                ZONA);

        return new CitasServiceImpl(
                citas,
                identidad,
                new CitasAdministracionServiceImpl(
                        citas,
                        solicitudes,
                        new PacientesRepositorioMemoria(),
                        new AgendasRepositorioMemoria(),
                        new HuecosAgendaRepositorioMemoria()),
                politicas,
                reloj);
    }

    private void fijarPlazo(int horas) {

        PoliticaCita politica = politicas.consultar();
        politica.setCancelacionMinimaHoras(horas);
        politicas.guardar(politica);
    }

    @Test
    void cancelarCitaProgramada() throws Exception {

        Cita cancelada =
                servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
        assertEquals(EstadoCita.CANCELADA,
                citas.buscarPorId(citaId).orElseThrow().getEstado());
    }

    @Test
    void cancelarCitaConfirmada() throws Exception {

        Cita confirmada = citas.buscarPorId(citaId)
                .orElseThrow().copia();
        confirmada.setEstado(EstadoCita.CONFIRMADA);
        citas.guardar(confirmada);

        Cita cancelada =
                servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void cancelarLiberaElHueco() throws Exception {

        assertTrue(citas.buscarActivaPorHueco(5L).isPresent());

        servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertTrue(citas.buscarActivaPorHueco(5L).isEmpty());
    }

    @Test
    void cancelarDevuelveLaSolicitudAPendiente() throws Exception {

        servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                solicitudes.buscarPorId(solicitudId)
                        .orElseThrow().getEstado());
    }

    @Test
    void cancelarConservaLosDatosDeLaCita() throws Exception {

        Cita cancelada =
                servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertEquals(citaId, cancelada.getId());
        assertEquals(10L, cancelada.getPacienteId());
        assertEquals(30L, cancelada.getMedicoId());
        assertEquals(40L, cancelada.getTipoPruebaId());
        assertEquals(CITA, cancelada.getFechaHora());
        assertEquals(5L, cancelada.getHuecoId());
    }

    @Test
    void cancelarCitaSinSolicitudFunciona() throws Exception {

        long sinSolicitud = guardarCita(
                10L, EstadoCita.PROGRAMADA, null).getId();

        Cita cancelada = servicioA(48)
                .cancelarMiCita(PACIENTE, sinSolicitud);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void rechazarCancelacionFueraDePlazo() {

        //la política inicial exige 24 horas de antelación
        assertThrows(CancelacionFueraDePlazoException.class,
                () -> servicioA(10).cancelarMiCita(PACIENTE, citaId));

        assertEquals(EstadoCita.PROGRAMADA,
                citas.buscarPorId(citaId).orElseThrow().getEstado());
        assertEquals(EstadoSolicitud.CITADA,
                solicitudes.buscarPorId(solicitudId)
                        .orElseThrow().getEstado());
    }

    @Test
    void permitirCancelarJustoEnElLimiteDelPlazo() throws Exception {

        Cita cancelada =
                servicioA(24).cancelarMiCita(PACIENTE, citaId);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void elPlazoSeLeeDeLaPolitica() throws Exception {

        fijarPlazo(0);

        Cita cancelada =
                servicioA(1).cancelarMiCita(PACIENTE, citaId);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void rechazarCitaYaCancelada() throws Exception {

        servicioA(48).cancelarMiCita(PACIENTE, citaId);

        assertThrows(CitaNoModificableException.class,
                () -> servicioA(48).cancelarMiCita(PACIENTE, citaId));
    }

    @Test
    void rechazarCitaRealizada() {

        long realizada = guardarCita(
                10L, EstadoCita.REALIZADA, null).getId();

        assertThrows(CitaNoModificableException.class,
                () -> servicioA(48).cancelarMiCita(PACIENTE, realizada));
    }

    @Test
    void citaCerradaInformaDelEstadoAunqueEsteFueraDePlazo() {

        long cancelada = guardarCita(
                10L, EstadoCita.CANCELADA, null).getId();

        assertThrows(CitaNoModificableException.class,
                () -> servicioA(1).cancelarMiCita(PACIENTE, cancelada));
    }

    @Test
    void rechazarCitaDeOtroPaciente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicioA(48)
                        .cancelarMiCita(OTRO_PACIENTE, citaId));

        assertEquals(EstadoCita.PROGRAMADA,
                citas.buscarPorId(citaId).orElseThrow().getEstado());
    }

    @Test
    void rechazarCitaInexistente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicioA(48).cancelarMiCita(PACIENTE, 999L));
    }

    @Test
    void rechazarUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicioA(48).cancelarMiCita(MEDICO, citaId));
    }

    @Test
    void rechazarUsuarioNulo() {

        assertThrows(SinPermisoException.class,
                () -> servicioA(48).cancelarMiCita(null, citaId));
    }

    @Test
    void rechazarPacienteSinVinculacion() {

        Usuario desconocido =
                new Usuario("desconocido", Rol.PACIENTE);

        assertThrows(PacienteNoVinculadoException.class,
                () -> servicioA(48)
                        .cancelarMiCita(desconocido, citaId));
    }
}

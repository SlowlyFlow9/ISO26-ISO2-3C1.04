package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agenda.AgendaMedica;
import agenda.AgendasRepositorioMemoria;
import agenda.HuecoAgenda;
import agenda.HuecoAgendaNoEncontradoException;
import agenda.HuecosAgendaRepositorioMemoria;
import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.SolicitudesRepositorioMemoria;

/** CU03: consulta de huecos libres y cambio de cita por el paciente (D-039). */
class CitasServiceCambioTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);
    private static final Usuario OTRO_PACIENTE =
            new Usuario("paciente2", Rol.PACIENTE);
    private static final Usuario MEDICO =
            new Usuario("medico1", Rol.MEDICO);

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 12, 1, 9, 0);

    private CitasRepositorioMemoria citas;
    private AgendasRepositorioMemoria agendas;
    private HuecosAgendaRepositorioMemoria huecos;
    private PoliticaCitaRepositorioMemoria politicas;
    private IdentidadPacienteMemoria identidad;
    private CitasServiceImpl servicio;

    private HuecoAgenda actual;      // 10/12 09:00, con la cita del paciente
    private HuecoAgenda libre;       // 10/12 10:00
    private HuecoAgenda bloqueado;   // 10/12 11:00
    private HuecoAgenda inactivo;    // agenda inactiva
    private HuecoAgenda cercano;     // 02/12 12:00, 27 horas después de ahora
    private HuecoAgenda lejano;      // 01/06/2027, a más de 90 días
    private long citaId;

    @BeforeEach
    void preparar() {

        citas = new CitasRepositorioMemoria();
        agendas = new AgendasRepositorioMemoria();
        huecos = new HuecosAgendaRepositorioMemoria();
        politicas = new PoliticaCitaRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();

        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        long agenda = crearAgenda(7L, LocalDate.of(2026, 12, 10), true);
        actual = crearHueco(agenda, LocalTime.of(9, 0), false);
        libre = crearHueco(agenda, LocalTime.of(10, 0), false);
        bloqueado = crearHueco(agenda, LocalTime.of(11, 0), true);

        inactivo = crearHueco(
                crearAgenda(8L, LocalDate.of(2026, 12, 11), false),
                LocalTime.of(9, 0), false);

        cercano = crearHueco(
                crearAgenda(9L, LocalDate.of(2026, 12, 2), true),
                LocalTime.of(12, 0), false);

        lejano = crearHueco(
                crearAgenda(9L, LocalDate.of(2027, 6, 1), true),
                LocalTime.of(9, 0), false);

        citaId = citas.guardar(
                nuevaCita(10L, EstadoCita.PROGRAMADA, actual)).getId();

        servicio = servicioA(AHORA);
    }

    private long crearAgenda(
            long medicoId, LocalDate dia, boolean activa) {

        AgendaMedica agenda = new AgendaMedica();
        agenda.setMedicoId(medicoId);
        agenda.setFecha(dia);
        agenda.setHoraInicio(LocalTime.of(8, 0));
        agenda.setHoraFin(LocalTime.of(14, 0));
        agenda.setActivo(activa);

        return agendas.guardar(agenda).getId();
    }

    private HuecoAgenda crearHueco(
            long agendaId, LocalTime inicio, boolean bloqueado) {

        HuecoAgenda hueco = new HuecoAgenda();
        hueco.setAgendaId(agendaId);
        hueco.setHoraInicio(inicio);
        hueco.setHoraFin(inicio.plusMinutes(30));
        hueco.setBloqueado(bloqueado);

        return huecos.guardar(hueco);
    }

    private Cita nuevaCita(
            long pacienteId, EstadoCita estado, HuecoAgenda hueco) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(7L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(LocalDateTime.of(
                2026, 12, 10, hueco.getHoraInicio().getHour(), 0));
        cita.setEstado(estado);
        cita.setHuecoId(hueco.getId());

        return cita;
    }

    /** Servicio con el reloj fijado al instante indicado. */
    private CitasServiceImpl servicioA(LocalDateTime ahora) {

        return new CitasServiceImpl(
                citas,
                identidad,
                new CitasAdministracionServiceImpl(
                        citas,
                        new SolicitudesRepositorioMemoria(),
                        new PacientesRepositorioMemoria(),
                        agendas,
                        huecos),
                politicas,
                Clock.fixed(ahora.atZone(ZONA).toInstant(), ZONA));
    }

    private void fijarPolitica(
            int antelacionMinimaHoras, int antelacionMaximaDias) {

        PoliticaCita politica = politicas.consultar();
        politica.setAntelacionMinimaHoras(antelacionMinimaHoras);
        politica.setAntelacionMaximaDias(antelacionMaximaDias);
        politicas.guardar(politica);
    }

    private List<Long> idsLibres(CitasServiceImpl s) throws Exception {

        return s.listarHuecosLibres(PACIENTE).stream()
                .map(HuecoLibre::id)
                .toList();
    }

    // ---------- huecos libres ----------

    @Test
    void listarSoloHuecosLibresYDisponibles() throws Exception {

        List<Long> ids = idsLibres(servicio);

        assertTrue(ids.contains(libre.getId()));
        assertFalse(ids.contains(actual.getId()));
        assertFalse(ids.contains(bloqueado.getId()));
        assertFalse(ids.contains(inactivo.getId()));
    }

    @Test
    void listarHuecosOrdenadosPorFechaYHora() throws Exception {

        List<Long> ids = idsLibres(servicio);

        assertEquals(List.of(cercano.getId(), libre.getId()), ids);
    }

    @Test
    void listarHuecosIncluyeLosDatosDelHueco() throws Exception {

        HuecoLibre hueco = servicio.listarHuecosLibres(PACIENTE)
                .stream()
                .filter(h -> h.id() == libre.getId())
                .findFirst().orElseThrow();

        assertEquals(7L, hueco.medicoId());
        assertEquals(LocalDateTime.of(2026, 12, 10, 10, 0),
                hueco.fechaHora());
    }

    @Test
    void listarExcluyeHuecosMasAllaDeLaVentana() throws Exception {

        assertFalse(idsLibres(servicio).contains(lejano.getId()));
    }

    @Test
    void listarRespetaLaAntelacionMinima() throws Exception {

        fijarPolitica(48, 90);

        List<Long> ids = idsLibres(servicio);

        assertFalse(ids.contains(cercano.getId()));
        assertTrue(ids.contains(libre.getId()));
    }

    @Test
    void listarRespetaLaVentanaMaxima() throws Exception {

        fijarPolitica(0, 5);

        List<Long> ids = idsLibres(servicio);

        assertTrue(ids.contains(cercano.getId()));
        assertFalse(ids.contains(libre.getId()));
    }

    @Test
    void listarIncluyeElHuecoQueSeLibera() throws Exception {

        servicio.cambiarMiCita(PACIENTE, citaId, libre.getId());

        List<Long> ids = idsLibres(servicio);

        assertTrue(ids.contains(actual.getId()));
        assertFalse(ids.contains(libre.getId()));
    }

    @Test
    void listarRechazaUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicio.listarHuecosLibres(MEDICO));
        assertThrows(SinPermisoException.class,
                () -> servicio.listarHuecosLibres(null));
    }

    @Test
    void listarRechazaPacienteSinVinculacion() {

        assertThrows(PacienteNoVinculadoException.class,
                () -> servicio.listarHuecosLibres(
                        new Usuario("desconocido", Rol.PACIENTE)));
    }

    // ---------- cambio de cita ----------

    @Test
    void cambiarCitaProgramada() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(
                PACIENTE, citaId, libre.getId());

        assertEquals(libre.getId(), cambiada.getHuecoId());
        assertEquals(LocalDateTime.of(2026, 12, 10, 10, 0),
                cambiada.getFechaHora());
        assertEquals(EstadoCita.PROGRAMADA, cambiada.getEstado());
        assertEquals(libre.getId(),
                citas.buscarPorId(citaId).orElseThrow().getHuecoId());
    }

    @Test
    void cambiarCitaConfirmadaLaDejaProgramada() throws Exception {

        Cita confirmada = citas.buscarPorId(citaId)
                .orElseThrow().copia();
        confirmada.setEstado(EstadoCita.CONFIRMADA);
        citas.guardar(confirmada);

        Cita cambiada = servicio.cambiarMiCita(
                PACIENTE, citaId, libre.getId());

        assertEquals(EstadoCita.PROGRAMADA, cambiada.getEstado());
    }

    @Test
    void cambiarLiberaElHuecoAnteriorYOcupaElNuevo() throws Exception {

        servicio.cambiarMiCita(PACIENTE, citaId, libre.getId());

        assertTrue(citas.buscarActivaPorHueco(actual.getId())
                .isEmpty());
        assertTrue(citas.buscarActivaPorHueco(libre.getId())
                .isPresent());
    }

    @Test
    void cambiarConservaLosDatosDeLaCita() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(
                PACIENTE, citaId, libre.getId());

        assertEquals(citaId, cambiada.getId());
        assertEquals(10L, cambiada.getPacienteId());
        assertEquals(40L, cambiada.getTipoPruebaId());
    }

    @Test
    void rechazarHuecoBloqueado() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, bloqueado.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoDeAgendaInactiva() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, inactivo.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoOcupadoPorOtraCita() {

        citas.guardar(nuevaCita(20L, EstadoCita.PROGRAMADA, libre));

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, libre.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarElMismoHueco() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, actual.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoInexistente() {

        assertThrows(HuecoAgendaNoEncontradoException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, 999L));

        assertSinCambios();
    }

    @Test
    void rechazarCitaCancelada() {

        long cancelada = citas.guardar(
                nuevaCita(10L, EstadoCita.CANCELADA, bloqueado))
                .getId();

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, cancelada, libre.getId()));
    }

    @Test
    void rechazarCitaRealizada() {

        long realizada = citas.guardar(
                nuevaCita(10L, EstadoCita.REALIZADA, bloqueado))
                .getId();

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, realizada, libre.getId()));
    }

    @Test
    void citaCerradaInformaDelEstadoAunqueElHuecoNoExista() {

        long cancelada = citas.guardar(
                nuevaCita(10L, EstadoCita.CANCELADA, bloqueado))
                .getId();

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, cancelada, 999L));
    }

    @Test
    void rechazarCitaDeOtroPaciente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cambiarMiCita(
                        OTRO_PACIENTE, citaId, libre.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarCitaInexistente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, 999L, libre.getId()));
    }

    @Test
    void rechazarUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicio.cambiarMiCita(
                        MEDICO, citaId, libre.getId()));
        assertThrows(SinPermisoException.class,
                () -> servicio.cambiarMiCita(
                        null, citaId, libre.getId()));
    }

    @Test
    void rechazarPacienteSinVinculacion() {

        assertThrows(PacienteNoVinculadoException.class,
                () -> servicio.cambiarMiCita(
                        new Usuario("desconocido", Rol.PACIENTE),
                        citaId, libre.getId()));
    }

    // ---------- política de citación ----------

    @Test
    void rechazarCambioConLaCitaFueraDelPlazoDeCancelacion() {

        //la cita es el 10/12 a las 09:00 y faltan 21 horas
        CitasServiceImpl tarde =
                servicioA(LocalDateTime.of(2026, 12, 9, 12, 0));

        assertThrows(CambioFueraDePoliticaException.class,
                () -> tarde.cambiarMiCita(
                        PACIENTE, citaId, libre.getId()));

        assertSinCambios();
    }

    @Test
    void permitirCambioJustoEnElLimiteDelPlazo() throws Exception {

        CitasServiceImpl limite =
                servicioA(LocalDateTime.of(2026, 12, 9, 9, 0));

        Cita cambiada = limite.cambiarMiCita(
                PACIENTE, citaId, libre.getId());

        assertEquals(libre.getId(), cambiada.getHuecoId());
    }

    @Test
    void rechazarHuecoNuevoAntesDeLaAntelacionMinima() {

        fijarPolitica(48, 90);

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, cercano.getId()));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoNuevoMasAllaDeLaVentanaMaxima() {

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, lejano.getId()));

        assertSinCambios();
    }

    @Test
    void laVentanaSeLeeDeLaPolitica() {

        fijarPolitica(0, 5);

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, citaId, libre.getId()));
    }

    @Test
    void permitirHuecoNuevoDentroDeLaVentana() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(
                PACIENTE, citaId, cercano.getId());

        assertEquals(cercano.getId(), cambiada.getHuecoId());
    }

    private void assertSinCambios() {

        Cita cita = citas.buscarPorId(citaId).orElseThrow();

        assertEquals(actual.getId(), cita.getHuecoId());
        assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
    }
}

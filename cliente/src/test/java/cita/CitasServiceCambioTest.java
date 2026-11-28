package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/** CU03: reglas del cambio de cita en el servicio simulado del cliente (D-039). */
class CitasServiceCambioTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime CITA =
            LocalDateTime.of(2026, 12, 10, 9, 0);

    private static final long HUECO_ACTUAL = 100L;
    private static final long HUECO_CONFIRMADA = 101L;
    private static final long LIBRE = 200L;
    private static final long LIBRE_TARDE = 201L;
    private static final long CERCANO = 202L;
    private static final long LEJANO = 203L;

    private CitasServiceSimulado servicio;

    @BeforeEach
    void preparar() {

        servicio = new CitasServiceSimulado();
        servicio.vincularPaciente("paciente1", 10L);
        servicio.vincularPaciente("paciente2", 20L);

        servicio.agregarHuecoLibre(hueco(HUECO_ACTUAL, 7L, CITA));
        servicio.agregarHuecoLibre(hueco(HUECO_CONFIRMADA, 7L,
                CITA.plusDays(1)));
        servicio.agregarHuecoLibre(hueco(LIBRE, 8L,
                CITA.plusHours(1)));
        servicio.agregarHuecoLibre(hueco(LIBRE_TARDE, 8L,
                CITA.plusHours(2)));
        servicio.agregarHuecoLibre(hueco(CERCANO, 9L,
                LocalDateTime.of(2026, 12, 2, 12, 0)));
        servicio.agregarHuecoLibre(hueco(LEJANO, 9L,
                LocalDateTime.of(2027, 6, 1, 9, 0)));

        //citas 1 a 5
        servicio.agregarCita(crearCita(10L, EstadoCita.PROGRAMADA,
                HUECO_ACTUAL));
        servicio.agregarCita(crearCita(10L, EstadoCita.CONFIRMADA,
                HUECO_CONFIRMADA));
        servicio.agregarCita(crearCita(10L, EstadoCita.CANCELADA,
                null));
        servicio.agregarCita(crearCita(10L, EstadoCita.REALIZADA,
                null));
        servicio.agregarCita(crearCita(20L, EstadoCita.PROGRAMADA,
                null));

        fijarAhora(AHORA);
    }

    private HuecoLibre hueco(
            long id, long medicoId, LocalDateTime fecha) {

        return new HuecoLibre(id, medicoId, fecha);
    }

    private Cita crearCita(
            long pacienteId, EstadoCita estado, Long huecoId) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(7L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(CITA);
        cita.setEstado(estado);
        cita.setHuecoId(huecoId);

        return cita;
    }

    private void fijarAhora(LocalDateTime ahora) {

        servicio.fijarReloj(Clock.fixed(
                ahora.atZone(ZONA).toInstant(), ZONA));
    }

    private List<Long> idsLibres() throws Exception {

        return servicio.listarHuecosLibres(PACIENTE).stream()
                .map(HuecoLibre::id)
                .toList();
    }

    // ---------- huecos libres ----------

    @Test
    void listarSoloLosHuecosSinCitaActiva() throws Exception {

        List<Long> ids = idsLibres();

        assertTrue(ids.contains(LIBRE));
        assertFalse(ids.contains(HUECO_ACTUAL));
        assertFalse(ids.contains(HUECO_CONFIRMADA));
    }

    @Test
    void listarOrdenadosPorFechaYHora() throws Exception {

        assertEquals(List.of(CERCANO, LIBRE, LIBRE_TARDE),
                idsLibres());
    }

    @Test
    void listarExcluyeLosHuecosFueraDeLaVentana() throws Exception {

        assertFalse(idsLibres().contains(LEJANO));

        servicio.fijarVentana(48, 90);
        assertFalse(idsLibres().contains(CERCANO));

        servicio.fijarVentana(0, 5);
        assertTrue(idsLibres().contains(CERCANO));
        assertFalse(idsLibres().contains(LIBRE));
    }

    @Test
    void listarIncluyeElHuecoQueSeLibera() throws Exception {

        servicio.cambiarMiCita(PACIENTE, 1L, LIBRE);

        List<Long> ids = idsLibres();

        assertTrue(ids.contains(HUECO_ACTUAL));
        assertFalse(ids.contains(LIBRE));
    }

    @Test
    void listarRechazaUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicio.listarHuecosLibres(
                        new Usuario("medico1", Rol.MEDICO)));
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

        Cita cambiada = servicio.cambiarMiCita(PACIENTE, 1L, LIBRE);

        assertEquals(LIBRE, cambiada.getHuecoId());
        assertEquals(CITA.plusHours(1), cambiada.getFechaHora());
        assertEquals(8L, cambiada.getMedicoId());
        assertEquals(EstadoCita.PROGRAMADA, cambiada.getEstado());
        assertEquals(LIBRE,
                servicio.consultarMiCita(PACIENTE, 1L).getHuecoId());
    }

    @Test
    void cambiarCitaConfirmadaLaDejaProgramada() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(PACIENTE, 2L, LIBRE);

        assertEquals(EstadoCita.PROGRAMADA, cambiada.getEstado());
    }

    @Test
    void cambiarConservaLosDatosDeLaCita() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(PACIENTE, 1L, LIBRE);

        assertEquals(1L, cambiada.getId());
        assertEquals(10L, cambiada.getPacienteId());
        assertEquals(40L, cambiada.getTipoPruebaId());
    }

    @Test
    void rechazarHuecoInexistente() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 999L));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoOcupadoPorOtraCita() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, 1L, HUECO_CONFIRMADA));

        assertSinCambios();
    }

    @Test
    void rechazarElMismoHueco() {

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(
                        PACIENTE, 1L, HUECO_ACTUAL));

        assertSinCambios();
    }

    @Test
    void rechazarCitaCancelada() {

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 3L, LIBRE));
    }

    @Test
    void rechazarCitaRealizada() {

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 4L, LIBRE));
    }

    @Test
    void citaCerradaInformaDelEstadoAunqueElHuecoNoExista() {

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 3L, 999L));
    }

    @Test
    void rechazarCitaDeOtroPaciente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 5L, LIBRE));
    }

    @Test
    void rechazarCitaInexistente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 999L, LIBRE));
    }

    @Test
    void rechazarUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicio.cambiarMiCita(
                        new Usuario("medico1", Rol.MEDICO),
                        1L, LIBRE));
    }

    @Test
    void rechazarPacienteSinVinculacion() {

        assertThrows(PacienteNoVinculadoException.class,
                () -> servicio.cambiarMiCita(
                        new Usuario("desconocido", Rol.PACIENTE),
                        1L, LIBRE));
    }

    // ---------- política de citación ----------

    @Test
    void rechazarCambioConLaCitaFueraDelPlazo() {

        //la cita es el 10/12 a las 09:00 y faltan 21 horas
        fijarAhora(LocalDateTime.of(2026, 12, 9, 12, 0));

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, LIBRE));

        assertSinCambios();
    }

    @Test
    void permitirCambioJustoEnElLimiteDelPlazo() throws Exception {

        fijarAhora(LocalDateTime.of(2026, 12, 9, 9, 0));

        assertEquals(LIBRE,
                servicio.cambiarMiCita(PACIENTE, 1L, LIBRE)
                        .getHuecoId());
    }

    @Test
    void rechazarHuecoAntesDeLaAntelacionMinima() {

        servicio.fijarVentana(48, 90);

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, CERCANO));

        assertSinCambios();
    }

    @Test
    void rechazarHuecoMasAllaDeLaVentanaMaxima() {

        assertThrows(CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, LEJANO));

        assertSinCambios();
    }

    @Test
    void permitirHuecoDentroDeLaVentana() throws Exception {

        assertEquals(CERCANO,
                servicio.cambiarMiCita(PACIENTE, 1L, CERCANO)
                        .getHuecoId());
    }

    private void assertSinCambios() {

        try {
            Cita cita = servicio.consultarMiCita(PACIENTE, 1L);

            assertEquals(HUECO_ACTUAL, cita.getHuecoId());
            assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());

        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}

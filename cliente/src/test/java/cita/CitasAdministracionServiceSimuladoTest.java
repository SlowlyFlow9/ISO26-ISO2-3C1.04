package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Paciente;
import paciente.PacienteNoEncontradoException;
import paciente.PacientesServiceSimulado;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;

/** CU25: servicio simulado de gestión de citas (mismas reglas que el servidor). */
class CitasAdministracionServiceSimuladoTest {

    private static final Usuario ADMIN =
            new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final Usuario PACIENTE =
            new Usuario("paciente", Rol.PACIENTE);
    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO);
    private static final Usuario ADMINISTRADOR =
            new Usuario("administrador", Rol.ADMINISTRADOR);

    private static final long MEDICO_AGENDA = 7L;
    private static final long TIPO_PRUEBA = 3L;
    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    private PacientesServiceSimulado pacientes;
    private CitasAdministracionServiceSimulado servicio;

    private Paciente paciente;
    private SolicitudPrueba solicitud;

    @BeforeEach
    void setUp() throws Exception {

        pacientes = new PacientesServiceSimulado();
        servicio = new CitasAdministracionServiceSimulado(pacientes);

        paciente = crearPaciente("12345678Z");
        solicitud = crearSolicitud(
                1, paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);

        servicio.agregarHueco(1, DIA, LocalTime.of(9, 0),
                LocalTime.of(9, 30), MEDICO_AGENDA, false, true);
        servicio.agregarHueco(2, DIA, LocalTime.of(9, 30),
                LocalTime.of(10, 0), MEDICO_AGENDA, false, true);
    }

    // ---------- datos de prueba ----------

    private Paciente crearPaciente(String dni) throws Exception {
        Paciente datos = new Paciente();
        datos.setNombre("Ana");
        datos.setApellidos("García");
        datos.setDni(dni);
        datos.setFechaNacimiento(LocalDate.of(1980, 5, 20));
        datos.setTelefono("600123456");
        return pacientes.crear(ADMIN, datos);
    }

    private SolicitudPrueba crearSolicitud(
            long id, long pacienteId, EstadoSolicitud estado) {
        SolicitudPrueba nueva = new SolicitudPrueba(
                id, pacienteId, 5, TIPO_PRUEBA, "Dolor", "Control",
                Prioridad.ORDINARIA, LocalDateTime.of(2026, 11, 20, 10, 0),
                estado);
        servicio.agregarSolicitud(nueva);
        return nueva;
    }

    private Cita asignarCita() throws Exception {
        return servicio.asignar(ADMIN, solicitud.getId(), 1);
    }

    private Cita citaEnEstado(EstadoCita estado) throws Exception {
        Cita cita = asignarCita().copia();
        cita.setEstado(estado);
        servicio.agregarCita(cita);
        return cita;
    }

    // ---------- asignar ----------

    @Test
    @DisplayName("Asignar crea una cita programada con los datos de la solicitud y del hueco")
    void asignarCreaCitaProgramada() throws Exception {
        // Ejecutar
        Cita cita = asignarCita();
        // Comprobar
        assertAll(
                () -> assertNotNull(cita.getId()),
                () -> assertEquals(EstadoCita.PROGRAMADA, cita.getEstado()),
                () -> assertEquals(paciente.getId(), cita.getPacienteId()),
                () -> assertEquals(MEDICO_AGENDA, cita.getMedicoId()),
                () -> assertEquals(TIPO_PRUEBA, cita.getTipoPruebaId()),
                () -> assertEquals(LocalDateTime.of(DIA, LocalTime.of(9, 0)),
                        cita.getFechaHora()),
                () -> assertEquals(1L, cita.getHuecoId()),
                () -> assertEquals(solicitud.getId(), cita.getSolicitudId()));
    }

    @Test
    @DisplayName("Asignar guarda la cita para poder consultarla después")
    void asignarGuardaLaCita() throws Exception {
        // Ejecutar
        Cita cita = asignarCita();
        // Comprobar
        assertEquals(cita.getId(),
                servicio.consultarCita(ADMIN, cita.getId()).getId());
    }

    @Test
    @DisplayName("Asignar pasa la solicitud a CITADA")
    void asignarMarcaLaSolicitudComoCitada() throws Exception {
        // Ejecutar
        asignarCita();
        // Comprobar
        assertEquals(EstadoSolicitud.CITADA,
                servicio.estadoDeSolicitud(solicitud.getId()));
    }

    @Test
    @DisplayName("Un paciente no puede asignar citas")
    void pacienteNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(PACIENTE, solicitud.getId(), 1));
    }

    @Test
    @DisplayName("Un médico no puede asignar citas")
    void medicoNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(MEDICO, solicitud.getId(), 1));
    }

    @Test
    @DisplayName("Un administrador del sistema no puede asignar citas")
    void administradorNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(ADMINISTRADOR, solicitud.getId(), 1));
    }

    @Test
    @DisplayName("Un usuario nulo no puede asignar citas")
    void usuarioNuloNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(null, solicitud.getId(), 1));
    }

    @Test
    @DisplayName("Asignar sin permiso no cambia la solicitud")
    void asignarSinPermisoNoCambiaNada() {
        // Ejecutar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(MEDICO, solicitud.getId(), 1));
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                servicio.estadoDeSolicitud(solicitud.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud inexistente")
    void asignarSolicitudInexistente() {
        // Comprobar
        assertThrows(SolicitudNoEncontradaException.class,
                () -> servicio.asignar(ADMIN, 999L, 1));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud que ya está citada")
    void asignarSolicitudYaCitada() throws Exception {
        // Preparar
        asignarCita();
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN, solicitud.getId(), 2));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud anulada")
    void asignarSolicitudAnulada() {
        // Preparar
        SolicitudPrueba anulada = crearSolicitud(
                2, paciente.getId(), EstadoSolicitud.ANULADA);
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN, anulada.getId(), 1));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud ya realizada")
    void asignarSolicitudRealizada() {
        // Preparar
        SolicitudPrueba realizada = crearSolicitud(
                2, paciente.getId(), EstadoSolicitud.REALIZADA);
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN, realizada.getId(), 1));
    }

    @Test
    @DisplayName("No se puede asignar cita si el paciente de la solicitud no existe")
    void asignarPacienteInexistente() {
        // Preparar
        SolicitudPrueba huerfana = crearSolicitud(
                2, 999L, EstadoSolicitud.PENDIENTE_CITACION);
        // Comprobar
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.asignar(ADMIN, huerfana.getId(), 1));
    }

    @Test
    @DisplayName("No se puede dar cita a un paciente dado de baja")
    void asignarPacienteDeBaja() throws Exception {
        // Preparar
        Paciente deBaja = crearPaciente("87654321X");
        pacientes.desactivar(ADMIN, deBaja.getId());
        SolicitudPrueba solicitudBaja = crearSolicitud(
                2, deBaja.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Ejecutar
        assertThrows(PacienteInactivoException.class,
                () -> servicio.asignar(ADMIN, solicitudBaja.getId(), 1));
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                servicio.estadoDeSolicitud(solicitudBaja.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco inexistente")
    void asignarHuecoInexistente() {
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN, solicitud.getId(), 999L));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco bloqueado")
    void asignarHuecoBloqueado() {
        // Preparar
        servicio.agregarHueco(3, DIA, LocalTime.of(10, 0),
                LocalTime.of(10, 30), MEDICO_AGENDA, true, true);
        // Ejecutar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN, solicitud.getId(), 3));
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                servicio.estadoDeSolicitud(solicitud.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco de una agenda inactiva")
    void asignarAgendaInactiva() {
        // Preparar
        servicio.agregarHueco(3, DIA, LocalTime.of(10, 0),
                LocalTime.of(10, 30), MEDICO_AGENDA, false, false);
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN, solicitud.getId(), 3));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco que ya está ocupado")
    void asignarHuecoOcupado() throws Exception {
        // Preparar
        asignarCita();
        SolicitudPrueba otra = crearSolicitud(
                2, paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN, otra.getId(), 1));
    }

    // ---------- reprogramar ----------

    @Test
    @DisplayName("Reprogramar mueve la cita al nuevo hueco y actualiza fecha, médico y hueco")
    void reprogramarMueveLaCita() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        Cita movida = servicio.reprogramar(ADMIN, cita.getId(), 2);
        // Comprobar
        assertAll(
                () -> assertEquals(cita.getId(), movida.getId()),
                () -> assertEquals(2L, movida.getHuecoId()),
                () -> assertEquals(LocalDateTime.of(DIA, LocalTime.of(9, 30)),
                        movida.getFechaHora()),
                () -> assertEquals(MEDICO_AGENDA, movida.getMedicoId()),
                () -> assertEquals(EstadoCita.PROGRAMADA, movida.getEstado()));
    }

    @Test
    @DisplayName("Reprogramar libera el hueco anterior")
    void reprogramarLiberaElHuecoAnterior() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        servicio.reprogramar(ADMIN, cita.getId(), 2);
        // Comprobar
        assertTrue(servicio.listarHuecosLibres(ADMIN).stream()
                .anyMatch(h -> h.id() == 1));
    }

    @Test
    @DisplayName("Reprogramar una cita confirmada la deja programada de nuevo")
    void reprogramarCitaConfirmada() throws Exception {
        // Preparar
        Cita confirmada = citaEnEstado(EstadoCita.CONFIRMADA);
        // Ejecutar
        Cita movida = servicio.reprogramar(ADMIN, confirmada.getId(), 2);
        // Comprobar
        assertEquals(EstadoCita.PROGRAMADA, movida.getEstado());
    }

    @Test
    @DisplayName("No se puede reprogramar al mismo hueco en el que ya está la cita")
    void reprogramarAlMismoHueco() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(ADMIN, cita.getId(), 1));
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco ocupado por otra cita")
    void reprogramarAHuecoOcupado() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        SolicitudPrueba otra = crearSolicitud(
                2, paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        servicio.asignar(ADMIN, otra.getId(), 2);
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(ADMIN, cita.getId(), 2));
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco bloqueado")
    void reprogramarAHuecoBloqueado() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        servicio.agregarHueco(3, DIA, LocalTime.of(10, 0),
                LocalTime.of(10, 30), MEDICO_AGENDA, true, true);
        // Ejecutar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(ADMIN, cita.getId(), 3));
        // Comprobar
        assertEquals(1L,
                servicio.consultarCita(ADMIN, cita.getId()).getHuecoId());
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco inexistente")
    void reprogramarAHuecoInexistente() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(ADMIN, cita.getId(), 999L));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita inexistente")
    void reprogramarCitaInexistente() {
        // Comprobar
        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.reprogramar(ADMIN, 999L, 2));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita cancelada")
    void reprogramarCitaCancelada() throws Exception {
        // Preparar
        Cita cancelada = citaEnEstado(EstadoCita.CANCELADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.reprogramar(ADMIN, cancelada.getId(), 2));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita realizada")
    void reprogramarCitaRealizada() throws Exception {
        // Preparar
        Cita realizada = citaEnEstado(EstadoCita.REALIZADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.reprogramar(ADMIN, realizada.getId(), 2));
    }

    @Test
    @DisplayName("Un médico no puede reprogramar y la cita no cambia")
    void medicoNoPuedeReprogramar() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        assertThrows(SinPermisoException.class,
                () -> servicio.reprogramar(MEDICO, cita.getId(), 2));
        // Comprobar
        assertEquals(1L,
                servicio.consultarCita(ADMIN, cita.getId()).getHuecoId());
    }

    // ---------- cancelar ----------

    @Test
    @DisplayName("Cancelar pasa la cita a CANCELADA")
    void cancelarPasaACancelada() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        Cita cancelada = servicio.cancelar(ADMIN, cita.getId());
        // Comprobar
        assertAll(
                () -> assertEquals(EstadoCita.CANCELADA, cancelada.getEstado()),
                () -> assertEquals(EstadoCita.CANCELADA,
                        servicio.consultarCita(ADMIN, cita.getId())
                                .getEstado()));
    }

    @Test
    @DisplayName("Cancelar libera el hueco para otra cita")
    void cancelarLiberaElHueco() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        servicio.cancelar(ADMIN, cita.getId());
        SolicitudPrueba otra = crearSolicitud(
                2, paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Ejecutar
        Cita nueva = servicio.asignar(ADMIN, otra.getId(), 1);
        // Comprobar
        assertEquals(1L, nueva.getHuecoId());
    }

    @Test
    @DisplayName("Cancelar devuelve la solicitud a PENDIENTE_CITACION")
    void cancelarDevuelveLaSolicitudAPendiente() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        servicio.cancelar(ADMIN, cita.getId());
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                servicio.estadoDeSolicitud(solicitud.getId()));
    }

    @Test
    @DisplayName("Se puede cancelar una cita confirmada")
    void cancelarCitaConfirmada() throws Exception {
        // Preparar
        Cita confirmada = citaEnEstado(EstadoCita.CONFIRMADA);
        // Ejecutar
        Cita cancelada = servicio.cancelar(ADMIN, confirmada.getId());
        // Comprobar
        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    @DisplayName("No se puede cancelar una cita que ya está cancelada")
    void cancelarCitaYaCancelada() throws Exception {
        // Preparar
        Cita cancelada = citaEnEstado(EstadoCita.CANCELADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.cancelar(ADMIN, cancelada.getId()));
    }

    @Test
    @DisplayName("No se puede cancelar una cita realizada")
    void cancelarCitaRealizada() throws Exception {
        // Preparar
        Cita realizada = citaEnEstado(EstadoCita.REALIZADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.cancelar(ADMIN, realizada.getId()));
    }

    @Test
    @DisplayName("No se puede cancelar una cita inexistente")
    void cancelarCitaInexistente() {
        // Comprobar
        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cancelar(ADMIN, 999L));
    }

    @Test
    @DisplayName("Un paciente no puede cancelar desde administración y la cita sigue activa")
    void pacienteNoPuedeCancelar() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        assertThrows(SinPermisoException.class,
                () -> servicio.cancelar(PACIENTE, cita.getId()));
        // Comprobar
        assertEquals(EstadoCita.PROGRAMADA,
                servicio.consultarCita(ADMIN, cita.getId()).getEstado());
    }

    @Test
    @DisplayName("Cancelar una cita sin solicitud asociada no falla")
    void cancelarCitaSinSolicitud() throws Exception {
        // Preparar
        Cita suelta = new Cita();
        suelta.setPacienteId(paciente.getId());
        suelta.setMedicoId(MEDICO_AGENDA);
        suelta.setTipoPruebaId(TIPO_PRUEBA);
        suelta.setFechaHora(LocalDateTime.of(DIA, LocalTime.of(12, 0)));
        suelta.setEstado(EstadoCita.PROGRAMADA);
        Cita guardada = servicio.agregarCita(suelta);
        // Ejecutar
        Cita cancelada = servicio.cancelar(ADMIN, guardada.getId());
        // Comprobar
        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    @DisplayName("Cancelar no cambia una solicitud que ya no está citada")
    void cancelarNoTocaUnaSolicitudAnulada() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        servicio.agregarSolicitud(new SolicitudPrueba(
                solicitud.getId(), paciente.getId(), 5, TIPO_PRUEBA,
                "Dolor", "Control", Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 20, 10, 0),
                EstadoSolicitud.ANULADA));
        // Ejecutar
        servicio.cancelar(ADMIN, cita.getId());
        // Comprobar
        assertEquals(EstadoSolicitud.ANULADA,
                servicio.estadoDeSolicitud(solicitud.getId()));
    }

    // ---------- listar y consultar ----------

    @Test
    @DisplayName("Listar devuelve solo las citas del paciente indicado")
    void listarDevuelveLasCitasDelPaciente() throws Exception {
        // Preparar
        asignarCita();
        Paciente otro = crearPaciente("87654321X");
        SolicitudPrueba solicitudOtro = crearSolicitud(
                2, otro.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        servicio.asignar(ADMIN, solicitudOtro.getId(), 2);
        // Ejecutar
        List<Cita> lista = servicio.listarCitasDePaciente(
                ADMIN, paciente.getId());
        // Comprobar
        assertAll(
                () -> assertEquals(1, lista.size()),
                () -> assertEquals(paciente.getId(),
                        lista.get(0).getPacienteId()));
    }

    @Test
    @DisplayName("Listar las citas de un paciente sin citas devuelve una lista vacía")
    void listarPacienteSinCitas() throws Exception {
        // Ejecutar
        List<Cita> lista = servicio.listarCitasDePaciente(
                ADMIN, paciente.getId());
        // Comprobar
        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("Listar las citas de un paciente inexistente produce un error")
    void listarPacienteInexistente() {
        // Comprobar
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.listarCitasDePaciente(ADMIN, 999L));
    }

    @Test
    @DisplayName("Un médico no puede listar las citas de un paciente")
    void medicoNoPuedeListar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.listarCitasDePaciente(
                        MEDICO, paciente.getId()));
    }

    @Test
    @DisplayName("Consultar devuelve la cita pedida")
    void consultarDevuelveLaCita() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        Cita consultada = servicio.consultarCita(ADMIN, cita.getId());
        // Comprobar
        assertAll(
                () -> assertEquals(cita.getId(), consultada.getId()),
                () -> assertEquals(paciente.getId(),
                        consultada.getPacienteId()));
    }

    @Test
    @DisplayName("Consultar una cita inexistente produce un error")
    void consultarCitaInexistente() {
        // Comprobar
        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.consultarCita(ADMIN, 999L));
    }

    @Test
    @DisplayName("Un paciente no puede consultar citas desde administración")
    void pacienteNoPuedeConsultar() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.consultarCita(PACIENTE, cita.getId()));
    }

    @Test
    @DisplayName("Modificar el resultado devuelto no cambia la cita guardada")
    void elResultadoEsUnaCopia() throws Exception {
        // Preparar
        Cita devuelta = asignarCita();
        // Ejecutar
        devuelta.setEstado(EstadoCita.REALIZADA);
        // Comprobar
        assertFalse(EstadoCita.REALIZADA
                == servicio.consultarCita(ADMIN, devuelta.getId())
                        .getEstado());
    }

    // ---------- listas para la pantalla ----------

    @Test
    @DisplayName("Las solicitudes pendientes son solo las del paciente que están pendientes de citación")
    void listarSolicitudesPendientes() throws Exception {
        // Preparar
        crearSolicitud(2, paciente.getId(), EstadoSolicitud.CITADA);
        Paciente otro = crearPaciente("87654321X");
        crearSolicitud(3, otro.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Ejecutar
        List<SolicitudPrueba> pendientes =
                servicio.listarSolicitudesPendientes(
                        ADMIN, paciente.getId());
        // Comprobar
        assertAll(
                () -> assertEquals(1, pendientes.size()),
                () -> assertEquals(solicitud.getId(),
                        pendientes.get(0).getId()));
    }

    @Test
    @DisplayName("Listar las solicitudes pendientes de un paciente inexistente produce un error")
    void listarSolicitudesPacienteInexistente() {
        // Comprobar
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.listarSolicitudesPendientes(ADMIN, 999L));
    }

    @Test
    @DisplayName("Un médico no puede listar las solicitudes pendientes")
    void medicoNoPuedeListarSolicitudes() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.listarSolicitudesPendientes(
                        MEDICO, paciente.getId()));
    }

    @Test
    @DisplayName("Los huecos libres excluyen los bloqueados, los de agendas inactivas y los ocupados")
    void listarHuecosLibres() throws Exception {
        // Preparar
        servicio.agregarHueco(3, DIA, LocalTime.of(10, 0),
                LocalTime.of(10, 30), MEDICO_AGENDA, true, true);
        servicio.agregarHueco(4, DIA, LocalTime.of(10, 30),
                LocalTime.of(11, 0), MEDICO_AGENDA, false, false);
        asignarCita(); // ocupa el hueco 1
        // Ejecutar
        List<HuecoDisponible> libres = servicio.listarHuecosLibres(ADMIN);
        // Comprobar
        assertAll(
                () -> assertEquals(1, libres.size()),
                () -> assertEquals(2L, libres.get(0).id()));
    }

    @Test
    @DisplayName("Los huecos libres se ordenan por día y hora")
    void huecosLibresOrdenados() throws Exception {
        // Preparar
        servicio.agregarHueco(5, DIA.minusDays(1), LocalTime.of(12, 0),
                LocalTime.of(12, 30), MEDICO_AGENDA, false, true);
        // Ejecutar
        List<HuecoDisponible> libres = servicio.listarHuecosLibres(ADMIN);
        // Comprobar
        assertAll(
                () -> assertEquals(3, libres.size()),
                () -> assertEquals(5L, libres.get(0).id()),
                () -> assertEquals(1L, libres.get(1).id()),
                () -> assertEquals(2L, libres.get(2).id()));
    }

    @Test
    @DisplayName("Un médico no puede listar los huecos libres")
    void medicoNoPuedeListarHuecos() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.listarHuecosLibres(MEDICO));
    }

    @Test
    @DisplayName("Un hueco disponible se muestra con su día, su hora y su médico")
    void huecoDisponibleSeMuestraLegible() {
        // Ejecutar
        HuecoDisponible hueco = new HuecoDisponible(
                1, DIA, LocalTime.of(9, 0), LocalTime.of(9, 30), 7);
        // Comprobar
        assertEquals("01/12/2026 09:00-09:30 (médico 7)",
                hueco.toString());
    }
}

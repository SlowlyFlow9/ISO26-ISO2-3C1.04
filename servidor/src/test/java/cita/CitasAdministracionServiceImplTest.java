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

import agenda.AgendaMedica;
import agenda.AgendaNoEncontradaException;
import agenda.AgendasRepositorioMemoria;
import agenda.HuecoAgenda;
import agenda.HuecoAgendaNoEncontradoException;
import agenda.HuecosAgendaRepositorioMemoria;
import paciente.Paciente;
import paciente.PacienteNoEncontradoException;
import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

/** CU25: gestión de las citas de un paciente por el personal administrativo. */
class CitasAdministracionServiceImplTest {

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

    private CitasRepositorioMemoria citas;
    private SolicitudesRepositorioMemoria solicitudes;
    private PacientesRepositorioMemoria pacientes;
    private AgendasRepositorioMemoria agendas;
    private HuecosAgendaRepositorioMemoria huecos;
    private CitasAdministracionServiceImpl servicio;

    private Paciente paciente;
    private SolicitudPrueba solicitud;
    private AgendaMedica agenda;
    private HuecoAgenda hueco1;
    private HuecoAgenda hueco2;

    @BeforeEach
    void setUp() {

        citas = new CitasRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();
        pacientes = new PacientesRepositorioMemoria();
        agendas = new AgendasRepositorioMemoria();
        huecos = new HuecosAgendaRepositorioMemoria();

        servicio = new CitasAdministracionServiceImpl(
                citas, solicitudes, pacientes, agendas, huecos);

        paciente = crearPaciente(true);
        solicitud = crearSolicitud(
                paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);

        agenda = crearAgenda(true);
        hueco1 = crearHueco(agenda.getId(), LocalTime.of(9, 0), false);
        hueco2 = crearHueco(agenda.getId(), LocalTime.of(9, 30), false);
    }

    // ---------- datos de prueba ----------

    private Paciente crearPaciente(boolean activo) {
        Paciente nuevo = new Paciente();
        nuevo.setNombre("Ana");
        nuevo.setApellidos("García");
        nuevo.setDni("12345678Z");
        nuevo.setActivo(activo);
        return pacientes.guardar(nuevo);
    }

    private SolicitudPrueba crearSolicitud(
            long pacienteId, EstadoSolicitud estado) {
        return solicitudes.guardar(new SolicitudPrueba(
                0, pacienteId, 5, TIPO_PRUEBA, "Dolor", "Control",
                Prioridad.ORDINARIA, LocalDateTime.of(2026, 11, 20, 10, 0),
                estado));
    }

    private AgendaMedica crearAgenda(boolean activa) {
        AgendaMedica nueva = new AgendaMedica();
        nueva.setMedicoId(MEDICO_AGENDA);
        nueva.setFecha(DIA);
        nueva.setHoraInicio(LocalTime.of(8, 0));
        nueva.setHoraFin(LocalTime.of(14, 0));
        nueva.setActivo(activa);
        return agendas.guardar(nueva);
    }

    private HuecoAgenda crearHueco(
            long agendaId, LocalTime inicio, boolean bloqueado) {
        HuecoAgenda nuevo = new HuecoAgenda();
        nuevo.setAgendaId(agendaId);
        nuevo.setHoraInicio(inicio);
        nuevo.setHoraFin(inicio.plusMinutes(30));
        nuevo.setBloqueado(bloqueado);
        return huecos.guardar(nuevo);
    }

    private Cita asignarCita() throws Exception {
        return servicio.asignar(ADMIN, solicitud.getId(), hueco1.getId());
    }

    private EstadoSolicitud estadoDeLaSolicitud() {
        return solicitudes.buscarPorId(solicitud.getId())
                .orElseThrow().getEstado();
    }

    private Cita citaEnEstado(EstadoCita estado) throws Exception {
        Cita cita = asignarCita();
        Cita modificada = citas.buscarPorId(cita.getId())
                .orElseThrow().copia();
        modificada.setEstado(estado);
        citas.guardar(modificada);
        return modificada;
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
                () -> assertEquals(hueco1.getId(), cita.getHuecoId()),
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
        assertEquals(EstadoSolicitud.CITADA, estadoDeLaSolicitud());
    }

    @Test
    @DisplayName("Un paciente no puede asignar citas")
    void pacienteNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(PACIENTE,
                        solicitud.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("Un médico no puede asignar citas")
    void medicoNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(MEDICO,
                        solicitud.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("Un administrador del sistema no puede asignar citas")
    void administradorNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(ADMINISTRADOR,
                        solicitud.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("Un usuario nulo no puede asignar citas")
    void usuarioNuloNoPuedeAsignar() {
        // Comprobar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(null,
                        solicitud.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("Asignar sin permiso no crea ninguna cita ni cambia la solicitud")
    void asignarSinPermisoNoCambiaNada() {
        // Ejecutar
        assertThrows(SinPermisoException.class,
                () -> servicio.asignar(MEDICO,
                        solicitud.getId(), hueco1.getId()));
        // Comprobar
        assertAll(
                () -> assertTrue(citas.buscarActivaPorHueco(
                        hueco1.getId()).isEmpty()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                        estadoDeLaSolicitud()));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud inexistente")
    void asignarSolicitudInexistente() {
        // Comprobar
        assertThrows(SolicitudNoEncontradaException.class,
                () -> servicio.asignar(ADMIN, 999L, hueco1.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud que ya está citada")
    void asignarSolicitudYaCitada() throws Exception {
        // Preparar
        asignarCita();
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN,
                        solicitud.getId(), hueco2.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud anulada")
    void asignarSolicitudAnulada() {
        // Preparar
        SolicitudPrueba anulada = crearSolicitud(
                paciente.getId(), EstadoSolicitud.ANULADA);
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN,
                        anulada.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita a una solicitud ya realizada")
    void asignarSolicitudRealizada() {
        // Preparar
        SolicitudPrueba realizada = crearSolicitud(
                paciente.getId(), EstadoSolicitud.REALIZADA);
        // Comprobar
        assertThrows(SolicitudNoCitableException.class,
                () -> servicio.asignar(ADMIN,
                        realizada.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita si el paciente de la solicitud no existe")
    void asignarPacienteInexistente() {
        // Preparar
        SolicitudPrueba huerfana = crearSolicitud(
                999L, EstadoSolicitud.PENDIENTE_CITACION);
        // Comprobar
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.asignar(ADMIN,
                        huerfana.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("No se puede dar cita a un paciente dado de baja")
    void asignarPacienteDeBaja() {
        // Preparar
        Paciente deBaja = crearPaciente(false);
        SolicitudPrueba deBajaSolicitud = crearSolicitud(
                deBaja.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Ejecutar
        assertThrows(PacienteInactivoException.class,
                () -> servicio.asignar(ADMIN,
                        deBajaSolicitud.getId(), hueco1.getId()));
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                solicitudes.buscarPorId(deBajaSolicitud.getId())
                        .orElseThrow().getEstado());
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco inexistente")
    void asignarHuecoInexistente() {
        // Comprobar
        assertThrows(HuecoAgendaNoEncontradoException.class,
                () -> servicio.asignar(ADMIN, solicitud.getId(), 999L));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco cuya agenda no existe")
    void asignarAgendaInexistente() {
        // Preparar
        HuecoAgenda sinAgenda = crearHueco(
                999L, LocalTime.of(11, 0), false);
        // Comprobar
        assertThrows(AgendaNoEncontradaException.class,
                () -> servicio.asignar(ADMIN,
                        solicitud.getId(), sinAgenda.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco bloqueado")
    void asignarHuecoBloqueado() {
        // Preparar
        HuecoAgenda bloqueado = crearHueco(
                agenda.getId(), LocalTime.of(10, 0), true);
        // Ejecutar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN,
                        solicitud.getId(), bloqueado.getId()));
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                estadoDeLaSolicitud());
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco de una agenda inactiva")
    void asignarAgendaInactiva() {
        // Preparar
        AgendaMedica inactiva = crearAgenda(false);
        HuecoAgenda huecoInactivo = crearHueco(
                inactiva.getId(), LocalTime.of(9, 0), false);
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN,
                        solicitud.getId(), huecoInactivo.getId()));
    }

    @Test
    @DisplayName("No se puede asignar cita en un hueco que ya está ocupado")
    void asignarHuecoOcupado() throws Exception {
        // Preparar
        asignarCita();
        SolicitudPrueba otra = crearSolicitud(
                paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.asignar(ADMIN,
                        otra.getId(), hueco1.getId()));
    }

    // ---------- reprogramar ----------

    @Test
    @DisplayName("Reprogramar mueve la cita al nuevo hueco y actualiza fecha, médico y hueco")
    void reprogramarMueveLaCita() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        Cita movida = servicio.reprogramar(
                ADMIN, cita.getId(), hueco2.getId());
        // Comprobar
        assertAll(
                () -> assertEquals(cita.getId(), movida.getId()),
                () -> assertEquals(hueco2.getId(), movida.getHuecoId()),
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
        servicio.reprogramar(ADMIN, cita.getId(), hueco2.getId());
        // Comprobar
        assertTrue(citas.buscarActivaPorHueco(hueco1.getId()).isEmpty());
    }

    @Test
    @DisplayName("Reprogramar una cita confirmada la deja programada de nuevo")
    void reprogramarCitaConfirmada() throws Exception {
        // Preparar
        Cita confirmada = citaEnEstado(EstadoCita.CONFIRMADA);
        // Ejecutar
        Cita movida = servicio.reprogramar(
                ADMIN, confirmada.getId(), hueco2.getId());
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
                () -> servicio.reprogramar(
                        ADMIN, cita.getId(), hueco1.getId()));
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco ocupado por otra cita")
    void reprogramarAHuecoOcupado() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        SolicitudPrueba otra = crearSolicitud(
                paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        servicio.asignar(ADMIN, otra.getId(), hueco2.getId());
        // Comprobar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(
                        ADMIN, cita.getId(), hueco2.getId()));
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco bloqueado")
    void reprogramarAHuecoBloqueado() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        HuecoAgenda bloqueado = crearHueco(
                agenda.getId(), LocalTime.of(10, 0), true);
        // Ejecutar
        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.reprogramar(
                        ADMIN, cita.getId(), bloqueado.getId()));
        // Comprobar
        assertEquals(hueco1.getId(),
                servicio.consultarCita(ADMIN, cita.getId()).getHuecoId());
    }

    @Test
    @DisplayName("No se puede reprogramar a un hueco inexistente")
    void reprogramarAHuecoInexistente() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Comprobar
        assertThrows(HuecoAgendaNoEncontradoException.class,
                () -> servicio.reprogramar(ADMIN, cita.getId(), 999L));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita inexistente")
    void reprogramarCitaInexistente() {
        // Comprobar
        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.reprogramar(ADMIN, 999L, hueco2.getId()));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita cancelada")
    void reprogramarCitaCancelada() throws Exception {
        // Preparar
        Cita cancelada = citaEnEstado(EstadoCita.CANCELADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.reprogramar(
                        ADMIN, cancelada.getId(), hueco2.getId()));
    }

    @Test
    @DisplayName("No se puede reprogramar una cita realizada")
    void reprogramarCitaRealizada() throws Exception {
        // Preparar
        Cita realizada = citaEnEstado(EstadoCita.REALIZADA);
        // Comprobar
        assertThrows(CitaNoModificableException.class,
                () -> servicio.reprogramar(
                        ADMIN, realizada.getId(), hueco2.getId()));
    }

    @Test
    @DisplayName("Un médico no puede reprogramar y la cita no cambia")
    void medicoNoPuedeReprogramar() throws Exception {
        // Preparar
        Cita cita = asignarCita();
        // Ejecutar
        assertThrows(SinPermisoException.class,
                () -> servicio.reprogramar(
                        MEDICO, cita.getId(), hueco2.getId()));
        // Comprobar
        assertEquals(hueco1.getId(),
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
                paciente.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        // Ejecutar
        Cita nueva = servicio.asignar(ADMIN, otra.getId(), hueco1.getId());
        // Comprobar
        assertEquals(hueco1.getId(), nueva.getHuecoId());
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
                estadoDeLaSolicitud());
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
        Cita guardada = citas.guardar(suelta);
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
        solicitudes.guardar(solicitudes.buscarPorId(solicitud.getId())
                .orElseThrow().conEstado(EstadoSolicitud.ANULADA));
        // Ejecutar
        servicio.cancelar(ADMIN, cita.getId());
        // Comprobar
        assertEquals(EstadoSolicitud.ANULADA, estadoDeLaSolicitud());
    }

    // ---------- listar y consultar ----------

    @Test
    @DisplayName("Listar devuelve solo las citas del paciente indicado")
    void listarDevuelveLasCitasDelPaciente() throws Exception {
        // Preparar
        asignarCita();
        Paciente otro = crearPaciente(true);
        SolicitudPrueba solicitudOtro = crearSolicitud(
                otro.getId(), EstadoSolicitud.PENDIENTE_CITACION);
        servicio.asignar(ADMIN, solicitudOtro.getId(), hueco2.getId());
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
}

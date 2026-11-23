package cita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import paciente.Paciente;
import paciente.PacienteNoEncontradoException;
import paciente.PacientesService;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.SolicitudPrueba;

/**
 * CU25: servicio en memoria para probar el cliente sin servidor.
 * Aplica las mismas reglas que el servidor (D-010, D-016, D-026 y D-032).
 */
public class CitasAdministracionServiceSimulado
        implements CitasAdministracionService {

    private record HuecoSimulado(
            long id,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            long medicoId,
            boolean bloqueado,
            boolean agendaActiva) {
    }

    private final PacientesService pacientes;
    private final Map<Long, SolicitudPrueba> solicitudes =
            new LinkedHashMap<>();
    private final Map<Long, HuecoSimulado> huecos =
            new LinkedHashMap<>();
    private final Map<Long, Cita> citas =
            new LinkedHashMap<>();
    private long siguienteCitaId = 1;

    public CitasAdministracionServiceSimulado(
            PacientesService pacientes) {
        this.pacientes = pacientes;
    }

    // ---------- datos de prueba ----------

    public void agregarSolicitud(SolicitudPrueba solicitud) {
        solicitudes.put(solicitud.getId(), solicitud);
    }

    public void agregarHueco(
            long id,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            long medicoId,
            boolean bloqueado,
            boolean agendaActiva) {
        huecos.put(id, new HuecoSimulado(
                id, fecha, horaInicio, horaFin,
                medicoId, bloqueado, agendaActiva));
    }

    public Cita agregarCita(Cita datos) {
        Cita almacenada = datos.copia();
        if (almacenada.getId() == null) {
            almacenada.setId(siguienteCitaId++);
        }
        citas.put(almacenada.getId(), almacenada);
        return almacenada.copia();
    }

    /** Estado actual de una solicitud, para comprobarlo en las pruebas. */
    public EstadoSolicitud estadoDeSolicitud(long solicitudId) {
        SolicitudPrueba solicitud = solicitudes.get(solicitudId);
        return solicitud == null ? null : solicitud.getEstado();
    }

    // ---------- operaciones ----------

    @Override
    public Cita asignar(
            Usuario solicitante,
            long solicitudId,
            long huecoId)
            throws SinPermisoException,
            SolicitudNoEncontradaException,
            SolicitudNoCitableException,
            PacienteNoEncontradoException,
            PacienteInactivoException,
            HuecoNoDisponibleException {

        exigirPermiso(solicitante);

        SolicitudPrueba solicitud = solicitudes.get(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException(
                    "No existe la solicitud " + solicitudId);
        }

        if (solicitud.getEstado()
                != EstadoSolicitud.PENDIENTE_CITACION) {
            throw new SolicitudNoCitableException(
                    "La solicitud " + solicitudId
                            + " no está pendiente de citación");
        }

        Paciente paciente = pacientes.consultar(
                solicitante, solicitud.getPacienteId());
        if (!paciente.isActivo()) {
            throw new PacienteInactivoException(
                    "El paciente " + paciente.getId()
                            + " está dado de baja");
        }

        HuecoSimulado hueco = buscarHuecoLibre(huecoId);

        Cita cita = new Cita();
        cita.setId(siguienteCitaId++);
        cita.setPacienteId(solicitud.getPacienteId());
        cita.setMedicoId(hueco.medicoId());
        cita.setTipoPruebaId(solicitud.getTipoPruebaId());
        cita.setFechaHora(fechaHora(hueco));
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setHuecoId(huecoId);
        cita.setSolicitudId(solicitudId);

        citas.put(cita.getId(), cita);
        solicitudes.put(solicitudId,
                conEstado(solicitud, EstadoSolicitud.CITADA));

        return cita.copia();
    }

    @Override
    public Cita reprogramar(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoNoDisponibleException {

        exigirPermiso(solicitante);

        Cita cita = buscarCitaModificable(citaId);
        HuecoSimulado hueco = buscarHuecoLibre(nuevoHuecoId);

        cita.setMedicoId(hueco.medicoId());
        cita.setFechaHora(fechaHora(hueco));
        cita.setHuecoId(nuevoHuecoId);
        // al cambiar de hueco hay que volver a confirmar la cita
        cita.setEstado(EstadoCita.PROGRAMADA);

        return cita.copia();
    }

    @Override
    public Cita cancelar(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException {

        exigirPermiso(solicitante);

        Cita cita = buscarCitaModificable(citaId);
        cita.setEstado(EstadoCita.CANCELADA);

        Long solicitudId = cita.getSolicitudId();
        if (solicitudId != null) {
            SolicitudPrueba solicitud = solicitudes.get(solicitudId);
            if (solicitud != null && solicitud.getEstado()
                    == EstadoSolicitud.CITADA) {
                solicitudes.put(solicitudId, conEstado(solicitud,
                        EstadoSolicitud.PENDIENTE_CITACION));
            }
        }

        return cita.copia();
    }

    @Override
    public List<Cita> listarCitasDePaciente(
            Usuario solicitante,
            long pacienteId)
            throws SinPermisoException,
            PacienteNoEncontradoException {

        exigirPermiso(solicitante);
        pacientes.consultar(solicitante, pacienteId);

        return citas.values().stream()
                .filter(cita -> cita.getPacienteId() == pacienteId)
                .map(Cita::copia)
                .toList();
    }

    @Override
    public Cita consultarCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException {

        exigirPermiso(solicitante);

        return buscarCita(citaId).copia();
    }

    @Override
    public List<SolicitudPrueba> listarSolicitudesPendientes(
            Usuario solicitante,
            long pacienteId)
            throws SinPermisoException,
            PacienteNoEncontradoException {

        exigirPermiso(solicitante);
        pacientes.consultar(solicitante, pacienteId);

        return solicitudes.values().stream()
                .filter(s -> s.getPacienteId() == pacienteId)
                .filter(s -> s.getEstado()
                        == EstadoSolicitud.PENDIENTE_CITACION)
                .toList();
    }

    @Override
    public List<HuecoDisponible> listarHuecosLibres(
            Usuario solicitante)
            throws SinPermisoException {

        exigirPermiso(solicitante);

        return huecos.values().stream()
                .filter(HuecoSimulado::agendaActiva)
                .filter(h -> !h.bloqueado())
                .filter(h -> !ocupado(h.id()))
                .sorted(Comparator
                        .comparing(HuecoSimulado::fecha)
                        .thenComparing(HuecoSimulado::horaInicio))
                .map(h -> new HuecoDisponible(
                        h.id(), h.fecha(), h.horaInicio(),
                        h.horaFin(), h.medicoId()))
                .toList();
    }

    // ---------- auxiliares ----------

    private void exigirPermiso(Usuario solicitante)
            throws SinPermisoException {
        if (solicitante == null
                || solicitante.rol()
                        != Rol.PERSONAL_ADMINISTRATIVO) {
            throw new SinPermisoException(
                    "Solo el personal administrativo puede "
                            + "gestionar las citas");
        }
    }

    private Cita buscarCita(long citaId)
            throws CitaNoEncontradaException {
        Cita cita = citas.get(citaId);
        if (cita == null) {
            throw new CitaNoEncontradaException(
                    "No existe la cita " + citaId);
        }
        return cita;
    }

    private Cita buscarCitaModificable(long citaId)
            throws CitaNoEncontradaException,
            CitaNoModificableException {

        Cita cita = buscarCita(citaId);

        if (cita.getEstado() != EstadoCita.PROGRAMADA
                && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new CitaNoModificableException(
                    "La cita " + citaId + " está "
                            + cita.getEstado()
                            + " y no se puede modificar");
        }
        return cita;
    }

    private HuecoSimulado buscarHuecoLibre(long huecoId)
            throws HuecoNoDisponibleException {

        HuecoSimulado hueco = huecos.get(huecoId);

        if (hueco == null) {
            throw new HuecoNoDisponibleException(
                    "No existe el hueco " + huecoId);
        }
        if (!hueco.agendaActiva()) {
            throw new HuecoNoDisponibleException(
                    "La agenda del hueco " + huecoId
                            + " no está activa");
        }
        if (hueco.bloqueado()) {
            throw new HuecoNoDisponibleException(
                    "El hueco " + huecoId + " está bloqueado");
        }
        if (ocupado(huecoId)) {
            throw new HuecoNoDisponibleException(
                    "El hueco " + huecoId + " ya tiene una cita");
        }
        return hueco;
    }

    // un hueco está ocupado si tiene una cita PROGRAMADA o CONFIRMADA
    private boolean ocupado(long huecoId) {
        return citas.values().stream()
                .filter(cita -> cita.getHuecoId() != null
                        && cita.getHuecoId() == huecoId)
                .anyMatch(cita ->
                        cita.getEstado() == EstadoCita.PROGRAMADA
                                || cita.getEstado()
                                        == EstadoCita.CONFIRMADA);
    }

    private LocalDateTime fechaHora(HuecoSimulado hueco) {
        return LocalDateTime.of(hueco.fecha(), hueco.horaInicio());
    }

    // la solicitud es inmutable: se crea una copia con el nuevo estado
    private SolicitudPrueba conEstado(
            SolicitudPrueba s, EstadoSolicitud estado) {
        return new SolicitudPrueba(
                s.getId(), s.getPacienteId(), s.getMedicoId(),
                s.getTipoPruebaId(), s.getIndicacionClinica(),
                s.getJustificacion(), s.getPrioridad(),
                s.getFechaSolicitud(), estado,
                s.getIntervalo(), s.getUnidad(),
                s.getNumeroRepeticiones(), s.getSolicitudOrigenId(),
                s.getMotivoAnulacion(), s.getFechaAnulacion());
    }
}

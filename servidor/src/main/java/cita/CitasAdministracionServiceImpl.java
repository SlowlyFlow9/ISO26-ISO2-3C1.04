package cita;

import java.time.LocalDateTime;
import java.util.List;

import agenda.AgendaMedica;
import agenda.AgendaNoEncontradaException;
import agenda.AgendasRepositorio;
import agenda.HuecoAgenda;
import agenda.HuecoAgendaNoEncontradoException;
import agenda.HuecosAgendaRepositorio;
import paciente.ControlAcceso;
import paciente.Paciente;
import paciente.PacienteNoEncontradoException;
import paciente.PacientesRepositorio;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorio;

/**
 * CU25: implementación del servicio de gestión de citas.
 * Los métodos que cambian datos son synchronized porque los
 * repositorios en memoria se comparten entre peticiones.
 */
public class CitasAdministracionServiceImpl
        implements CitasAdministracionService {

    private final CitasRepositorio citas;
    private final SolicitudesRepositorio solicitudes;
    private final PacientesRepositorio pacientes;
    private final AgendasRepositorio agendas;
    private final HuecosAgendaRepositorio huecos;

    public CitasAdministracionServiceImpl(
            CitasRepositorio citas,
            SolicitudesRepositorio solicitudes,
            PacientesRepositorio pacientes,
            AgendasRepositorio agendas,
            HuecosAgendaRepositorio huecos) {

        this.citas = citas;
        this.solicitudes = solicitudes;
        this.pacientes = pacientes;
        this.agendas = agendas;
        this.huecos = huecos;
    }

    @Override
    public synchronized Cita asignar(
            Usuario solicitante,
            long solicitudId,
            long huecoId)
            throws SinPermisoException,
            SolicitudNoEncontradaException,
            SolicitudNoCitableException,
            PacienteNoEncontradoException,
            PacienteInactivoException,
            HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException {

        exigirPermiso(solicitante);

        SolicitudPrueba solicitud =
                solicitudes.buscarPorId(solicitudId)
                        .orElseThrow(() ->
                                new SolicitudNoEncontradaException(
                                        "No existe la solicitud "
                                                + solicitudId));

        if (solicitud.getEstado()
                != EstadoSolicitud.PENDIENTE_CITACION) {
            throw new SolicitudNoCitableException(
                    "La solicitud " + solicitudId
                            + " no está pendiente de citación");
        }

        Paciente paciente =
                pacientes.buscarPorId(solicitud.getPacienteId())
                        .orElseThrow(() ->
                                new PacienteNoEncontradoException(
                                        "No existe el paciente "
                                                + solicitud.getPacienteId()));

        if (!paciente.isActivo()) {
            throw new PacienteInactivoException(
                    "El paciente " + paciente.getId()
                            + " está dado de baja");
        }

        HuecoElegido elegido = buscarHuecoLibre(huecoId);

        Cita cita = new Cita();
        cita.setPacienteId(solicitud.getPacienteId());
        cita.setMedicoId(elegido.agenda().getMedicoId());
        cita.setTipoPruebaId(solicitud.getTipoPruebaId());
        cita.setFechaHora(fechaHora(elegido));
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setHuecoId(huecoId);
        cita.setSolicitudId(solicitudId);

        Cita guardada = citas.guardar(cita);

        solicitudes.guardar(
                solicitud.conEstado(EstadoSolicitud.CITADA));

        return guardada.copia();
    }

    @Override
    public synchronized Cita reprogramar(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException {

        exigirPermiso(solicitante);

        Cita actual = buscarCitaModificable(citaId);

        HuecoElegido elegido = buscarHuecoLibre(nuevoHuecoId);

        Cita modificada = actual.copia();
        modificada.setMedicoId(elegido.agenda().getMedicoId());
        modificada.setFechaHora(fechaHora(elegido));
        modificada.setHuecoId(nuevoHuecoId);
        // al cambiar de hueco hay que volver a confirmar la cita
        modificada.setEstado(EstadoCita.PROGRAMADA);

        return citas.guardar(modificada).copia();
    }

    @Override
    public synchronized Cita cancelar(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException {

        exigirPermiso(solicitante);

        Cita actual = buscarCitaModificable(citaId);

        Cita cancelada = actual.copia();
        cancelada.setEstado(EstadoCita.CANCELADA);
        Cita guardada = citas.guardar(cancelada);

        devolverSolicitudAPendiente(guardada.getSolicitudId());

        return guardada.copia();
    }

    @Override
    public List<Cita> listarCitasDePaciente(
            Usuario solicitante,
            long pacienteId)
            throws SinPermisoException,
            PacienteNoEncontradoException {

        exigirPermiso(solicitante);

        if (pacientes.buscarPorId(pacienteId).isEmpty()) {
            throw new PacienteNoEncontradoException(
                    "No existe el paciente " + pacienteId);
        }

        return citas.listarPorPaciente(pacienteId).stream()
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

    // ---------- auxiliares ----------

    private void exigirPermiso(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);
    }

    private Cita buscarCita(long citaId)
            throws CitaNoEncontradaException {
        return citas.buscarPorId(citaId)
                .orElseThrow(() ->
                        new CitaNoEncontradaException(
                                "No existe la cita " + citaId));
    }

    // solo se pueden reprogramar o cancelar las citas PROGRAMADA o CONFIRMADA
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

    // comprueba que el hueco existe, su agenda está activa y no está bloqueado ni ocupado
    private HuecoElegido buscarHuecoLibre(long huecoId)
            throws HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException {

        HuecoAgenda hueco =
                huecos.buscarPorId(huecoId)
                        .orElseThrow(() ->
                                new HuecoAgendaNoEncontradoException(
                                        "No existe el hueco "
                                                + huecoId));

        AgendaMedica agenda =
                agendas.buscarPorId(hueco.getAgendaId())
                        .orElseThrow(() ->
                                new AgendaNoEncontradaException(
                                        "No existe la agenda "
                                                + hueco.getAgendaId()));

        if (!agenda.isActivo()) {
            throw new HuecoNoDisponibleException(
                    "La agenda del hueco " + huecoId
                            + " no está activa");
        }

        if (hueco.isBloqueado()) {
            throw new HuecoNoDisponibleException(
                    "El hueco " + huecoId + " está bloqueado");
        }

        if (citas.buscarActivaPorHueco(huecoId).isPresent()) {
            throw new HuecoNoDisponibleException(
                    "El hueco " + huecoId
                            + " ya tiene una cita");
        }

        return new HuecoElegido(agenda, hueco);
    }

    private LocalDateTime fechaHora(HuecoElegido elegido) {
        return LocalDateTime.of(
                elegido.agenda().getFecha(),
                elegido.hueco().getHoraInicio());
    }

    // al cancelar, la solicitud vuelve a poder citarse (D-026)
    private void devolverSolicitudAPendiente(Long solicitudId) {

        if (solicitudId == null) {
            return;
        }

        solicitudes.buscarPorId(solicitudId)
                .filter(s -> s.getEstado()
                        == EstadoSolicitud.CITADA)
                .ifPresent(s -> solicitudes.guardar(
                        s.conEstado(
                                EstadoSolicitud.PENDIENTE_CITACION)));
    }

    private record HuecoElegido(
            AgendaMedica agenda,
            HuecoAgenda hueco) {
    }
}

package cita;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasIncidenciasServiceImpl
        implements CitasIncidenciasService {

    private final CitasRepositorio citas;
    private final IncidenciasRepositorio incidencias;
    private final Clock reloj;

    public CitasIncidenciasServiceImpl(
            CitasRepositorio citas,
            IncidenciasRepositorio incidencias) {
        this(citas, incidencias, Clock.systemDefaultZone());
    }

    CitasIncidenciasServiceImpl(
            CitasRepositorio citas,
            IncidenciasRepositorio incidencias,
            Clock reloj) {
        this.citas = citas;
        this.incidencias = incidencias;
        this.reloj = reloj;
    }

    @Override
    public synchronized IncidenciaCita registrarIncidencia(
            Usuario solicitante,
            long citaId,
            String descripcion)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoIncidenciableException {
        exigirIdentidadMedico(solicitante);
        Cita cita = buscarCitaPropia(solicitante, citaId);

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new CitaNoIncidenciableException(
                    "No se pueden registrar incidencias en una cita cancelada");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción de la incidencia es obligatoria");
        }

        IncidenciaCita incidencia = new IncidenciaCita(
                null,
                citaId,
                cita.getMedicoId(),
                LocalDateTime.now(reloj),
                descripcion.trim());
        return incidencias.guardar(incidencia);
    }

    @Override
    public List<IncidenciaCita> listarIncidencias(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException {
        exigirIdentidadMedico(solicitante);
        buscarCitaPropia(solicitante, citaId);
        return incidencias.listarPorCita(citaId);
    }

    private Cita buscarCitaPropia(
            Usuario solicitante,
            long citaId)
            throws CitaNoEncontradaException,
            SinPermisoException {
        Cita cita = citas.buscarPorId(citaId)
                .orElseThrow(() ->
                        new CitaNoEncontradaException(
                                "No existe la cita con id " + citaId));
        if (cita.getMedicoId() != solicitante.medicoId()) {
            throw new SinPermisoException(
                    "El médico solo puede gestionar sus propias citas");
        }
        return cita;
    }

    private void exigirIdentidadMedico(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(solicitante, Rol.MEDICO);
        if (solicitante.medicoId() == null
                || solicitante.medicoId() <= 0) {
            throw new SinPermisoException(
                    "El identificador del médico es obligatorio");
        }
    }
}

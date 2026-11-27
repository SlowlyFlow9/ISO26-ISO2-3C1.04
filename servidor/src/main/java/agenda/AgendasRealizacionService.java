package agenda;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/** Operaciones de CU14 sobre las agendas del médico solicitante. */
public interface AgendasRealizacionService {

    AgendaMedica crearPropia(
            Usuario solicitante,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException;

    AgendaMedica consultarPropia(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException;

    List<AgendaMedica> listarPropias(
            Usuario solicitante)
            throws SinPermisoException;

    AgendaMedica modificarPropia(
            Usuario solicitante,
            long id,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException,
            AgendaNoEncontradaException;

    void eliminarPropia(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException;
}

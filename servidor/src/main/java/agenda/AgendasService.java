package agenda;

import java.time.LocalDate;
import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface AgendasService {

    AgendaMedica crear(
            Usuario solicitante,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException;

    AgendaMedica consultar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException;

    List<AgendaMedica> listar(
            Usuario solicitante,
            Long medicoId,
            LocalDate fecha,
            Boolean activo)
            throws SinPermisoException;

    AgendaMedica modificar(
            Usuario solicitante,
            long id,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException,
            AgendaNoEncontradaException;

    void eliminar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException;
}
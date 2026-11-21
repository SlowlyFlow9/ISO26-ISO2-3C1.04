package agenda;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU29 - Servicio de gestión de huecos de agenda.
 */
public interface HuecosAgendaService {

    //Crear un hueco disponible
    HuecoAgenda crear(
            Usuario solicitante,
            HuecoAgenda datos)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            DatosHuecoInvalidosException;

    // Consultar los huecos de una agenda
    List<HuecoAgenda> listarPorAgenda(
            Usuario solicitante,
            long agendaId)
            throws SinPermisoException,
            AgendaNoEncontradaException;

    //Bloquear un hueco existente
    HuecoAgenda bloquear(
            Usuario solicitante,
            long huecoId)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException;

    // Liberar un hueco bloqueado
    HuecoAgenda liberar(
            Usuario solicitante,
            long huecoId)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException;
}

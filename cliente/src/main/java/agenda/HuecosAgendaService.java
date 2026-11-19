package agenda;

import java.util.List;

import paciente.Usuario;

/**
 * CU29 - Servicio de gestion de huecos de agenda.
 *
 * Define las operaciones que utiliza el cliente
 * para comunicarse con el servidor.
 */

public interface HuecosAgendaService {

    // Crear un nuevo hueco en una agenda.
    HuecoAgenda crear(
            Usuario solicitante,
            HuecoAgenda datos);

    // Consultar los huecos de una agenda.
    List<HuecoAgenda> listarPorAgenda(
            Usuario solicitante,
            long agendaId);

    // Bloquear un hueco existente.
    HuecoAgenda bloquear(
            Usuario solicitante,
            long huecoId);

    //Liberar un hueco bloqueado
    HuecoAgenda liberar(
            Usuario solicitante,
            long huecoId);
}

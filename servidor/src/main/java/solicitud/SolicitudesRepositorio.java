package solicitud;

import java.util.Optional;

/**
 * Interfaz para el almacenamiento de solicitudes de pruebas.
 * CU11 - Prescribir pruebas diagnosticas.
 */
public interface SolicitudesRepositorio {

     //Guarda una solicitud y devuelve la solicitud almacenada.
    SolicitudPrueba guardar(SolicitudPrueba solicitud);

    //Busca una solicitud por su identificador.
    Optional<SolicitudPrueba> buscarPorId(long id);
}

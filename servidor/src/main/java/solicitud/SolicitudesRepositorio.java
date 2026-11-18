package solicitud;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el almacenamiento de solicitudes de pruebas.
 * CU11 - Prescribir pruebas diagnosticas. CU13 - Gestionar solicitudes.
 */
public interface SolicitudesRepositorio {

     //Guarda una solicitud y devuelve la solicitud almacenada.
    SolicitudPrueba guardar(SolicitudPrueba solicitud);

    //Busca una solicitud por su identificador.
    Optional<SolicitudPrueba> buscarPorId(long id);

    //CU13: devuelve todas las solicitudes guardadas.
    List<SolicitudPrueba> todas();
}

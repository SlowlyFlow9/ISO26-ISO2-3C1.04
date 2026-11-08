package solicitud;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

/**
 * Pruebas del repositorio de solicitudes.
 * CU11 - Prescribir pruebas diagnosticas.
 */
public class SolicitudesRepoTest {

    // Crea una solicitud para utilizar en las pruebas
    private SolicitudPrueba crearSolicitud() {
        return new SolicitudPrueba(
            0,
            1,
            2,
            3,
            "Dolor de rodilla",
            "Necesita una resonancia",
            Prioridad.PREFERENTE,
            LocalDateTime.now(),
            EstadoSolicitud.PENDIENTE_CITACION
        );
    }

    @Test
    public void guardarSolicitud() {
        SolicitudesRepositorioMemoria repo =
                new SolicitudesRepositorioMemoria();

        SolicitudPrueba guardada =
                repo.guardar(crearSolicitud());

        assertEquals(1, guardada.getId());
        assertEquals("Dolor de rodilla",
                guardada.getIndicacionClinica());
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                guardada.getEstado());
    }

    @Test
    public void asignarIdentificadores() {
        SolicitudesRepositorioMemoria repo =
                new SolicitudesRepositorioMemoria();

        SolicitudPrueba primera =
                repo.guardar(crearSolicitud());

        SolicitudPrueba segunda =
                repo.guardar(crearSolicitud());

        assertEquals(1, primera.getId());
        assertEquals(2, segunda.getId());
    }

    @Test
    public void buscarSolicitud() {
        SolicitudesRepositorioMemoria repo =
                new SolicitudesRepositorioMemoria();

        SolicitudPrueba guardada =
                repo.guardar(crearSolicitud());

        assertTrue(repo.buscarPorId(
                guardada.getId()).isPresent());
    }

    @Test
    public void buscarInexistente() {
        SolicitudesRepositorioMemoria repo =
                new SolicitudesRepositorioMemoria();

        assertTrue(repo.buscarPorId(99).isEmpty());
    }

    @Test
    public void guardarNula() {
        SolicitudesRepositorioMemoria repo =
                new SolicitudesRepositorioMemoria();

        assertThrows(IllegalArgumentException.class,
                () -> repo.guardar(null));
    }
}

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
    // PREPARAR
    SolicitudesRepositorioMemoria repo =
            new SolicitudesRepositorioMemoria();

    SolicitudPrueba guardada =
            repo.guardar(crearSolicitud());

    //EJECUTAR
    SolicitudPrueba encontrada =
            repo.buscarPorId(guardada.getId())
                .orElseThrow();

    //COMPROBAR
    assertEquals(guardada.getId(), encontrada.getId());
    assertEquals("Dolor de rodilla",
            encontrada.getIndicacionClinica());
    assertEquals(Prioridad.PREFERENTE,
            encontrada.getPrioridad());
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


    @Test
    public void rechazarIdentificadorNegativo() {

    // PREPARAR: repositorio y solicitud con ID incorrecto
    SolicitudesRepositorioMemoria repo =
            new SolicitudesRepositorioMemoria();

    SolicitudPrueba solicitud =
            crearSolicitud().conId(-1);

    // EJECUTAR: comprobar que se lanza la excepcion
    IllegalArgumentException excepcion = assertThrows(
            IllegalArgumentException.class,
            () -> repo.guardar(solicitud)
    );

    // COMPROBAR: tipo de error y ausencia de cambios
    assertEquals(
            "El identificador no puede ser negativo",
            excepcion.getMessage()
    );

    assertTrue(repo.buscarPorId(-1).isEmpty());

    // Una solicitud valida debe conservar el primer ID
    assertEquals(1, repo.guardar(crearSolicitud()).getId());
}

@Test
public void respetarIdentificadorExistente() {

    // PREPARAR
    SolicitudesRepositorioMemoria repo =
            new SolicitudesRepositorioMemoria();

    SolicitudPrueba solicitud =
            crearSolicitud().conId(10);

    //EJECUTAR
    repo.guardar(solicitud);

    SolicitudPrueba siguiente =
            repo.guardar(crearSolicitud());

    //COMPROBAR
    assertTrue(repo.buscarPorId(10).isPresent());

    assertEquals(10,
            repo.buscarPorId(10).orElseThrow().getId());

    assertEquals(11, siguiente.getId());
}

}

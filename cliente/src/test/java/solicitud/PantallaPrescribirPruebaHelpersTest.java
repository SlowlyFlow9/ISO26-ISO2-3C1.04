package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaPrescribirPruebaHelpersTest {

    @Test
    @DisplayName(
            "Una solicitud normal no muestra descripción de seguimiento")
    void descripcionSinSeguimiento()
            throws Exception {

        SolicitudPrueba solicitud =
                new SolicitudPrueba(
                        1,
                        2,
                        3,
                        4,
                        "Dolor",
                        "Control",
                        Prioridad.ORDINARIA,
                        null,
                        EstadoSolicitud.PENDIENTE_CITACION);

        assertEquals(
                "",
                invocarDescripcion(solicitud));
    }

    @Test
    @DisplayName(
            "Un seguimiento muestra su periodicidad")
    void descripcionSeguimiento()
            throws Exception {

        SolicitudPrueba solicitud =
                new SolicitudPrueba(
                        1,
                        2,
                        3,
                        4,
                        "Dolor",
                        "Control",
                        Prioridad.PREFERENTE,
                        null,
                        EstadoSolicitud.PENDIENTE_CITACION,
                        7,
                        UnidadPeriodicidad.SEMANAS,
                        3,
                        10L);

        String texto =
                invocarDescripcion(solicitud);

        assertEquals(
                "\nSeguimiento: cada 7 SEMANAS, 3 repeticiones",
                texto);
    }

    @Test
    @DisplayName(
            "Error nulo produce mensaje desconocido")
    void mensajeErrorNulo()
            throws Exception {

        assertEquals(
                "Error desconocido",
                invocarMensajeError(null));
    }

    @Test
    @DisplayName(
            "Error sin mensaje produce mensaje desconocido")
    void mensajeErrorSinMensaje()
            throws Exception {

        assertEquals(
                "Error desconocido",
                invocarMensajeError(
                        new RuntimeException()));
    }

    @Test
    @DisplayName(
            "Se utiliza el mensaje de la excepción")
    void mensajeErrorConMensaje()
            throws Exception {

        assertEquals(
                "Servidor caído",
                invocarMensajeError(
                        new RuntimeException(
                                "Servidor caído")));
    }

    @Test
    @DisplayName(
            "El administrador no puede acceder a prescripción")
    void administradorNoPuedeAcceder() {

        assertFalse(
                PantallaPrescribirPrueba
                        .disponiblePara(
                                new Usuario(
                                        "admin",
                                        Rol.ADMINISTRADOR)));
    }

    private String invocarDescripcion(
            SolicitudPrueba solicitud)
            throws Exception {

        Method metodo =
                PantallaPrescribirPrueba.class
                        .getDeclaredMethod(
                                "descripcionSeguimiento",
                                SolicitudPrueba.class);

        metodo.setAccessible(true);

        return (String)
                metodo.invoke(
                        null,
                        solicitud);
    }

    private String invocarMensajeError(
            Throwable causa)
            throws Exception {

        Method metodo =
                PantallaPrescribirPrueba.class
                        .getDeclaredMethod(
                                "mensajeError",
                                Throwable.class);

        metodo.setAccessible(true);

        return (String)
                metodo.invoke(
                        null,
                        causa);
    }
}
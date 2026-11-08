package paciente;

import java.io.IOException;
import com.sun.net.httpserver.HttpServer;

import solicitud.SolicitudesRepositorioMemoria;
import solicitud.SolicitudesService;
import solicitud.SolicitudesServiceImpl;
import solicitud.SolicitudesServidorHttp;

import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/** Arranque del servidor: Servidor [puerto], por defecto 8080. Los datos se guardan en memoria (D-024). */
public class Servidor {

    public static void main(String[] args) throws IOException {

        int puerto = args.length > 0
                ? Integer.parseInt(args[0]) : 8080;

        // 1. Crear los repositorios
        PacientesRepositorioMemoria pacientes =
                new PacientesRepositorioMemoria();

        RepositorioTiposPruebaMemoria tipos =
                new RepositorioTiposPruebaMemoria();

        SolicitudesRepositorioMemoria solicitudes =
                new SolicitudesRepositorioMemoria();

        // Datos opcionales para pruebas locales
        if (args.length > 1 && args[1].equals("--demo")) {

            tipos.guardar(new TipoPrueba(
                    0,
                    "Radiografia",
                    "Tipo de prueba de demostracion",
                    15,
                    false,
                    "",
                    true
            ));
        }

        // 2. Crear los servicios
        PacientesService servicioPacientes =
                new PacientesServiceImpl(pacientes);

        SolicitudesService servicioSolicitudes =
                new SolicitudesServiceImpl(
                        pacientes,
                        tipos,
                        solicitudes
                );

        // 3. Arrancar el servidor HTTP de pacientes
        HttpServer http =
                PacientesServidorHttp.iniciar(
                        servicioPacientes,
                        puerto
                );

        // 4. Registrar las rutas HTTP de CU11
        SolicitudesServidorHttp.registrar(
                http,
                servicioSolicitudes
        );

        System.out.println(
                "Servidor escuchando en http://localhost:" + puerto);

        System.out.println("Rutas disponibles:");
        System.out.println("/pacientes");
        System.out.println("/solicitudes");
        System.out.println("/solicitudes/tipos");
    }
}

package paciente;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import com.sun.net.httpserver.HttpServer;

import agenda.AgendaMedica;
import agenda.AgendasRepositorioMemoria;
import agenda.HuecosAgendaRepositorioMemoria;
import agenda.HuecosAgendaService;
import agenda.HuecosAgendaServiceImpl;
import agenda.HuecosAgendaServidorHttp;

import solicitud.ListaEsperaService;
import solicitud.ListaEsperaServiceImpl;
import solicitud.SolicitudesRepositorioMemoria;
import solicitud.SolicitudesService;
import solicitud.SolicitudesServiceImpl;
import solicitud.SolicitudesServidorHttp;

import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/**
 * Arranque del servidor compartido.
 * Los datos se almacenan en memoria.
 */
public class Servidor {

    public static void main(String[] args) throws IOException {

        int puerto = args.length > 0
                ? Integer.parseInt(args[0]) : 8080;

        // 1. Crear los repositorios existentes.
        PacientesRepositorioMemoria pacientes =
                new PacientesRepositorioMemoria();

        RepositorioTiposPruebaMemoria tipos =
                new RepositorioTiposPruebaMemoria();

        SolicitudesRepositorioMemoria solicitudes =
                new SolicitudesRepositorioMemoria();

        // CU29: repositorios de agendas y huecos.
        AgendasRepositorioMemoria agendas =
                new AgendasRepositorioMemoria();

        HuecosAgendaRepositorioMemoria huecos =
                new HuecosAgendaRepositorioMemoria();

        // 2. Datos opcionales para pruebas locales.
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

            // CU29: agenda de demostración.
            AgendaMedica agendaDemo = new AgendaMedica();

            agendaDemo.setMedicoId(1L);
            agendaDemo.setFecha(LocalDate.now());
            agendaDemo.setHoraInicio(LocalTime.of(8, 0));
            agendaDemo.setHoraFin(LocalTime.of(14, 0));

            agendas.guardar(agendaDemo);

            System.out.println(
                    "Agenda de demostracion creada: ID "
                    + agendaDemo.getId());
        }

        // 3. Crear los servicios.
        PacientesService servicioPacientes =
                new PacientesServiceImpl(pacientes);

        SolicitudesService servicioSolicitudes =
                new SolicitudesServiceImpl(
                        pacientes,
                        tipos,
                        solicitudes
                );

        ListaEsperaService servicioListaEspera =
                new ListaEsperaServiceImpl(solicitudes);

        // CU29: servicio de gestión de huecos.
        HuecosAgendaService servicioHuecos =
                new HuecosAgendaServiceImpl(
                        agendas,
                        huecos
                );

        // 4. Arrancar el servidor HTTP.
        HttpServer http =
                PacientesServidorHttp.iniciar(
                        servicioPacientes,
                        puerto
                );

        // Rutas HTTP de solicitudes.
        SolicitudesServidorHttp.registrar(
                http,
                servicioSolicitudes
        );

        SolicitudesServidorHttp.registrarListaEspera(
                http,
                servicioListaEspera
        );

        // CU29: registrar rutas HTTP de huecos.
        HuecosAgendaServidorHttp.registrar(
                http,
                servicioHuecos
        );

        System.out.println(
                "Servidor escuchando en http://localhost:" + puerto);

        System.out.println("Rutas disponibles:");
        System.out.println("/pacientes");
        System.out.println("/solicitudes");
        System.out.println("/solicitudes/tipos");
        System.out.println("/listas-espera");
        System.out.println("/huecos");
    }
}

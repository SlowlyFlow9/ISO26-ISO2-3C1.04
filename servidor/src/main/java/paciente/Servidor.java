package paciente;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import com.sun.net.httpserver.HttpServer;

import agenda.AgendaMedica;
import agenda.AgendasRepositorioMemoria;
import agenda.AgendasServiceImpl;
import agenda.AgendasServidorHttp;
import agenda.HorarioServicioRepositorioMemoria;
import agenda.HorarioServicioService;
import agenda.HorarioServicioServiceImpl;
import agenda.HorarioServicioServidorHttp;
import agenda.HuecosAgendaRepositorioMemoria;
import agenda.HuecosAgendaService;
import agenda.HuecosAgendaServiceImpl;
import agenda.HuecosAgendaServidorHttp;

import solicitud.ListaEsperaService;
import solicitud.ListaEsperaServiceImpl;
import cita.Cita;
import cita.CitasRepositorioMemoria;
import cita.CitasService;
import cita.CitasServiceImpl;
import cita.CitasRealizacionService;
import cita.CitasRealizacionServiceImpl;
import cita.CitasIncidenciasService;
import cita.CitasIncidenciasServiceImpl;
import cita.CitasServidorHttp;
import cita.EstadoCita;
import cita.IdentidadPacienteMemoria;
import cita.IncidenciasRepositorioMemoria;
import cita.PoliticaCitaRepositorioMemoria;
import cita.PoliticaCitaService;
import cita.PoliticaCitaServiceImpl;
import cita.PoliticasCitaServidorHttp;
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

        // CU14 y CU29: repositorios de agendas y huecos.
        // CU01 y CU02: repositorio compartido de citas.
        CitasRepositorioMemoria citas =
                new CitasRepositorioMemoria();
        IncidenciasRepositorioMemoria incidencias =
                new IncidenciasRepositorioMemoria();

        IdentidadPacienteMemoria identidadPaciente =
                new IdentidadPacienteMemoria();

        // CU36: repositorio de politicas de citacion.
        PoliticaCitaRepositorioMemoria politicaCita =
                new PoliticaCitaRepositorioMemoria();

        // CU29: repositorios de agendas y huecos.
        AgendasRepositorioMemoria agendas =
                new AgendasRepositorioMemoria();

        HuecosAgendaRepositorioMemoria huecos =
                new HuecosAgendaRepositorioMemoria();

        // CU35: configuracion del horario del servicio.
        HorarioServicioRepositorioMemoria horario =
                new HorarioServicioRepositorioMemoria();

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

            // Agenda compartida de demostración para CU14 y CU29.
            AgendaMedica agendaDemo = new AgendaMedica();

            agendaDemo.setMedicoId(1L);
            agendaDemo.setFecha(LocalDate.now());
            agendaDemo.setHoraInicio(LocalTime.of(8, 0));
            agendaDemo.setHoraFin(LocalTime.of(14, 0));

            agendas.guardar(agendaDemo);

            System.out.println(
                    "Agenda de demostracion creada: ID "
                    + agendaDemo.getId());


            // CU02: paciente de demostracion.
            Paciente pacienteDemo = new Paciente();

            pacienteDemo.setNombre("Paciente");
            pacienteDemo.setApellidos("Demo");
            pacienteDemo.setDni("12345678Z");
            pacienteDemo.setFechaNacimiento(
                    LocalDate.of(2000, 1, 1));
            pacienteDemo.setTelefono("600123456");
            pacienteDemo.setActivo(true);
            pacienteDemo.setFechaAlta(LocalDate.now());

            pacientes.guardar(pacienteDemo);

            // Vincular el usuario con su paciente.
            identidadPaciente.vincular(
                    "paciente-demo",
                    pacienteDemo.getId());

            // Crear una cita pendiente de confirmacion.
            Cita citaDemo = new Cita();

            citaDemo.setPacienteId(
                    pacienteDemo.getId());
            citaDemo.setMedicoId(1L);
            citaDemo.setTipoPruebaId(1L);
            citaDemo.setFechaHora(
                    LocalDate.now()
                            .plusDays(7)
                            .atTime(10, 0));
            citaDemo.setEstado(
                    EstadoCita.PROGRAMADA);

            citas.guardar(citaDemo);

            System.out.println(
                    "CU02 - Usuario: paciente-demo");
            System.out.println(
                    "CU02 - Cita de demostracion: ID "
                    + citaDemo.getId());

        }

        // 3. Crear los servicios
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
        // CU01 y CU02: consulta y confirmación de citas.
        CitasService servicioCitas =
                new CitasServiceImpl(
                        citas,
                        identidadPaciente
                );

        CitasRealizacionService servicioCitasMedico =
                new CitasRealizacionServiceImpl(
                        citas,
                        solicitudes
                );
        CitasIncidenciasService servicioIncidencias =
                new CitasIncidenciasServiceImpl(
                        citas,
                        incidencias
                );

        // CU29: servicio de gestión de huecos.
        HuecosAgendaService servicioHuecos =
                new HuecosAgendaServiceImpl(
                        agendas,
                        huecos
                );

        AgendasServiceImpl servicioAgendas =
                new AgendasServiceImpl(agendas);

        PoliticaCitaService servicioPoliticaCita =
                new PoliticaCitaServiceImpl(politicaCita);

        HorarioServicioService servicioHorario =
                new HorarioServicioServiceImpl(horario);


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

        AgendasServidorHttp.registrar(
                http,
                servicioAgendas
        );
        // CU35: registrar la configuracion horaria.
        HorarioServicioServidorHttp.registrar(
                http,
                servicioHorario
        );

        // CU01 y CU02: rutas HTTP de citas del paciente.
        CitasServidorHttp.registrar(
                http,
                servicioCitas,
                servicioCitasMedico,
                servicioIncidencias
        );

        // CU36: registrar la configuracion de politicas de citacion.
        PoliticasCitaServidorHttp.registrar(
                http,
                servicioPoliticaCita
        );

        System.out.println(
                "Servidor escuchando en http://localhost:" + puerto);

        System.out.println("Rutas disponibles:");
        System.out.println("/pacientes");
        System.out.println("/solicitudes");
        System.out.println("/solicitudes/tipos");
        System.out.println("/listas-espera");
        System.out.println("/huecos");
        System.out.println("/agendas-realizacion");
        System.out.println("/horarios-servicio");
        System.out.println("/citas");
        System.out.println("/citas/{id}");
        System.out.println("/citas/{id}/confirmar");
        System.out.println("/citas/{id}/realizar");
        System.out.println("/citas/{id}/incidencias");
        System.out.println("/politicas-citacion");
    }
}

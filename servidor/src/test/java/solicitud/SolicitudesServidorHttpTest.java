package solicitud;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.Paciente;
import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/** Pruebas HTTP del CU11. */
public class SolicitudesServidorHttpTest {

    private HttpServer servidor;

    private final HttpClient http =
            HttpClient.newHttpClient();

    private PacientesRepositorioMemoria pacientes;
    private RepositorioTiposPruebaMemoria tipos;

    //Prepara el servidor antes de cada prueba
    @BeforeEach
    public void preparar() throws Exception {

        pacientes = new PacientesRepositorioMemoria();

        tipos = new RepositorioTiposPruebaMemoria();

        SolicitudesRepositorioMemoria solicitudes =
                new SolicitudesRepositorioMemoria();

        //Arrancar el servidor de pacientes
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(pacientes), 0);

        //Registrar las rutas CU11 en el mismo servidor
        SolicitudesServidorHttp.registrar(
                servidor,
                new SolicitudesServiceImpl(
                        pacientes, tipos, solicitudes));

        // Crear un paciente para las pruebas
        Paciente paciente = new Paciente();

        paciente.setNombre("Paciente");
        paciente.setApellidos("De prueba");
        paciente.setDni("11111111H");
        paciente.setFechaNacimiento(
                LocalDate.of(1995, 1, 1));
        paciente.setTelefono("600123456");
        paciente.setActivo(true);

        pacientes.guardar(paciente);

        // Crear un tipo de prueba activo
        tipos.guardar(new TipoPrueba(
                0, "Radiografia", "Prueba diagnostica",
                15, false, "", true
        ));
    }

    //Detener el servidor despues de cada prueba
    @AfterEach
    public void parar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    //Construye la direccion del servidor
    private String url(String ruta) {
        return "http://localhost:"
                + servidor.getAddress().getPort()
                + ruta;
    }

    // Envia peticiones HTTP
    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String json,
            String rol) throws Exception {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder(
                        URI.create(url(ruta)))
                .header("Content-Type",
                        "application/json");

        if (rol != null) {
            builder.header("X-Usuario", "Medico de prueba");
            builder.header("X-Rol", rol);
        }

        if (metodo.equals("POST")) {
            builder.POST(
                    HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.GET();
        }

        return http.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // Datos de una solicitud de ejemplo
    private String datos(long pacienteId, long tipoId) {

        return "{\"pacienteId\":" + pacienteId
                + ",\"medicoId\":2"
                + ",\"tipoPruebaId\":" + tipoId
                + ",\"indicacionClinica\":\"Dolor de rodilla\""
                + ",\"justificacion\":\"Radiografia necesaria\""
                + ",\"prioridad\":\"PREFERENTE\"}";
    }

    @Test
    public void registrarYConsultarSolicitud()
            throws Exception {

        HttpResponse<String> creada =
                pedir("/solicitudes", "POST",
                        datos(1, 1), "MEDICO");

        assertEquals(201, creada.statusCode());

        assertTrue(creada.body().contains("\"id\":1"));

        assertTrue(creada.body().contains(
                "\"estado\":\"PENDIENTE_CITACION\""));

        assertTrue(creada.body().contains(
                "\"fechaSolicitud\":\""));

        HttpResponse<String> consulta =
                pedir("/solicitudes/1", "GET",
                        "", "MEDICO");

        assertEquals(200, consulta.statusCode());

        assertTrue(consulta.body().contains("\"id\":1"));
    }

    @Test
    public void rechazarUsuarioSinPermiso()
            throws Exception {

        assertEquals(403,
                pedir("/solicitudes", "POST",
                        datos(1, 1), null).statusCode());

        assertEquals(403,
                pedir("/solicitudes", "POST",
                        datos(1, 1),
                        "PERSONAL_ADMINISTRATIVO")
                        .statusCode());
    }

    @Test
    public void rechazarDatosInvalidos()
            throws Exception {

        assertEquals(400,
                pedir("/solicitudes", "POST",
                        "{}", "MEDICO").statusCode());

        assertEquals(400,
                pedir("/solicitudes", "POST",
                        "esto no es json", "MEDICO")
                        .statusCode());
    }

    @Test
    public void rechazarPacienteOTipoInexistente()
            throws Exception {

        assertEquals(404,
                pedir("/solicitudes", "POST",
                        datos(999, 1), "MEDICO")
                        .statusCode());

        assertEquals(404,
                pedir("/solicitudes", "POST",
                        datos(1, 999), "MEDICO")
                        .statusCode());

        assertEquals(404,
                pedir("/solicitudes/999", "GET",
                        "", "MEDICO").statusCode());
    }

    @Test
    public void consultarTiposDisponibles()
            throws Exception {

        tipos.guardar(new TipoPrueba(
                0, "Inactiva", "No disponible",
                10, false, "", false
        ));

        HttpResponse<String> respuesta =
                pedir("/solicitudes/tipos", "GET",
                        "", "MEDICO");

        assertEquals(200, respuesta.statusCode());

        assertTrue(respuesta.body().contains("Radiografia"));

        assertFalse(respuesta.body().contains("Inactiva"));
    }

    @Test
    public void compartirPacientesEntreServicios()
            throws Exception {

        // Dar de alta un segundo paciente por HTTP
        String nuevoPaciente =
                "{\"nombre\":\"Ana\","
                + "\"apellidos\":\"Perez\","
                + "\"dni\":\"12345678Z\","
                + "\"fechaNacimiento\":\"1990-05-01\","
                + "\"telefono\":\"600111222\"}";

        HttpResponse<String> alta =
                pedir("/pacientes", "POST",
                        nuevoPaciente,
                        "PERSONAL_ADMINISTRATIVO");

        assertEquals(201, alta.statusCode());

        assertTrue(alta.body().contains("\"id\":2"));

        //Prescribir una prueba al nuevo paciente
        HttpResponse<String> solicitud =
                pedir("/solicitudes", "POST",
                        datos(2, 1), "MEDICO");

        assertEquals(201, solicitud.statusCode());
    }
}

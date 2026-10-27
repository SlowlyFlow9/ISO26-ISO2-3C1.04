package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba el cliente HTTP contra un servidor de pruebas que responde como el protocolo de D-022. */
class PacientesServiceHttpTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private HttpServer servidor;
    private int estado;
    private String respuesta;
    private String metodo, ruta, cabeceraUsuario, cabeceraRol, cuerpo;

    @BeforeEach
    void arrancar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            metodo = exchange.getRequestMethod();
            ruta = exchange.getRequestURI().getPath();
            cabeceraUsuario = exchange.getRequestHeaders().getFirst("X-Usuario");
            cabeceraRol = exchange.getRequestHeaders().getFirst("X-Rol");
            cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();
    }

    @AfterEach
    void parar() { servidor.stop(0); }

    private PacientesServiceHttp servicio() { return new PacientesServiceHttp("http://localhost:" + servidor.getAddress().getPort()); }

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        return p;
    }

    @Test
    void altaCorrectaEnviaLaPeticionYDevuelveElPaciente() throws Exception {
        estado = 201;
        respuesta = "{\"id\":7,\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"dni\":\"12345678Z\","
                + "\"fechaNacimiento\":\"1990-05-01\",\"telefono\":\"600111222\",\"fechaAlta\":\"2026-10-27\",\"activo\":true}";
        Paciente r = servicio().crear(ADMIN, paciente());
        assertEquals("POST", metodo);
        assertEquals("/pacientes", ruta);
        assertEquals("ana", cabeceraUsuario);
        assertEquals("PERSONAL_ADMINISTRATIVO", cabeceraRol);
        assertTrue(cuerpo.contains("\"fechaNacimiento\":\"1990-05-01\""));
        assertEquals(7L, r.getId());
        assertEquals(LocalDate.of(1990, 5, 1), r.getFechaNacimiento());
        assertEquals(LocalDate.of(2026, 10, 27), r.getFechaAlta());
        assertTrue(r.isActivo());
    }

    @Test
    void respuesta400LanzaDatosInvalidosConLosErrores() {
        estado = 400;
        respuesta = "{\"error\":\"DATOS_INVALIDOS\",\"errores\":[\"El nombre es obligatorio\",\"El email no es válido\"]}";
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, paciente()));
        assertEquals(2, e.getErrores().size());
        assertEquals("El nombre es obligatorio", e.getErrores().get(0));
    }

    @Test
    void respuesta409LanzaDuplicado() {
        estado = 409;
        respuesta = "{\"error\":\"DUPLICADO\",\"mensaje\":\"Ya existe un paciente con el DNI 12345678Z\"}";
        PacienteDuplicadoException e = assertThrows(PacienteDuplicadoException.class, () -> servicio().crear(ADMIN, paciente()));
        assertEquals("Ya existe un paciente con el DNI 12345678Z", e.getMessage());
    }

    @Test
    void respuesta403LanzaSinPermiso() {
        estado = 403;
        respuesta = "{\"error\":\"SIN_PERMISO\",\"mensaje\":\"Solo el Personal administrativo puede dar de alta pacientes\"}";
        assertThrows(SinPermisoException.class, () -> servicio().crear(new Usuario("x", Rol.MEDICO), paciente()));
        assertEquals("MEDICO", cabeceraRol);
    }

    @Test
    void respuestaInesperadaLanzaServicioNoDisponible() {
        estado = 500;
        respuesta = "{}";
        assertThrows(ServicioNoDisponibleException.class, () -> servicio().crear(ADMIN, paciente()));
    }

    @Test
    void cuerpoNoValidoLanzaServicioNoDisponible() {
        estado = 201;
        respuesta = "no es json";
        assertThrows(ServicioNoDisponibleException.class, () -> servicio().crear(ADMIN, paciente()));
    }

    @Test
    void sinServidorLanzaServicioNoDisponible() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        assertThrows(ServicioNoDisponibleException.class,
                () -> new PacientesServiceHttp("http://localhost:" + puerto).crear(ADMIN, paciente()));
    }
}

package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
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
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Una respuesta 201 devuelve el paciente creado")
    void altaCorrectaDevuelveElPaciente() throws Exception {
        // Preparar
        estado = 201;
        respuesta = "{\"id\":7,\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"dni\":\"12345678Z\","
                + "\"fechaNacimiento\":\"1990-05-01\",\"telefono\":\"600111222\",\"fechaAlta\":\"2026-10-27\",\"activo\":true}";
        // Ejecutar
        Paciente r = servicio().crear(ADMIN, paciente());
        // Comprobar
        assertAll(
                () -> assertEquals(7L, r.getId()),
                () -> assertEquals(LocalDate.of(1990, 5, 1), r.getFechaNacimiento()),
                () -> assertEquals(LocalDate.of(2026, 10, 27), r.getFechaAlta()),
                () -> assertTrue(r.isActivo()));
    }

    @Test
    @DisplayName("El alta envía un POST a /pacientes con usuario, rol y cuerpo JSON")
    void altaEnviaLaPeticionEsperada() throws Exception {
        // Preparar
        estado = 201;
        respuesta = "{\"id\":7,\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"dni\":\"12345678Z\","
                + "\"fechaNacimiento\":\"1990-05-01\",\"telefono\":\"600111222\",\"fechaAlta\":\"2026-10-27\",\"activo\":true}";
        // Ejecutar
        servicio().crear(ADMIN, paciente());
        // Comprobar
        assertAll(
                () -> assertEquals("POST", metodo),
                () -> assertEquals("/pacientes", ruta),
                () -> assertEquals("ana", cabeceraUsuario),
                () -> assertEquals("PERSONAL_ADMINISTRATIVO", cabeceraRol),
                () -> assertTrue(cuerpo.contains("\"fechaNacimiento\":\"1990-05-01\"")));
    }

    @Test
    @DisplayName("Una respuesta 400 lanza datos inválidos con la lista de errores")
    void respuesta400LanzaDatosInvalidosConLosErrores() {
        // Preparar
        estado = 400;
        respuesta = "{\"error\":\"DATOS_INVALIDOS\",\"errores\":[\"El nombre es obligatorio\",\"El email no es válido\"]}";
        // Ejecutar
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, paciente()));
        // Comprobar
        assertAll(
                () -> assertEquals(2, e.getErrores().size()),
                () -> assertEquals("El nombre es obligatorio", e.getErrores().get(0)));
    }

    @Test
    @DisplayName("Una respuesta 409 lanza duplicado con el mensaje del servidor")
    void respuesta409LanzaDuplicado() {
        // Preparar
        estado = 409;
        respuesta = "{\"error\":\"DUPLICADO\",\"mensaje\":\"Ya existe un paciente con el DNI 12345678Z\"}";
        // Ejecutar
        PacienteDuplicadoException e = assertThrows(PacienteDuplicadoException.class, () -> servicio().crear(ADMIN, paciente()));
        // Comprobar
        assertEquals("Ya existe un paciente con el DNI 12345678Z", e.getMessage());
    }

    @Test
    @DisplayName("Una respuesta 403 lanza sin permiso y se envía el rol del usuario")
    void respuesta403LanzaSinPermiso() {
        // Preparar
        estado = 403;
        respuesta = "{\"error\":\"SIN_PERMISO\",\"mensaje\":\"Solo el Personal administrativo puede dar de alta pacientes\"}";
        Usuario medico = new Usuario("x", Rol.MEDICO);
        // Ejecutar
        assertThrows(SinPermisoException.class, () -> servicio().crear(medico, paciente()));
        // Comprobar
        assertEquals("MEDICO", cabeceraRol);
    }

    @Test
    @DisplayName("La baja envía un DELETE a /pacientes/{id} con usuario y rol")
    void bajaEnviaDeleteConElIdYElUsuario() throws Exception {
        // Preparar
        estado = 204;
        respuesta = "";
        // Ejecutar
        servicio().desactivar(ADMIN, 7L);
        // Comprobar
        assertAll(
                () -> assertEquals("DELETE", metodo),
                () -> assertEquals("/pacientes/7", ruta),
                () -> assertEquals("ana", cabeceraUsuario),
                () -> assertEquals("PERSONAL_ADMINISTRATIVO", cabeceraRol));
    }

    @Test
    @DisplayName("Una respuesta 403 a la baja lanza sin permiso")
    void bajaConRespuesta403LanzaSinPermiso() {
        // Preparar
        estado = 403;
        respuesta = "{\"error\":\"SIN_PERMISO\",\"mensaje\":\"Solo Personal administrativo\"}";
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio().desactivar(ADMIN, 7L));
    }

    @Test
    @DisplayName("Una respuesta 404 a la baja lanza paciente no encontrado")
    void bajaConRespuesta404LanzaNoEncontrado() {
        // Preparar
        estado = 404;
        respuesta = "{\"error\":\"PACIENTE_NO_ENCONTRADO\",\"mensaje\":\"No existe el paciente con id 7\"}";
        // Ejecutar y comprobar
        assertThrows(PacienteNoEncontradoException.class, () -> servicio().desactivar(ADMIN, 7L));
    }

    @Test
    @DisplayName("Una respuesta 409 a la baja lanza paciente ya inactivo")
    void bajaConRespuesta409LanzaYaInactivo() {
        // Preparar
        estado = 409;
        respuesta = "{\"error\":\"PACIENTE_YA_INACTIVO\",\"mensaje\":\"El paciente 7 está dado de baja\"}";
        // Ejecutar y comprobar
        assertThrows(PacienteYaInactivoException.class, () -> servicio().desactivar(ADMIN, 7L));
    }

    @Test
    @DisplayName("Una respuesta 500 lanza servicio no disponible")
    void respuestaInesperadaLanzaServicioNoDisponible() {
        // Preparar
        estado = 500;
        respuesta = "{}";
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> servicio().crear(ADMIN, paciente()));
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON lanza servicio no disponible")
    void cuerpoNoValidoLanzaServicioNoDisponible() {
        // Preparar
        estado = 201;
        respuesta = "no es json";
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> servicio().crear(ADMIN, paciente()));
    }

    @Test
    @DisplayName("Sin servidor lanza servicio no disponible")
    void sinServidorLanzaServicioNoDisponible() {
        // Preparar
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        PacientesServiceHttp sinServidor = new PacientesServiceHttp("http://localhost:" + puerto);
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> sinServidor.crear(ADMIN, paciente()));
    }
}

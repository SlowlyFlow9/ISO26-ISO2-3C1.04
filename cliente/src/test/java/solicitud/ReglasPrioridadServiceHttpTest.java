package solicitud;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

class ReglasPrioridadServiceHttpTest {

    private static final String REGLA_JSON =
            "{\"id\":1,\"nombre\":\"Urgente\","
                    + "\"descripcion\":\"Prioridad alta\","
                    + "\"orden\":1,\"activo\":true,"
                    + "\"modificadoPor\":\"admin\","
                    + "\"modificadoEn\":\"2026-11-28T13:00:00\"}";
    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final Usuario administrador =
            new Usuario("admin", Rol.ADMINISTRADOR);

    @BeforeEach
    void preparar() throws IOException {
        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String urlBase() {
        return "http://127.0.0.1:"
                + servidor.getAddress().getPort();
    }

    private String pedir(
            String metodo,
            String ruta,
            Usuario usuario,
            String cuerpo) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create(urlBase() + ruta))
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name());
        if (metodo.equals("GET")) {
            request.GET();
        } else if (metodo.equals("DELETE")) {
            request.DELETE();
        } else {
            request.method(metodo,
                    HttpRequest.BodyPublishers.ofString(cuerpo));
        }
        HttpResponse<String> respuesta = cliente.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString());
        return respuesta.statusCode() + ":" + respuesta.body();
    }

    private static void responder(
            HttpExchange exchange,
            int estado,
            String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static void responderRegla(
            HttpExchange exchange,
            int estado) throws IOException {
        responder(exchange, estado, REGLA_JSON);
    }

    @Test
    void ejecutaCrudHttpConCabecerasYJson() throws Exception {
        List<String> metodos = new CopyOnWriteArrayList<>();
        List<String> rutas = new CopyOnWriteArrayList<>();
        AtomicReference<String> usuario = new AtomicReference<>();
        AtomicReference<String> cuerpo = new AtomicReference<>();
        servidor.createContext("/reglas-prioridad", exchange -> {
            metodos.add(exchange.getRequestMethod());
            rutas.add(exchange.getRequestURI().getPath());
            usuario.set(exchange.getRequestHeaders()
                    .getFirst("X-Usuario"));
            cuerpo.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));

            if (exchange.getRequestMethod().equals("DELETE")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            } else if (exchange.getRequestMethod().equals("GET")
                    && exchange.getRequestURI().getPath()
                            .equals("/reglas-prioridad")) {
                responder(exchange, 200, "[" + REGLA_JSON + "]");
            } else if (exchange.getRequestMethod().equals("POST")) {
                responderRegla(exchange, 201);
            } else if (exchange.getRequestMethod().equals("PUT")) {
                responderRegla(exchange, 200);
            } else {
                responderRegla(exchange, 200);
            }
        });
        servidor.start();

        ReglasPrioridadServiceHttp servicio =
                new ReglasPrioridadServiceHttp(urlBase() + "/");
        DatosReglaPrioridad datos =
                new DatosReglaPrioridad("Urgente", "Alta", 1);

        ReglaPrioridad creada = servicio.crear(administrador, datos);
        List<ReglaPrioridad> listado = servicio.listar(administrador);
        ReglaPrioridad consultada =
                servicio.consultar(administrador, 1);
        ReglaPrioridad modificada = servicio.modificar(
                administrador, 1, datos);
        servicio.desactivar(administrador, 1);

        assertEquals(
                List.of("POST", "GET", "GET", "PUT", "DELETE"),
                metodos);
        assertEquals(
                List.of(
                        "/reglas-prioridad",
                        "/reglas-prioridad",
                        "/reglas-prioridad/1",
                        "/reglas-prioridad/1",
                        "/reglas-prioridad/1"),
                rutas);
        assertEquals("admin", usuario.get());
        assertEquals("", cuerpo.get());
        assertEquals(1, creada.getId());
        assertEquals("Urgente", creada.getNombre());
        assertEquals("Prioridad alta", creada.getDescripcion());
        assertEquals(1, creada.getOrden());
        assertTrue(creada.isActivo());
        assertEquals(LocalDateTime.of(2026, 11, 28, 13, 0),
                creada.getModificadoEn());
        assertEquals(1, listado.size());
        assertEquals(1, consultada.getId());
        assertEquals(1, modificada.getId());
    }

    @Test
    void rechazaEntradasInvalidasYUsuariosSinPermiso() {
        servidor.createContext("/reglas-prioridad", exchange -> {
            try {
                responder(exchange, 200, "[]");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        servidor.start();
        ReglasPrioridadServiceHttp servicio =
                new ReglasPrioridadServiceHttp(urlBase());

        assertThrows(IllegalArgumentException.class,
                () -> new ReglasPrioridadServiceHttp(" "));
        assertThrows(SinPermisoException.class,
                () -> servicio.listar(new Usuario("medico", Rol.MEDICO)));
        assertThrows(SinPermisoException.class,
                () -> servicio.listar(null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultar(administrador, 0));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(administrador, null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar(administrador, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.desactivar(administrador, -1));
    }

    @Test
    void traduceErroresHttpYRespuestasInvalidas() throws Exception {
        AtomicInteger estado = new AtomicInteger(400);
        AtomicReference<String> cuerpo =
                new AtomicReference<>(
                        "{\"error\":\"DATOS_INVALIDOS\","
                                + "\"mensaje\":\"Dato incorrecto\"}");
        servidor.createContext("/reglas-prioridad", exchange -> {
            try {
                responder(exchange, estado.get(), cuerpo.get());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        servidor.start();
        ReglasPrioridadServiceHttp servicio =
                new ReglasPrioridadServiceHttp(urlBase());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.listar(administrador));
        estado.set(409);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listar(administrador));
        estado.set(403);
        assertThrows(SinPermisoException.class,
                () -> servicio.listar(administrador));
        estado.set(404);
        assertThrows(java.util.NoSuchElementException.class,
                () -> servicio.listar(administrador));
        estado.set(500);
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listar(administrador));
        estado.set(200);
        cuerpo.set("no es json");
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listar(administrador));
        cuerpo.set("[{}]");
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listar(administrador));
    }

    @Test
    void erroresHttpSinJsonSeInformanConCodigo() throws Exception {
        servidor.createContext("/reglas-prioridad", exchange -> {
            try {
                responder(exchange, 503, "no disponible");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        servidor.start();
        ReglasPrioridadServiceHttp servicio =
                new ReglasPrioridadServiceHttp(urlBase());

        ServicioNoDisponibleException error = assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listar(administrador));

        assertTrue(error.getMessage().contains("503"));
    }
}

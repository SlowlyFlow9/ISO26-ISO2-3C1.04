package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ListaEsperaServiceTest {

    private static final Usuario ADMINISTRATIVO =
            new Usuario("administrativo", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 10, 9, 0);

    private SolicitudesRepositorioMemoria solicitudes;
    private ListaEsperaService servicio;

    @BeforeEach
    void setUp() {
        solicitudes = new SolicitudesRepositorioMemoria();
        servicio = new ListaEsperaServiceImpl(solicitudes);

        solicitudes.guardar(solicitud(
                1, 10, 1, Prioridad.ORDINARIA, FECHA,
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(
                2, 20, 2, Prioridad.URGENTE, FECHA.plusDays(1),
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(
                3, 30, 3, Prioridad.URGENTE, FECHA,
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(
                7, 70, 7, Prioridad.URGENTE, FECHA,
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(
                4, 40, 4, Prioridad.PREFERENTE, FECHA,
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(
                5, 50, 5, Prioridad.URGENTE, FECHA,
                EstadoSolicitud.CITADA));
        solicitudes.guardar(solicitud(
                6, 60, 6, Prioridad.URGENTE, FECHA,
                EstadoSolicitud.ANULADA));
    }

    private static SolicitudPrueba solicitud(
            long id,
            long pacienteId,
            long tipoPruebaId,
            Prioridad prioridad,
            LocalDateTime fecha,
            EstadoSolicitud estado) {

        return new SolicitudPrueba(
                id,
                pacienteId,
                7,
                tipoPruebaId,
                "Dato clinico privado",
                "Justificacion privada",
                prioridad,
                fecha,
                estado);
    }

    @Test
    @DisplayName("La lista incluye solo solicitudes pendientes y las ordena por prioridad, fecha e ID")
    void consultaSolicitudesPendientesOrdenadas() throws SinPermisoException {
        // Ejecutar
        List<EntradaListaEspera> resultado =
                servicio.consultar(ADMINISTRATIVO, null);

        // Comprobar
        assertEquals(
                List.of(3L, 7L, 2L, 4L, 1L),
                resultado.stream()
                        .map(EntradaListaEspera::solicitudId)
                        .toList());
        assertEquals(30, resultado.get(0).pacienteId());
        assertEquals(3, resultado.get(0).tipoPruebaId());
        assertEquals(Prioridad.URGENTE, resultado.get(0).prioridad());
        assertEquals(FECHA, resultado.get(0).fechaSolicitud());
    }

    @Test
    @DisplayName("El filtro de prioridad solo devuelve solicitudes pendientes de esa prioridad")
    void filtraPorPrioridad() throws SinPermisoException {
        // Ejecutar y comprobar
        assertEquals(
                List.of(3L, 7L, 2L),
                servicio.consultar(ADMINISTRATIVO, Prioridad.URGENTE)
                        .stream()
                        .map(EntradaListaEspera::solicitudId)
                        .toList());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "PERSONAL_ADMINISTRATIVO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Solo el personal administrativo puede consultar la lista")
    void exigePersonalAdministrativo(Rol rol) {
        // Ejecutar y comprobar
        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(new Usuario("otro", rol), null));
    }

    @Test
    @DisplayName("No se puede consultar la lista sin usuario")
    void exigeUsuario() {
        // Ejecutar y comprobar
        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(null, null));
    }
}

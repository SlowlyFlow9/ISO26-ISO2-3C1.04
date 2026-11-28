package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ReglasPrioridadServiceImplTest {

    private ReglasPrioridadRepositorioMemoria repositorio;
    private ReglasPrioridadServiceImpl servicio;
    private Usuario administrador;

    @BeforeEach
    void preparar() {
        repositorio = new ReglasPrioridadRepositorioMemoria();
        servicio = new ReglasPrioridadServiceImpl(repositorio);
        administrador = new Usuario("admin", Rol.ADMINISTRADOR);
    }

    private DatosReglaPrioridad datos(
            String nombre,
            String descripcion,
            int orden) {
        return new DatosReglaPrioridad(nombre, descripcion, orden);
    }

    @Test
    void crearRecortaDatosYRegistraAuditoria() throws Exception {
        ReglaPrioridad creada = servicio.crear(
                administrador, datos(" Urgente ", " prioridad alta ", 1));

        assertEquals(1, creada.getId());
        assertEquals("Urgente", creada.getNombre());
        assertEquals("prioridad alta", creada.getDescripcion());
        assertEquals(1, creada.getOrden());
        assertTrue(creada.isActivo());
        assertEquals("admin", creada.getModificadoPor());
        assertNotNull(creada.getModificadoEn());
    }

    @Test
    void crearRechazaDatosInvalidos() {
        assertThrows(DatosReglaPrioridadInvalidosException.class,
                () -> servicio.crear(administrador, null));
        assertThrows(DatosReglaPrioridadInvalidosException.class,
                () -> servicio.crear(
                        administrador, datos("  ", "", 1)));
        assertThrows(DatosReglaPrioridadInvalidosException.class,
                () -> servicio.crear(
                        administrador, datos("Urgente", "", 0)));
    }

    @Test
    void crearRechazaNombreYOrdenDuplicados() throws Exception {
        servicio.crear(administrador, datos("Urgente", "", 1));

        assertThrows(ReglaPrioridadDuplicadaException.class,
                () -> servicio.crear(
                        administrador, datos(" urgente ", "", 2)));
        assertThrows(ReglaPrioridadDuplicadaException.class,
                () -> servicio.crear(
                        administrador, datos("Preferente", "", 1)));
    }

    @Test
    void listarDevuelveReglasPorOrdenAscendente() throws Exception {
        servicio.crear(administrador, datos("Ordinaria", "", 3));
        servicio.crear(administrador, datos("Urgente", "", 1));
        servicio.crear(administrador, datos("Preferente", "", 2));

        List<ReglaPrioridad> resultado =
                servicio.listar(administrador);

        assertEquals(List.of(1, 2, 3), resultado.stream()
                .map(ReglaPrioridad::getOrden).toList());
    }

    @Test
    void consultarYModificarRegla() throws Exception {
        ReglaPrioridad creada = servicio.crear(
                administrador, datos("Urgente", "", 1));

        ReglaPrioridad consultada =
                servicio.consultar(administrador, creada.getId());
        ReglaPrioridad modificada = servicio.modificar(
                administrador,
                creada.getId(),
                datos("Prioritaria", "atención prioritaria", 2));

        assertEquals("Urgente", consultada.getNombre());
        assertEquals("Prioritaria", modificada.getNombre());
        assertEquals("atención prioritaria",
                modificada.getDescripcion());
        assertEquals(2, modificada.getOrden());
        assertTrue(modificada.isActivo());
        assertEquals("admin", modificada.getModificadoPor());
        assertNotNull(modificada.getModificadoEn());
    }

    @Test
    void modificarRechazaDatosDuplicadosOInvalidos() throws Exception {
        ReglaPrioridad urgente = servicio.crear(
                administrador, datos("Urgente", "", 1));
        servicio.crear(administrador, datos("Ordinaria", "", 2));

        assertThrows(ReglaPrioridadDuplicadaException.class,
                () -> servicio.modificar(
                        administrador,
                        urgente.getId(),
                        datos("Ordinaria", "", 3)));
        assertThrows(ReglaPrioridadDuplicadaException.class,
                () -> servicio.modificar(
                        administrador,
                        urgente.getId(),
                        datos("Urgente", "", 2)));
        assertThrows(DatosReglaPrioridadInvalidosException.class,
                () -> servicio.modificar(
                        administrador,
                        urgente.getId(),
                        datos("", "", 1)));
    }

    @Test
    void desactivarEsBajaLogicaEIdempotente() throws Exception {
        ReglaPrioridad creada = servicio.crear(
                administrador, datos("Urgente", "", 1));

        servicio.desactivar(administrador, creada.getId());
        ReglaPrioridad desactivada =
                servicio.consultar(administrador, creada.getId());
        servicio.desactivar(administrador, creada.getId());
        ReglaPrioridad consultadaDeNuevo =
                servicio.consultar(administrador, creada.getId());

        assertFalse(desactivada.isActivo());
        assertEquals(desactivada.getModificadoEn(),
                consultadaDeNuevo.getModificadoEn());
        assertEquals(1, servicio.listar(administrador).size());
    }

    @Test
    void rechazaIdentificadoresInexistentes() {
        assertThrows(ReglaPrioridadNoEncontradaException.class,
                () -> servicio.consultar(administrador, 999));
        assertThrows(ReglaPrioridadNoEncontradaException.class,
                () -> servicio.modificar(
                        administrador, 999, datos("Urgente", "", 1)));
        assertThrows(ReglaPrioridadNoEncontradaException.class,
                () -> servicio.desactivar(administrador, 999));
    }

    @Test
    void soloAdministradorPuedeGestionarReglas() {
        Usuario medico = new Usuario("medico", Rol.MEDICO);
        DatosReglaPrioridad datos = datos("Urgente", "", 1);

        assertThrows(SinPermisoException.class,
                () -> servicio.crear(medico, datos));
        assertThrows(SinPermisoException.class,
                () -> servicio.listar(medico));
        assertThrows(SinPermisoException.class,
                () -> servicio.consultar(medico, 1));
        assertThrows(SinPermisoException.class,
                () -> servicio.modificar(medico, 1, datos));
        assertThrows(SinPermisoException.class,
                () -> servicio.desactivar(medico, 1));
        assertThrows(SinPermisoException.class,
                () -> servicio.listar(null));
    }

    @Test
    void constructorRechazaRepositorioNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReglasPrioridadServiceImpl(null));
    }
}

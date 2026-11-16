package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TiposPruebaServicioSimuladoTest {

    private TiposPruebaServicioSimulado servicio;

    @BeforeEach
    void setUp() {
        servicio = new TiposPruebaServicioSimulado();
    }

    private DatosTipoPrueba datosValidos() {
        return new DatosTipoPrueba(
                "TAC abdominal",
                "Tomografía abdominal",
                30,
                true,
                "Ayuno de 6 horas");
    }

    @Test
    @DisplayName("El servicio comienza con tres tipos de prueba")
    void servicioComienzaConTresTipos() throws Exception {
        var resultado = servicio.listar(
                new FiltroTipoPrueba("", FiltroTipoPrueba.Estado.TODOS));

        assertEquals(3, resultado.size());
    }

    @Test
    @DisplayName("Crear un tipo de prueba lo añade al listado")
    void crearTipoPrueba() throws Exception {
        TipoPrueba creado = servicio.crear(datosValidos());

        assertAll(
                () -> assertEquals(4L, creado.getId()),
                () -> assertEquals("TAC abdominal", creado.getNombre()),
                () -> assertEquals("Tomografía abdominal", creado.getDescripcion()),
                () -> assertEquals(30, creado.getDuracionEstimada()),
                () -> assertTrue(creado.isRequierePreparacion()),
                () -> assertEquals("Ayuno de 6 horas", creado.getInstruccionesPaciente()),
                () -> assertTrue(creado.isActivo())
        );
    }

    @Test
    @DisplayName("Consultar devuelve el tipo de prueba solicitado")
    void consultarTipoExistente() throws Exception {
        TipoPrueba resultado = servicio.consultar(1L);

        assertAll(
                () -> assertEquals(1L, resultado.getId()),
                () -> assertEquals("Radiografía de tórax", resultado.getNombre())
        );
    }

    @Test
    @DisplayName("Consultar un tipo inexistente produce error")
    void consultarTipoInexistente() {
        assertThrows(
                ServicioException.class,
                () -> servicio.consultar(999L));
    }

    @Test
    @DisplayName("No se puede crear un nombre duplicado")
    void crearNombreDuplicado() {
        DatosTipoPrueba datos = new DatosTipoPrueba(
                "Radiografía de tórax",
                "Otra descripción",
                20,
                false,
                "");

        assertThrows(
                NombreDuplicadoException.class,
                () -> servicio.crear(datos));
    }

    @Test
    @DisplayName("La comprobación de nombres duplicados ignora mayúsculas")
    void crearNombreDuplicadoIgnoraMayusculas() {
        DatosTipoPrueba datos = new DatosTipoPrueba(
                "RADIOGRAFÍA DE TÓRAX",
                "Otra descripción",
                20,
                false,
                "");

        assertThrows(
                NombreDuplicadoException.class,
                () -> servicio.crear(datos));
    }

    @Test
    @DisplayName("Modificar actualiza los datos de un tipo de prueba")
    void modificarTipoPrueba() throws Exception {
        DatosTipoPrueba nuevosDatos = new DatosTipoPrueba(
                "Radiografía torácica",
                "Descripción actualizada",
                25,
                true,
                "Sin preparación especial");

        TipoPrueba modificado =
                servicio.modificar(1L, nuevosDatos);

        assertAll(
                () -> assertEquals(1L, modificado.getId()),
                () -> assertEquals("Radiografía torácica", modificado.getNombre()),
                () -> assertEquals("Descripción actualizada", modificado.getDescripcion()),
                () -> assertEquals(25, modificado.getDuracionEstimada()),
                () -> assertTrue(modificado.isRequierePreparacion()),
                () -> assertEquals(
                        "Sin preparación especial",
                        modificado.getInstruccionesPaciente()),
                () -> assertTrue(modificado.isActivo())
        );
    }

    @Test
    @DisplayName("Modificar permite conservar el mismo nombre")
    void modificarManteniendoMismoNombre() throws Exception {
        DatosTipoPrueba datos = new DatosTipoPrueba(
                "Radiografía de tórax",
                "Descripción modificada",
                20,
                false,
                "");

        TipoPrueba modificado =
                servicio.modificar(1L, datos);

        assertEquals(
                "Radiografía de tórax",
                modificado.getNombre());
    }

    @Test
    @DisplayName("Modificar con el nombre de otro tipo produce error")
    void modificarConNombreDuplicado() {
        DatosTipoPrueba datos = new DatosTipoPrueba(
                "Resonancia cerebral",
                "Descripción",
                20,
                false,
                "");

        assertThrows(
                NombreDuplicadoException.class,
                () -> servicio.modificar(1L, datos));
    }

    @Test
    @DisplayName("Modificar un tipo inexistente produce error")
    void modificarTipoInexistente() {
        assertThrows(
                ServicioException.class,
                () -> servicio.modificar(
                        999L,
                        datosValidos()));
    }

    @Test
    @DisplayName("Desactivar realiza una baja lógica")
    void desactivarTipoPrueba() throws Exception {
        servicio.desactivar(1L);

        TipoPrueba resultado =
                servicio.consultar(1L);

        assertFalse(resultado.isActivo());
    }

    @Test
    @DisplayName("Desactivar un tipo inexistente produce error")
    void desactivarTipoInexistente() {
        assertThrows(
                ServicioException.class,
                () -> servicio.desactivar(999L));
    }

    @Test
    @DisplayName("Listar permite filtrar por texto")
    void listarPorTexto() throws Exception {
        var resultado = servicio.listar(
                new FiltroTipoPrueba(
                        "resonancia",
                        FiltroTipoPrueba.Estado.TODOS));

        assertAll(
                () -> assertEquals(1, resultado.size()),
                () -> assertEquals(
                        "Resonancia cerebral",
                        resultado.get(0).getNombre())
        );
    }

    @Test
    @DisplayName("El filtro de texto ignora mayúsculas")
    void listarTextoIgnoraMayusculas() throws Exception {
        var resultado = servicio.listar(
                new FiltroTipoPrueba(
                        "RESONANCIA",
                        FiltroTipoPrueba.Estado.TODOS));

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar activos excluye los tipos desactivados")
    void listarSoloActivos() throws Exception {
        servicio.desactivar(1L);

        var resultado = servicio.listar(
                new FiltroTipoPrueba(
                        "",
                        FiltroTipoPrueba.Estado.ACTIVOS));

        assertAll(
                () -> assertEquals(2, resultado.size()),
                () -> assertTrue(
                        resultado.stream()
                                .allMatch(TipoPrueba::isActivo))
        );
    }

    @Test
    @DisplayName("Listar inactivos devuelve los tipos desactivados")
    void listarSoloInactivos() throws Exception {
        servicio.desactivar(1L);

        var resultado = servicio.listar(
                new FiltroTipoPrueba(
                        "",
                        FiltroTipoPrueba.Estado.INACTIVOS));

        assertAll(
                () -> assertEquals(1, resultado.size()),
                () -> assertFalse(resultado.get(0).isActivo())
        );
    }

    @Test
    @DisplayName("El tipo 2 simula tres pruebas pendientes")
    void contarPendientesTipoDos() {
        assertEquals(
                3,
                servicio.contarPruebasPendientes(2L));
    }

    @Test
    @DisplayName("Los demás tipos no tienen pruebas pendientes simuladas")
    void contarPendientesOtroTipo() {
        assertEquals(
                0,
                servicio.contarPruebasPendientes(1L));
    }
}
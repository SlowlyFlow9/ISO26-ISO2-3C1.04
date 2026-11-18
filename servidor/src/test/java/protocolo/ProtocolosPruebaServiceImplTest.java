package protocolo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ProtocolosPruebaServiceImplTest {

    private static final Usuario ADMIN =
            new Usuario("admin", Rol.ADMINISTRADOR);

    private ProtocolosPruebaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        ProtocolosPruebaRepositorio repositorio =
                new ProtocolosPruebaRepositorioMemoria();

        TiposPruebaConsulta tiposPrueba =
                id -> id == 1L || id == 2L;

        servicio = new ProtocolosPruebaServiceImpl(
                repositorio, tiposPrueba);
    }

    private ProtocoloPrueba valido() {
        ProtocoloPrueba p = new ProtocoloPrueba();

        p.setNombre("Protocolo TAC");
        p.setDescripcion("Protocolo para realización de TAC");
        p.setTipoPruebaId(1L);
        p.setInstruccionesRealizacion(
                "Realizar según procedimiento");
        p.setRequiereContraste(true);
        p.setRequiereConsentimiento(true);
        p.setRequisitosSeguridad("Comprobar alergias");

        return p;
    }

    private void prepararProtocolosParaFiltros()
            throws Exception {

        ProtocoloPrueba primero = valido();
        primero.setTipoPruebaId(1L);

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        ProtocoloPrueba p1 =
                servicio.crear(ADMIN, primero);

        servicio.crear(ADMIN, segundo);

        servicio.desactivar(ADMIN, p1.getId());
    }

    @Test
    @DisplayName("Crear un protocolo válido lo deja activo y con identificador")
    void crearProtocoloCorrectamente() throws Exception {

        // Preparar
        ProtocoloPrueba datos = valido();

        // Ejecutar
        ProtocoloPrueba creado =
                servicio.crear(ADMIN, datos);

        // Comprobar
        assertAll(
                () -> assertNotNull(creado.getId()),
                () -> assertTrue(creado.isActivo()),
                () -> assertEquals(
                        "Protocolo TAC",
                        creado.getNombre()),
                () -> assertEquals(
                        1L,
                        creado.getTipoPruebaId())
        );
    }

    @Test
    @DisplayName("No se puede crear con un tipo de prueba inexistente")
    void noPermiteTipoPruebaInexistente() {

        ProtocoloPrueba datos = valido();
        datos.setTipoPruebaId(99L);

        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear con el nombre vacío")
    void noPermiteNombreVacio() {

        ProtocoloPrueba datos = valido();
        datos.setNombre("");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear con el nombre formado solo por espacios")
    void noPermiteNombreSoloEspacios() {

        ProtocoloPrueba datos = valido();
        datos.setNombre("   ");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear con la descripción vacía")
    void noPermiteDescripcionVacia() {

        ProtocoloPrueba datos = valido();
        datos.setDescripcion("");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear sin tipo de prueba")
    void noPermiteTipoPruebaNulo() {

        ProtocoloPrueba datos = valido();
        datos.setTipoPruebaId(null);

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("Consultar un protocolo existente devuelve sus datos")
    void consultarDevuelveProtocoloCreado() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        assertAll(
                () -> assertEquals(
                        creado.getId(),
                        consultado.getId()),
                () -> assertEquals(
                        creado.getNombre(),
                        consultado.getNombre())
        );
    }

    @Test
    @DisplayName("Consultar un protocolo inexistente produce un error")
    void consultarProtocoloInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.consultar(
                        ADMIN,
                        999L));
    }

    @Test
    @DisplayName("Modificar un protocolo actualiza sus datos")
    void modificarActualizaDatos() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        ProtocoloPrueba nuevosDatos = valido();
        nuevosDatos.setNombre(
                "Protocolo TAC actualizado");
        nuevosDatos.setTipoPruebaId(2L);

        ProtocoloPrueba modificado =
                servicio.modificar(
                        ADMIN,
                        creado.getId(),
                        nuevosDatos);

        assertAll(
                () -> assertEquals(
                        "Protocolo TAC actualizado",
                        modificado.getNombre()),
                () -> assertEquals(
                        2L,
                        modificado.getTipoPruebaId())
        );
    }

    @Test
    @DisplayName("Modificar un protocolo inexistente produce un error")
    void modificarProtocoloInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.modificar(
                        ADMIN,
                        999L,
                        valido()));
    }

    @Test
    @DisplayName("No se puede modificar usando datos obligatorios vacíos")
    void modificarConDatosInvalidosNoCambiaProtocolo()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        ProtocoloPrueba datosInvalidos = valido();
        datosInvalidos.setDescripcion("");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.modificar(
                        ADMIN,
                        creado.getId(),
                        datosInvalidos));

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        assertEquals(
                "Protocolo para realización de TAC",
                consultado.getDescripcion());
    }

    @Test
    @DisplayName("No se puede modificar usando un tipo de prueba inexistente")
    void modificarConTipoInexistenteNoCambiaProtocolo()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        ProtocoloPrueba nuevosDatos = valido();
        nuevosDatos.setNombre("Nombre modificado");
        nuevosDatos.setTipoPruebaId(99L);

        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio.modificar(
                        ADMIN,
                        creado.getId(),
                        nuevosDatos));

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        assertAll(
                () -> assertEquals(
                        "Protocolo TAC",
                        consultado.getNombre()),
                () -> assertEquals(
                        1L,
                        consultado.getTipoPruebaId())
        );
    }

    @Test
    @DisplayName("El protocolo conserva los datos de seguridad")
    void conservaDatosDeSeguridad() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        assertAll(
                () -> assertTrue(
                        creado.isRequiereContraste()),
                () -> assertTrue(
                        creado.isRequiereConsentimiento()),
                () -> assertEquals(
                        "Comprobar alergias",
                        creado.getRequisitosSeguridad())
        );
    }

    @Test
    @DisplayName("Desactivar realiza una baja lógica conservando los datos")
    void desactivarRealizaBajaLogica() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        Long id = creado.getId();
        String nombre = creado.getNombre();
        String descripcion = creado.getDescripcion();
        Long tipoPruebaId = creado.getTipoPruebaId();

        servicio.desactivar(ADMIN, id);

        ProtocoloPrueba consultado =
                servicio.consultar(ADMIN, id);

        assertAll(
                () -> assertFalse(
                        consultado.isActivo()),
                () -> assertEquals(
                        id,
                        consultado.getId()),
                () -> assertEquals(
                        nombre,
                        consultado.getNombre()),
                () -> assertEquals(
                        descripcion,
                        consultado.getDescripcion()),
                () -> assertEquals(
                        tipoPruebaId,
                        consultado.getTipoPruebaId())
        );
    }

    @Test
    @DisplayName("Desactivar un protocolo inexistente produce un error")
    void desactivarProtocoloInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.desactivar(
                        ADMIN,
                        999L));
    }

    @Test
    @DisplayName("Listar por tipo 1 e inactivo devuelve el protocolo esperado")
    void listarTipoUnoInactivo() throws Exception {

        prepararProtocolosParaFiltros();

        var resultado =
                servicio.listar(ADMIN, 1L, false);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertEquals(
                        1L,
                        resultado.get(0).getTipoPruebaId()),
                () -> assertFalse(
                        resultado.get(0).isActivo())
        );
    }

    @Test
    @DisplayName("Listar por tipo 2 y activo devuelve el protocolo esperado")
    void listarTipoDosActivo() throws Exception {

        prepararProtocolosParaFiltros();

        var resultado =
                servicio.listar(ADMIN, 2L, true);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertEquals(
                        2L,
                        resultado.get(0).getTipoPruebaId()),
                () -> assertTrue(
                        resultado.get(0).isActivo())
        );
    }

    @Test
    @DisplayName("Listar protocolos activos devuelve solo los activos")
    void listarSoloActivos() throws Exception {

        prepararProtocolosParaFiltros();

        var resultado =
                servicio.listar(ADMIN, null, true);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertTrue(
                        resultado.get(0).isActivo())
        );
    }

    @Test
    @DisplayName("Listar protocolos inactivos devuelve solo los inactivos")
    void listarSoloInactivos() throws Exception {

        prepararProtocolosParaFiltros();

        var resultado =
                servicio.listar(ADMIN, null, false);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertFalse(
                        resultado.get(0).isActivo())
        );
    }

    @Test
    @DisplayName("Un médico no puede crear protocolos")
    void medicoNoPuedeCrear() {

        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        medico,
                        valido()));
    }

    @Test
    @DisplayName("Un paciente no puede crear protocolos")
    void pacienteNoPuedeCrear() {

        Usuario paciente =
                new Usuario(
                        "paciente",
                        Rol.PACIENTE);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        paciente,
                        valido()));
    }

    @Test
    @DisplayName("El personal administrativo no puede crear protocolos")
    void personalAdministrativoNoPuedeCrear() {

        Usuario personal =
                new Usuario(
                        "personal",
                        Rol.PERSONAL_ADMINISTRATIVO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        personal,
                        valido()));
    }

    @Test
    @DisplayName("Un usuario nulo no puede crear protocolos")
    void usuarioNuloNoPuedeCrear() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        null,
                        valido()));
    }

    @Test
    @DisplayName("Un médico no puede modificar y el protocolo no cambia")
    void medicoNoPuedeModificar() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        ProtocoloPrueba nuevosDatos = valido();
        nuevosDatos.setNombre("Nombre modificado");

        assertThrows(
                SinPermisoException.class,
                () -> servicio.modificar(
                        medico,
                        creado.getId(),
                        nuevosDatos));

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        assertEquals(
                "Protocolo TAC",
                consultado.getNombre());
    }

    @Test
    @DisplayName("Un médico no puede desactivar y el protocolo sigue activo")
    void medicoNoPuedeDesactivar() throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.desactivar(
                        medico,
                        creado.getId()));

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        assertTrue(consultado.isActivo());
    }
}
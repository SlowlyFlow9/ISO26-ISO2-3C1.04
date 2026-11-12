package protocolo;

import static org.junit.jupiter.api.Assertions.*;

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
        p.setInstruccionesRealizacion("Realizar según procedimiento");
        p.setRequiereContraste(true);
        p.setRequiereConsentimiento(true);
        p.setRequisitosSeguridad("Comprobar alergias");

        return p;
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
    @DisplayName("No se puede crear un protocolo con un tipo de prueba inexistente")
    void noPermiteTipoPruebaInexistente() {

        // Preparar
        ProtocoloPrueba datos = valido();
        datos.setTipoPruebaId(99L);

        // Ejecutar y comprobar
        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear un protocolo con el nombre vacío")
    void noPermiteNombreVacio() {

        // Preparar
        ProtocoloPrueba datos = valido();
        datos.setNombre("");

        // Ejecutar y comprobar
        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear un protocolo con el nombre formado solo por espacios")
    void noPermiteNombreSoloEspacios() {

        // Preparar
        ProtocoloPrueba datos = valido();
        datos.setNombre("   ");

        // Ejecutar y comprobar
        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("No se puede crear un protocolo sin tipo de prueba")
    void noPermiteTipoPruebaNulo() {

        // Preparar
        ProtocoloPrueba datos = valido();
        datos.setTipoPruebaId(null);

        // Ejecutar y comprobar
        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.crear(ADMIN, datos));
    }

    @Test
    @DisplayName("Consultar un protocolo existente devuelve sus datos")
    void consultarDevuelveProtocoloCreado() throws Exception {

        // Preparar
        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        // Ejecutar
        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        // Comprobar
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

        // Preparar
        long idInexistente = 999L;

        // Ejecutar y comprobar
        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.consultar(
                        ADMIN,
                        idInexistente));
    }

    @Test
    @DisplayName("Modificar un protocolo actualiza sus datos")
    void modificarActualizaDatos() throws Exception {

        // Preparar
        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        ProtocoloPrueba nuevosDatos =
                valido();

        nuevosDatos.setNombre(
                "Protocolo TAC actualizado");

        nuevosDatos.setTipoPruebaId(2L);

        // Ejecutar
        ProtocoloPrueba modificado =
                servicio.modificar(
                        ADMIN,
                        creado.getId(),
                        nuevosDatos);

        // Comprobar
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

        // Preparar
        long idInexistente = 999L;

        // Ejecutar y comprobar
        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.modificar(
                        ADMIN,
                        idInexistente,
                        valido()));
    }

    @Test
    @DisplayName("El protocolo conserva contraste, consentimiento y requisitos de seguridad")
    void conservaDatosDeSeguridad() throws Exception {

        // Preparar
        ProtocoloPrueba datos = valido();

        // Ejecutar
        ProtocoloPrueba creado =
                servicio.crear(ADMIN, datos);

        // Comprobar
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
    @DisplayName("Desactivar un protocolo realiza una baja lógica")
    void desactivarRealizaBajaLogica() throws Exception {

        // Preparar
        ProtocoloPrueba creado =
                servicio.crear(ADMIN, valido());

        // Ejecutar
        servicio.desactivar(
                ADMIN,
                creado.getId());

        ProtocoloPrueba consultado =
                servicio.consultar(
                        ADMIN,
                        creado.getId());

        // Comprobar
        assertFalse(consultado.isActivo());
    }

    @Test
    @DisplayName("El listado filtra protocolos por tipo y estado")
    void listarFiltraPorTipoYEstado() throws Exception {

        // Preparar
        ProtocoloPrueba primero = valido();
        primero.setTipoPruebaId(1L);

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        ProtocoloPrueba p1 =
                servicio.crear(ADMIN, primero);

        servicio.crear(ADMIN, segundo);

        servicio.desactivar(
                ADMIN,
                p1.getId());

        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(
                        1,
                        servicio.listar(
                                ADMIN,
                                1L,
                                false).size()),

                () -> assertEquals(
                        1,
                        servicio.listar(
                                ADMIN,
                                2L,
                                true).size()),

                () -> assertEquals(
                        1,
                        servicio.listar(
                                ADMIN,
                                null,
                                true).size()),

                () -> assertEquals(
                        1,
                        servicio.listar(
                                ADMIN,
                                null,
                                false).size())
        );
    }

    @Test
    @DisplayName("Un usuario que no es administrador no puede crear protocolos")
    void soloAdministradorPuedeEscribir() {

        // Preparar
        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        // Ejecutar y comprobar
        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        medico,
                        valido()));
    }
}
package protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ProtocolosPruebaServiceSimuladoCoberturaTest {

    private Usuario admin;
    private ProtocolosPruebaServiceSimulado servicio;

    @BeforeEach
    void setUp() {
        admin = new Usuario(
                "admin",
                Rol.ADMINISTRADOR);

        servicio =
                new ProtocolosPruebaServiceSimulado(
                        List.of(1L, 2L));
    }

    private ProtocoloPrueba valido() {
        ProtocoloPrueba p =
                new ProtocoloPrueba();

        p.setNombre("Protocolo TAC");
        p.setDescripcion("Descripción");
        p.setTipoPruebaId(1L);
        p.setInstruccionesRealizacion(
                "Realizar en ayunas");
        p.setRequisitosSeguridad(
                "Comprobar alergias");

        return p;
    }

    @Test
    @DisplayName(
            "Consultar un protocolo inexistente genera error")
    void consultarInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.consultar(
                        admin,
                        999L));
    }

    @Test
    @DisplayName(
            "Listar sin filtros devuelve todos los protocolos")
    void listarSinFiltros()
            throws Exception {

        servicio.crear(admin, valido());

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        servicio.crear(admin, segundo);

        assertEquals(
                2,
                servicio.listar(
                        admin,
                        null,
                        null).size());
    }

    @Test
    @DisplayName(
            "Listar filtra únicamente por tipo")
    void listarPorTipo()
            throws Exception {

        servicio.crear(admin, valido());

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        servicio.crear(admin, segundo);

        var resultado =
                servicio.listar(
                        admin,
                        2L,
                        null);

        assertEquals(1, resultado.size());
        assertEquals(
                2L,
                resultado.get(0).getTipoPruebaId());
    }

    @Test
    @DisplayName(
            "Listar filtra únicamente por estado")
    void listarPorEstado()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");

        servicio.crear(admin, segundo);

        servicio.desactivar(
                admin,
                creado.getId());

        assertEquals(
                1,
                servicio.listar(
                        admin,
                        null,
                        false).size());
    }

    @Test
    @DisplayName(
            "Modificar con datos inválidos genera error")
    void modificarDatosInvalidos()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba datos =
                valido();

        datos.setNombre(" ");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio.modificar(
                        admin,
                        creado.getId(),
                        datos));
    }

    @Test
    @DisplayName(
            "Modificar con tipo inexistente genera error")
    void modificarTipoInexistente()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba datos =
                valido();

        datos.setTipoPruebaId(999L);

        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio.modificar(
                        admin,
                        creado.getId(),
                        datos));
    }

    @Test
    @DisplayName(
            "Modificar un protocolo inexistente genera error")
    void modificarProtocoloInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.modificar(
                        admin,
                        999L,
                        valido()));
    }

    @Test
    @DisplayName(
            "Desactivar un protocolo inexistente genera error")
    void desactivarInexistente() {

        assertThrows(
                ProtocoloNoEncontradoException.class,
                () -> servicio.desactivar(
                        admin,
                        999L));
    }

    @Test
    @DisplayName(
            "Un médico no puede modificar protocolos")
    void medicoNoPuedeModificar()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.modificar(
                        medico,
                        creado.getId(),
                        valido()));
    }

    @Test
    @DisplayName(
            "Un médico no puede desactivar protocolos")
    void medicoNoPuedeDesactivar()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.desactivar(
                        medico,
                        creado.getId()));
    }

    @Test
    @DisplayName(
            "Un usuario nulo no puede crear protocolos")
    void usuarioNuloNoPuedeCrear() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        null,
                        valido()));
    }

    @Test
    @DisplayName(
            "Modificar limpia campos opcionales vacíos")
    void modificarLimpiaCamposVacios()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba datos =
                valido();

        datos.setInstruccionesRealizacion("   ");
        datos.setRequisitosSeguridad(null);

        ProtocoloPrueba modificado =
                servicio.modificar(
                        admin,
                        creado.getId(),
                        datos);

        assertNull(
                modificado.getInstruccionesRealizacion());

        assertNull(
                modificado.getRequisitosSeguridad());
    }

    @Test
    @DisplayName(
            "Modificar recorta campos opcionales")
    void modificarRecortaCamposOpcionales()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba datos =
                valido();

        datos.setInstruccionesRealizacion(
                "  Ayuno  ");

        datos.setRequisitosSeguridad(
                "  Sin metal  ");

        ProtocoloPrueba modificado =
                servicio.modificar(
                        admin,
                        creado.getId(),
                        datos);

        assertEquals(
                "Ayuno",
                modificado.getInstruccionesRealizacion());

        assertEquals(
                "Sin metal",
                modificado.getRequisitosSeguridad());
    }

    @Test
    @DisplayName(
            "El protocolo devuelto es una copia independiente")
    void consultarDevuelveCopia()
            throws Exception {

        ProtocoloPrueba creado =
                servicio.crear(
                        admin,
                        valido());

        ProtocoloPrueba consultado =
                servicio.consultar(
                        admin,
                        creado.getId());

        consultado.setNombre("Modificado fuera");

        ProtocoloPrueba original =
                servicio.consultar(
                        admin,
                        creado.getId());

        assertEquals(
                "Protocolo TAC",
                original.getNombre());

        assertTrue(original.isActivo());
    }
}
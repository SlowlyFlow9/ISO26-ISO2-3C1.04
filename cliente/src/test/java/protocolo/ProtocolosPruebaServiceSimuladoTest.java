package protocolo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ProtocolosPruebaServiceSimuladoTest {

    private final Usuario admin =
            new Usuario(
                    "admin",
                    Rol.ADMINISTRADOR);

    private ProtocolosPruebaServiceSimulado servicio() {

        return new ProtocolosPruebaServiceSimulado(
                List.of(1L, 2L));
    }

    private ProtocoloPrueba valido() {

        ProtocoloPrueba p =
                new ProtocoloPrueba();

        p.setNombre("Protocolo TAC");
        p.setDescripcion("Descripción");
        p.setTipoPruebaId(1L);
        p.setRequiereContraste(true);
        p.setRequiereConsentimiento(true);
        p.setRequisitosSeguridad(
                "Comprobar alergias");

        return p;
    }

    @Test
    void crearProtocolo() throws Exception {

        ProtocoloPrueba creado =
                servicio().crear(
                        admin,
                        valido());

        assertNotNull(creado.getId());
        assertTrue(creado.isActivo());
    }

    @Test
    void tipoInexistenteDaError() {

        ProtocoloPrueba p = valido();

        p.setTipoPruebaId(99L);

        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio().crear(admin, p));
    }

    @Test
    void usuarioNoAdministradorNoPuedeCrear() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio().crear(
                        medico,
                        valido()));
    }

    @Test
    void modificarProtocolo()
            throws Exception {

        ProtocolosPruebaServiceSimulado s =
                servicio();

        ProtocoloPrueba creado =
                s.crear(admin, valido());

        ProtocoloPrueba datos =
                valido();

        datos.setNombre("Protocolo nuevo");

        ProtocoloPrueba modificado =
                s.modificar(
                        admin,
                        creado.getId(),
                        datos);

        assertEquals(
                "Protocolo nuevo",
                modificado.getNombre());
    }

    @Test
    void desactivarHaceBajaLogica()
            throws Exception {

        ProtocolosPruebaServiceSimulado s =
                servicio();

        ProtocoloPrueba creado =
                s.crear(admin, valido());

        s.desactivar(
                admin,
                creado.getId());

        ProtocoloPrueba consultado =
                s.consultar(
                        admin,
                        creado.getId());

        assertFalse(
                consultado.isActivo());
    }

    @Test
    void listarFiltraPorEstadoYTipo()
            throws Exception {

        ProtocolosPruebaServiceSimulado s =
                servicio();

        ProtocoloPrueba primero =
                valido();

        ProtocoloPrueba segundo =
                valido();

        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        ProtocoloPrueba creado =
                s.crear(admin, primero);

        s.crear(admin, segundo);

        s.desactivar(
                admin,
                creado.getId());

        assertEquals(
                1,
                s.listar(
                        admin,
                        1L,
                        false).size());

        assertEquals(
                1,
                s.listar(
                        admin,
                        2L,
                        true).size());
    }
}
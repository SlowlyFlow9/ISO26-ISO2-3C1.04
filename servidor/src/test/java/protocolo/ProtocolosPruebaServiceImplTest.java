package protocolo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class ProtocolosPruebaServiceImplTest {

    private static final Usuario ADMIN =
            new Usuario("admin", Rol.ADMINISTRADOR);

    private ProtocolosPruebaServiceImpl servicio() {
        ProtocolosPruebaRepositorio repositorio =
                new ProtocolosPruebaRepositorioMemoria();

        TiposPruebaConsulta tiposPrueba =
                id -> id == 1L || id == 2L;

        return new ProtocolosPruebaServiceImpl(
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
    void crearProtocoloCorrectamente() throws Exception {
        ProtocoloPrueba creado =
                servicio().crear(ADMIN, valido());

        assertNotNull(creado.getId());
        assertTrue(creado.isActivo());
        assertEquals("Protocolo TAC", creado.getNombre());
        assertEquals(1L, creado.getTipoPruebaId());
    }

    @Test
    void noPermiteTipoPruebaInexistente() {
        ProtocoloPrueba p = valido();
        p.setTipoPruebaId(99L);

        assertThrows(
                TipoPruebaNoEncontradoException.class,
                () -> servicio().crear(ADMIN, p));
    }

    @Test
    void noPermiteCamposObligatoriosVacios() {
        ProtocoloPrueba p = valido();
        p.setNombre(" ");

        assertThrows(
                DatosProtocoloInvalidosException.class,
                () -> servicio().crear(ADMIN, p));
    }

    @Test
    void consultarDevuelveProtocoloCreado() throws Exception {
        ProtocolosPruebaServiceImpl s = servicio();

        ProtocoloPrueba creado = s.crear(ADMIN, valido());

        ProtocoloPrueba consultado =
                s.consultar(ADMIN, creado.getId());

        assertEquals(creado.getId(), consultado.getId());
        assertEquals(creado.getNombre(), consultado.getNombre());
    }

    @Test
    void modificarActualizaDatos() throws Exception {
        ProtocolosPruebaServiceImpl s = servicio();

        ProtocoloPrueba creado = s.crear(ADMIN, valido());

        ProtocoloPrueba nuevosDatos = valido();
        nuevosDatos.setNombre("Protocolo TAC actualizado");
        nuevosDatos.setTipoPruebaId(2L);

        ProtocoloPrueba modificado =
                s.modificar(ADMIN, creado.getId(), nuevosDatos);

        assertEquals(
                "Protocolo TAC actualizado",
                modificado.getNombre());

        assertEquals(2L, modificado.getTipoPruebaId());
    }

    @Test
    void conservaDatosDeContrasteConsentimientoYSeguridad()
            throws Exception {

        ProtocoloPrueba creado =
                servicio().crear(ADMIN, valido());

        assertTrue(creado.isRequiereContraste());
        assertTrue(creado.isRequiereConsentimiento());

        assertEquals(
                "Comprobar alergias",
                creado.getRequisitosSeguridad());
    }

    @Test
    void desactivarRealizaBajaLogica() throws Exception {
        ProtocolosPruebaServiceImpl s = servicio();

        ProtocoloPrueba creado = s.crear(ADMIN, valido());

        s.desactivar(ADMIN, creado.getId());

        ProtocoloPrueba consultado =
                s.consultar(ADMIN, creado.getId());

        assertFalse(consultado.isActivo());
    }

    @Test
    void listarFiltraPorTipoYEstado() throws Exception {
        ProtocolosPruebaServiceImpl s = servicio();

        ProtocoloPrueba primero = valido();
        primero.setTipoPruebaId(1L);

        ProtocoloPrueba segundo = valido();
        segundo.setNombre("Protocolo RM");
        segundo.setTipoPruebaId(2L);

        ProtocoloPrueba p1 = s.crear(ADMIN, primero);
        s.crear(ADMIN, segundo);

        s.desactivar(ADMIN, p1.getId());

        assertEquals(1, s.listar(ADMIN, 1L, false).size());
        assertEquals(1, s.listar(ADMIN, 2L, true).size());
        assertEquals(1, s.listar(ADMIN, null, true).size());
        assertEquals(1, s.listar(ADMIN, null, false).size());
    }

    @Test
    void soloAdministradorPuedeEscribir() {
        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio().crear(medico, valido()));
    }
}
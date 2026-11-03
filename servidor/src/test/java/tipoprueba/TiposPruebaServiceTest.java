package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TiposPruebaServiceTest {

    private SesionSimple sesion;
    private RegistroAuditoriaMemoria auditoria;
    private int pendientes;
    private TiposPruebaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        sesion = new SesionSimple("admin", Rol.ADMINISTRADOR);
        auditoria = new RegistroAuditoriaMemoria();
        pendientes = 0;
        servicio = new TiposPruebaServiceImpl(new RepositorioTiposPruebaMemoria(), sesion,
                id -> pendientes, auditoria);
    }

    private static DatosTipoPrueba datos(String nombre, int minutos) {
        return new DatosTipoPrueba(nombre, "desc", minutos, false, "");
    }

    // ---- crear ----

    @Test
    void creaTipoConNombreUnicoYDuracionPositiva() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("Radiografía de tórax", 15));
        assertTrue(t.getId() > 0);
        assertTrue(t.isActivo());
        assertEquals(15, t.getDuracionEstimada());
    }

    @Test
    void rechazaNombreDuplicadoSinDistinguirMayusculas() throws ServicioException {
        servicio.crear(datos("TAC craneal", 30));
        assertThrows(NombreDuplicadoException.class, () -> servicio.crear(datos("  tac CRANEAL ", 20)));
    }

    @Test
    void rechazaDuracionCeroONegativa() {
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(datos("A", 0)));
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(datos("A", -5)));
    }

    @Test
    void rechazaNombreVacioONulo() {
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(datos("   ", 10)));
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(datos(null, 10)));
    }

    @Test
    void exigeInstruccionesSiRequierePreparacion() {
        assertThrows(DatosInvalidosException.class,
                () -> servicio.crear(new DatosTipoPrueba("RM", "d", 45, true, " ")));
    }

    // ---- consultar / modificar ----

    @Test
    void consultarInexistenteDaTipoNoEncontrado() {
        assertThrows(TipoPruebaNoEncontradoException.class, () -> servicio.consultar(99));
    }

    @Test
    void modificaYSeConsultaElResultado() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("Eco", 20));
        servicio.modificar(t.getId(), new DatosTipoPrueba("Ecografía", "nueva", 25, true, "Ayunas"));
        TipoPrueba m = servicio.consultar(t.getId());
        assertEquals("Ecografía", m.getNombre());
        assertEquals(25, m.getDuracionEstimada());
        assertEquals("Ayunas", m.getInstruccionesPaciente());
    }

    @Test
    void modificarPuedeConservarSuPropioNombrePeroNoUsarElDeOtro() throws ServicioException {
        TipoPrueba a = servicio.crear(datos("A", 10));
        servicio.crear(datos("B", 10));
        servicio.modificar(a.getId(), datos("A", 12)); // mismo nombre: válido
        assertThrows(NombreDuplicadoException.class, () -> servicio.modificar(a.getId(), datos("b", 12)));
    }

    // ---- listar ----

    @Test
    void listaFiltraPorNombreYPorEstado() throws ServicioException {
        servicio.crear(datos("Ecografía abdominal", 30));
        servicio.crear(datos("Resonancia cerebral", 45));
        TipoPrueba tac = servicio.crear(datos("TAC torácico", 20));
        servicio.desactivar(tac.getId());

        assertEquals(3, servicio.listar(new FiltroTipoPrueba("", FiltroTipoPrueba.Estado.TODOS)).size());
        List<TipoPrueba> porNombre = servicio.listar(new FiltroTipoPrueba("ECO", FiltroTipoPrueba.Estado.TODOS));
        assertEquals(1, porNombre.size());
        assertEquals(2, servicio.listar(new FiltroTipoPrueba("", FiltroTipoPrueba.Estado.ACTIVOS)).size());
        List<TipoPrueba> inactivos = servicio.listar(new FiltroTipoPrueba("", FiltroTipoPrueba.Estado.INACTIVOS));
        assertEquals(1, inactivos.size());
        assertEquals("TAC torácico", inactivos.get(0).getNombre());
    }

    // ---- desactivar ----

    @Test
    void desactivarEsBajaLogicaYNoBorra() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("Mamografía", 20));
        servicio.desactivar(t.getId());
        assertFalse(servicio.consultar(t.getId()).isActivo()); // se conserva
        assertTrue(servicio.listar(new FiltroTipoPrueba("", FiltroTipoPrueba.Estado.ACTIVOS)).isEmpty());
    }

    @Test
    void desactivarConPendientesSinConfirmarFalla() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("RM", 40));
        pendientes = 3;
        PruebasPendientesException e = assertThrows(PruebasPendientesException.class,
                () -> servicio.desactivar(t.getId()));
        assertEquals(3, e.getPendientes());
        assertTrue(servicio.consultar(t.getId()).isActivo());
        assertEquals(3, servicio.contarPruebasPendientes(t.getId()));
    }

    @Test
    void desactivarConPendientesConfirmadoFunciona() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("RM", 40));
        pendientes = 3;
        servicio.desactivar(t.getId(), true);
        assertFalse(servicio.consultar(t.getId()).isActivo());
    }

    // ---- permisos ----

    @Test
    void sinRolAdministradorNoSePuedeEscribirPeroSiConsultar() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("Rx", 10));
        for (Rol rol : new Rol[] { Rol.PACIENTE, Rol.MEDICO, Rol.ADMISION, null }) {
            sesion.setRol(rol);
            assertThrows(PermisoDenegadoException.class, () -> servicio.crear(datos("Otro", 10)));
            assertThrows(PermisoDenegadoException.class, () -> servicio.modificar(t.getId(), datos("Rx2", 10)));
            assertThrows(PermisoDenegadoException.class, () -> servicio.desactivar(t.getId()));
            assertEquals("Rx", servicio.consultar(t.getId()).getNombre());
            assertEquals(1, servicio.listar(new FiltroTipoPrueba("", null)).size());
        }
    }

    // ---- auditoría ----

    @Test
    void registraCadaModificacionConElUsuarioYNoLosIntentosRechazados() throws ServicioException {
        TipoPrueba t = servicio.crear(datos("Eco", 10));
        servicio.modificar(t.getId(), datos("Eco2", 12));
        servicio.desactivar(t.getId());
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(datos("", 10)));

        List<EventoAuditoria> eventos = auditoria.eventos();
        assertEquals(3, eventos.size());
        assertEquals(EventoAuditoria.Accion.CREAR, eventos.get(0).getAccion());
        assertEquals(EventoAuditoria.Accion.MODIFICAR, eventos.get(1).getAccion());
        assertEquals(EventoAuditoria.Accion.DESACTIVAR, eventos.get(2).getAccion());
        assertEquals("admin", eventos.get(0).getUsuario());
    }
}

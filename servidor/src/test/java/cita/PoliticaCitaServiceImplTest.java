package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class PoliticaCitaServiceImplTest {

    private PoliticaCitaRepositorioMemoria repositorio;
    private PoliticaCitaServiceImpl servicio;
    private Usuario administrador;

    @BeforeEach
    void preparar() {
        repositorio = new PoliticaCitaRepositorioMemoria();
        servicio = new PoliticaCitaServiceImpl(repositorio);
        administrador = new Usuario("admin", Rol.ADMINISTRADOR);
    }

    private PoliticaCita politicaValida() {
        PoliticaCita politica = new PoliticaCita();
        politica.setAntelacionMinimaHoras(0);
        politica.setAntelacionMaximaDias(90);
        politica.setCancelacionMinimaHoras(24);
        politica.setMaxCitasActivasPorPaciente(3);
        return politica;
    }

    @Test
    void consultarDevuelveValoresIniciales() throws Exception {
        PoliticaCita politica = servicio.consultar(administrador);

        assertEquals(0, politica.getAntelacionMinimaHoras());
        assertEquals(90, politica.getAntelacionMaximaDias());
        assertEquals(24, politica.getCancelacionMinimaHoras());
        assertEquals(3, politica.getMaxCitasActivasPorPaciente());
    }

    @Test
    void modificarGuardaPoliticaYAuditoria() throws Exception {
        PoliticaCita guardada =
                servicio.modificar(administrador, politicaValida());

        assertEquals("admin", guardada.getModificadoPor());
        assertNotNull(guardada.getModificadoEn());
        assertEquals(guardada.getModificadoEn(),
                servicio.consultar(administrador).getModificadoEn());
    }

    @Test
    void rechazarUsuarioSinPermisoAlConsultarOModificar() {
        Usuario medico = new Usuario("medico", Rol.MEDICO);

        assertThrows(SinPermisoException.class,
                () -> servicio.consultar(medico));
        assertThrows(SinPermisoException.class,
                () -> servicio.modificar(medico, politicaValida()));
    }

    @Test
    void rechazarDatosNulosOCamposIncompletos() {
        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, null));

        PoliticaCita incompleta = politicaValida();
        incompleta.setCancelacionMinimaHoras(null);
        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, incompleta));
    }

    @Test
    void rechazarValoresNegativos() {
        PoliticaCita minimaNegativa = politicaValida();
        minimaNegativa.setAntelacionMinimaHoras(-1);
        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, minimaNegativa));

        PoliticaCita maximaNegativa = politicaValida();
        maximaNegativa.setAntelacionMaximaDias(-1);
        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, maximaNegativa));

        PoliticaCita cancelacionNegativa = politicaValida();
        cancelacionNegativa.setCancelacionMinimaHoras(-1);
        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(
                        administrador, cancelacionNegativa));
    }

    @Test
    void rechazarMaximoDeCitasNoPositivo() {
        PoliticaCita politica = politicaValida();
        politica.setMaxCitasActivasPorPaciente(0);

        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, politica));
    }

    @Test
    void rechazarVentanaMaximaNoSuperiorALaAntelacionMinima() {
        PoliticaCita politica = politicaValida();
        politica.setAntelacionMinimaHoras(24);
        politica.setAntelacionMaximaDias(1);

        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, politica));
    }

    @Test
    void rechazarCancelacionFueraDeLaVentanaDeReserva() {
        PoliticaCita politica = politicaValida();
        politica.setCancelacionMinimaHoras(2161);

        assertThrows(DatosPoliticaCitaInvalidosException.class,
                () -> servicio.modificar(administrador, politica));
    }

    @Test
    void permitirCancelacionIgualALaVentanaMaxima()
            throws Exception {
        PoliticaCita politica = politicaValida();
        politica.setCancelacionMinimaHoras(90 * 24);

        assertEquals(90 * 24,
                servicio.modificar(administrador, politica)
                        .getCancelacionMinimaHoras());
    }

    @Test
    void modificacionNoAlteraElObjetoRecibido() throws Exception {
        PoliticaCita entrada = politicaValida();
        PoliticaCita guardada =
                servicio.modificar(administrador, entrada);

        assertTrue(entrada.getModificadoPor() == null);
        assertNotNull(guardada.getModificadoPor());
    }
}

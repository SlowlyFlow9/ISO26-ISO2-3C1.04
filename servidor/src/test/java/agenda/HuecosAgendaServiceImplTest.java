package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU29 - Pruebas del servicio de huecos de agenda.
 * Comprueba las reglas de negocio y los permisos.
 */
public class HuecosAgendaServiceImplTest {

    private AgendasRepositorioMemoria agendas;
    private HuecosAgendaRepositorioMemoria huecos;
    private HuecosAgendaServiceImpl servicio;
    private Usuario administrativo;
    private long agendaId;

    @BeforeEach
    void preparar() throws Exception {

        agendas = new AgendasRepositorioMemoria();
        huecos = new HuecosAgendaRepositorioMemoria();

        servicio = new HuecosAgendaServiceImpl(
                agendas, huecos);

        administrativo = new Usuario(
                "Administrativo",
                Rol.PERSONAL_ADMINISTRATIVO);

        // Crear una agenda válida de 08:00 a 14:00.
        AgendaMedica datos = new AgendaMedica();
        datos.setMedicoId(1L);
        datos.setFecha(LocalDate.of(2026, 11, 20));
        datos.setHoraInicio(LocalTime.of(8, 0));
        datos.setHoraFin(LocalTime.of(14, 0));

        AgendaMedica guardada =
                new AgendasServiceImpl(agendas)
                        .crear(administrativo, datos);

        agendaId = guardada.getId();
    }

    private HuecoAgenda datos(
            int horaInicio, int minutoInicio,
            int horaFin, int minutoFin) {

        HuecoAgenda hueco = new HuecoAgenda();
        hueco.setAgendaId(agendaId);
        hueco.setHoraInicio(
                LocalTime.of(horaInicio, minutoInicio));
        hueco.setHoraFin(
                LocalTime.of(horaFin, minutoFin));

        return hueco;
    }

    @Test
    void crearHuecoValido() throws Exception {

        // EJECUTAR
        HuecoAgenda creado = servicio.crear(
                administrativo, datos(9, 0, 9, 30));

        // COMPROBAR
        assertNotNull(creado.getId());
        assertEquals(agendaId, creado.getAgendaId());
        assertFalse(creado.isBloqueado());

        assertEquals(1,
                servicio.listarPorAgenda(
                        administrativo, agendaId).size());
    }

    @Test
    void rechazarUsuarioSinPermiso() {

        Usuario medico = new Usuario(
                "Medico", Rol.MEDICO);

        assertThrows(SinPermisoException.class,
                () -> servicio.crear(
                        medico, datos(9, 0, 9, 30)));

        assertTrue(huecos.listarPorAgenda(agendaId).isEmpty());
    }

    @Test
    void rechazarHuecoNulo() {

        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, null));

        assertTrue(huecos.listarPorAgenda(agendaId).isEmpty());
    }

    @Test
    void rechazarIdentificadorAsignado() {

        HuecoAgenda nuevo = datos(9, 0, 9, 30);
        nuevo.setId(10L);

        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, nuevo));

        assertTrue(huecos.listarPorAgenda(agendaId).isEmpty());
    }

    @Test
    void rechazarAgendaInexistente() {

        HuecoAgenda nuevo = datos(9, 0, 9, 30);
        nuevo.setAgendaId(999L);

        assertThrows(AgendaNoEncontradaException.class,
                () -> servicio.crear(
                        administrativo, nuevo));

        assertThrows(AgendaNoEncontradaException.class,
                () -> servicio.listarPorAgenda(
                        administrativo, 999L));
    }

    @Test
    void rechazarAgendaInactiva() {

        // PREPARAR: desactivar la agenda.
        agendas.buscarPorId(agendaId)
                .orElseThrow()
                .setActivo(false);

        // COMPROBAR: no se pueden crear nuevos huecos.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(9, 0, 9, 30)));
    }

    @Test
    void rechazarIntervalosInvalidos() {

        // Inicio y fin iguales.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(10, 0, 10, 0)));

        // Hora de fin anterior al inicio.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(11, 0, 10, 0)));

        // Hora de inicio nula.
        HuecoAgenda nuevo = datos(9, 0, 9, 30);
        nuevo.setHoraInicio(null);

        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(administrativo, nuevo));

        assertTrue(huecos.listarPorAgenda(agendaId).isEmpty());
    }

    @Test
    void rechazarHuecosFueraDeHorario() {

        // La agenda comienza a las 08:00.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(7, 30, 8, 30)));

        // La agenda termina a las 14:00.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(13, 30, 14, 30)));
    }

    @Test
    void rechazarSolapamiento() throws Exception {

        // PREPARAR: existe un hueco de 09:00 a 10:00.
        servicio.crear(
                administrativo, datos(9, 0, 10, 0));

        // EJECUTAR: intentar crear otro superpuesto.
        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.crear(
                        administrativo, datos(9, 30, 10, 30)));

        // COMPROBAR: no se guarda el segundo.
        assertEquals(1,
                huecos.listarPorAgenda(agendaId).size());
    }

    @Test
    void permitirHuecosConsecutivos() throws Exception {

        servicio.crear(
                administrativo, datos(9, 0, 9, 30));

        servicio.crear(
                administrativo, datos(9, 30, 10, 0));

        assertEquals(2,
                servicio.listarPorAgenda(
                        administrativo, agendaId).size());
    }

    @Test
    void bloquearYLiberarHueco() throws Exception {

        // PREPARAR
        HuecoAgenda creado = servicio.crear(
                administrativo, datos(9, 0, 9, 30));

        // EJECUTAR Y COMPROBAR bloqueo.
        HuecoAgenda bloqueado = servicio.bloquear(
                administrativo, creado.getId());

        assertTrue(bloqueado.isBloqueado());

        // EJECUTAR Y COMPROBAR liberación.
        HuecoAgenda liberado = servicio.liberar(
                administrativo, creado.getId());

        assertFalse(liberado.isBloqueado());
    }

    @Test
    void rechazarBloqueoYLiberacionRepetidos()
            throws Exception {

        HuecoAgenda creado = servicio.crear(
                administrativo, datos(9, 0, 9, 30));

        servicio.bloquear(
                administrativo, creado.getId());

        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.bloquear(
                        administrativo, creado.getId()));

        servicio.liberar(
                administrativo, creado.getId());

        assertThrows(DatosHuecoInvalidosException.class,
                () -> servicio.liberar(
                        administrativo, creado.getId()));
    }

    @Test
    void rechazarHuecoInexistente() {

        assertThrows(HuecoAgendaNoEncontradoException.class,
                () -> servicio.bloquear(
                        administrativo, 999L));

        assertThrows(HuecoAgendaNoEncontradoException.class,
                () -> servicio.liberar(
                        administrativo, 999L));
    }

    @Test
    void protegerLosDatosAlDevolverUnHueco()
            throws Exception {

        HuecoAgenda creado = servicio.crear(
                administrativo, datos(9, 0, 9, 30));

        // Intentar modificar el objeto recibido.
        creado.setBloqueado(true);
        creado.setHoraInicio(LocalTime.of(8, 0));

        // Consultar de nuevo los datos reales.
        HuecoAgenda almacenado =
                servicio.listarPorAgenda(
                        administrativo, agendaId).get(0);

        assertFalse(almacenado.isBloqueado());
        assertEquals(
                LocalTime.of(9, 0),
                almacenado.getHoraInicio());
    }


}

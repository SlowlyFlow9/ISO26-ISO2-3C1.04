package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

class CitasRealizacionServiceImplTest {

    private static final long MEDICO_ID = 30L;
    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 27, 10, 0);
    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, MEDICO_ID);

    private CitasRepositorioMemoria citas;
    private SolicitudesRepositorioMemoria solicitudes;
    private CitasRealizacionServiceImpl servicio;

    @BeforeEach
    void preparar() {
        citas = new CitasRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();
        servicio = new CitasRealizacionServiceImpl(
                citas, solicitudes);
    }

    private Cita guardarCita(
            long medicoId,
            EstadoCita estado,
            Long solicitudId) {
        Cita cita = new Cita();
        cita.setPacienteId(10L);
        cita.setMedicoId(medicoId);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(FECHA);
        cita.setEstado(estado);
        cita.setSolicitudId(solicitudId);
        return citas.guardar(cita);
    }

    private SolicitudPrueba guardarSolicitud(
            EstadoSolicitud estado) {
        return solicitudes.guardar(new SolicitudPrueba(
                0,
                10,
                MEDICO_ID,
                40,
                "Prueba",
                "Seguimiento",
                Prioridad.ORDINARIA,
                FECHA,
                estado));
    }

    @Test
    void marcarRealizadaActualizaCitaYSolicitudVinculada()
            throws Exception {
        SolicitudPrueba solicitud =
                guardarSolicitud(EstadoSolicitud.CITADA);
        Cita cita = guardarCita(
                MEDICO_ID,
                EstadoCita.CONFIRMADA,
                solicitud.getId());

        Cita realizada =
                servicio.marcarRealizada(MEDICO, cita.getId());

        assertEquals(EstadoCita.REALIZADA, realizada.getEstado());
        assertEquals(cita.getId(), realizada.getId());
        assertEquals(EstadoCita.REALIZADA,
                citas.buscarPorId(cita.getId())
                        .orElseThrow().getEstado());
        assertEquals(EstadoSolicitud.REALIZADA,
                solicitudes.buscarPorId(solicitud.getId())
                        .orElseThrow().getEstado());
    }

    @Test
    void marcarRealizadaCitaSinSolicitudVinculada()
            throws Exception {
        Cita cita = guardarCita(
                MEDICO_ID, EstadoCita.PROGRAMADA, null);

        assertEquals(
                EstadoCita.REALIZADA,
                servicio.marcarRealizada(MEDICO, cita.getId())
                        .getEstado());
    }

    @Test
    void rechazarEstadosNoPermitidosSinCambiarLaCita() {
        for (EstadoCita estado : List.of(
                EstadoCita.CANCELADA,
                EstadoCita.REALIZADA)) {
            Cita cita = guardarCita(MEDICO_ID, estado, null);

            assertThrows(
                    CitaNoRealizableException.class,
                    () -> servicio.marcarRealizada(
                            MEDICO, cita.getId()));
            assertEquals(
                    estado,
                    citas.buscarPorId(cita.getId())
                            .orElseThrow().getEstado());
        }
    }

    @Test
    void solicitudVinculadaInexistenteNoModificaLaCita() {
        Cita cita = guardarCita(
                MEDICO_ID, EstadoCita.PROGRAMADA, 999L);

        assertThrows(
                SolicitudNoEncontradaException.class,
                () -> servicio.marcarRealizada(
                        MEDICO, cita.getId()));
        assertEquals(
                EstadoCita.PROGRAMADA,
                citas.buscarPorId(cita.getId())
                        .orElseThrow().getEstado());
    }

    @Test
    void solicitudNoCitadaNoModificaNingunaEntidad() {
        SolicitudPrueba solicitud =
                guardarSolicitud(EstadoSolicitud.ANULADA);
        Cita cita = guardarCita(
                MEDICO_ID,
                EstadoCita.PROGRAMADA,
                solicitud.getId());

        assertThrows(
                CitaNoRealizableException.class,
                () -> servicio.marcarRealizada(
                        MEDICO, cita.getId()));
        assertEquals(
                EstadoCita.PROGRAMADA,
                citas.buscarPorId(cita.getId())
                        .orElseThrow().getEstado());
        assertEquals(
                EstadoSolicitud.ANULADA,
                solicitudes.buscarPorId(solicitud.getId())
                        .orElseThrow().getEstado());
    }

    @Test
    void soloElMedicoResponsablePuedeMarcarLaCita()
            throws Exception {
        Cita cita = guardarCita(
                MEDICO_ID, EstadoCita.PROGRAMADA, null);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.marcarRealizada(
                        new Usuario("otro", Rol.MEDICO, 31L),
                        cita.getId()));
        assertThrows(
                SinPermisoException.class,
                () -> servicio.marcarRealizada(
                        new Usuario("paciente", Rol.PACIENTE),
                        cita.getId()));
    }
}

package cita;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import agenda.AgendaNoEncontradaException;
import agenda.HuecoAgendaNoEncontradoException;
import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasServiceImpl
        implements CitasService {

    private final CitasRepositorio repositorio;
    private final IdentidadPaciente identidadPaciente;
    private final CitasAdministracionServiceImpl administracion;
    private final PoliticaCitaRepositorio politicas;
    private final Clock reloj;

    public CitasServiceImpl(
            CitasRepositorio repositorio,
            IdentidadPaciente identidadPaciente,
            CitasAdministracionServiceImpl administracion,
            PoliticaCitaRepositorio politicas) {

        this(repositorio, identidadPaciente, administracion,
                politicas, Clock.systemDefaultZone());
    }

    public CitasServiceImpl(
            CitasRepositorio repositorio,
            IdentidadPaciente identidadPaciente,
            CitasAdministracionServiceImpl administracion,
            PoliticaCitaRepositorio politicas,
            Clock reloj) {

        this.repositorio = repositorio;
        this.identidadPaciente = identidadPaciente;
        this.administracion = administracion;
        this.politicas = politicas;
        this.reloj = reloj;
    }

    @Override
    public List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PACIENTE);

        long pacienteId =
                identidadPaciente
                        .obtenerPacienteId(
                                solicitante);

        return repositorio
                .listarPorPaciente(
                        pacienteId);
    }

    @Override
    public Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PACIENTE);

        long pacienteId =
                identidadPaciente
                        .obtenerPacienteId(
                                solicitante);

        Cita cita =
                repositorio.buscarPorId(citaId)
                        .orElseThrow(() ->
                                new CitaNoEncontradaException(
                                        "No existe la cita con id "
                                        + citaId));

        if (cita.getPacienteId()
                != pacienteId) {

            /*
             * Se devuelve el mismo error que para una cita
             * inexistente para no revelar información sobre
             * citas pertenecientes a otros pacientes.
             */
            throw new CitaNoEncontradaException(
                    "No existe la cita con id "
                    + citaId);
        }

        return cita;
    }

     //CU02: Confirma una cita del propio paciente.
    @Override
    public synchronized Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException {

        //Comprobar el usuario y que la cita sea suya
        Cita actual = consultarMiCita(solicitante, citaId);

        //solo se pueden confirmar citas programadas.
        if (actual.getEstado() != EstadoCita.PROGRAMADA) {
            throw new CitaNoConfirmableException(
                    "La cita " + citaId
                    + " no se puede confirmar porque está "
                    + actual.getEstado());
        }

        //crear una copia para modificar su estado.
        Cita confirmada = actual.copia();
        confirmada.setEstado(EstadoCita.CONFIRMADA);

        //Guardar el cambio y devolver una copia.
        return repositorio.guardar(confirmada).copia();
    }

    //CU04: Cancela una cita del propio paciente (D-038).
    @Override
    public synchronized Cita cancelarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            CancelacionFueraDePlazoException {

        //Comprobar el usuario y que la cita sea suya
        Cita actual = consultarMiCita(solicitante, citaId);

        //El plazo solo importa si la cita se puede cancelar;
        //si no, cancelarCita informa del estado.
        if (actual.getEstado() == EstadoCita.PROGRAMADA
                || actual.getEstado() == EstadoCita.CONFIRMADA) {
            exigirPlazo(actual);
        }

        //Misma cancelación que CU25: cita CANCELADA, hueco libre
        //y solicitud de nuevo pendiente de citación.
        return administracion.cancelarCita(citaId);
    }

    //CU03: huecos libres que cumplen la política de citación (D-039).
    @Override
    public List<HuecoLibre> listarHuecosLibres(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PACIENTE);

        identidadPaciente.obtenerPacienteId(solicitante);

        PoliticaCita politica = politicas.consultar();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        return administracion.huecosLibres().stream()
                .filter(hueco -> !hueco.fechaHora().isBefore(
                        ahora.plusHours(
                                politica.getAntelacionMinimaHoras()))
                        && !hueco.fechaHora().isAfter(
                        ahora.plusDays(
                                politica.getAntelacionMaximaDias())))
                .toList();
    }

    //CU03: Cambia una cita del propio paciente a otro hueco libre (D-039).
    @Override
    public synchronized Cita cambiarMiCita(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException,
            CambioFueraDePoliticaException {

        //Comprobar el usuario y que la cita sea suya
        Cita actual = consultarMiCita(solicitante, citaId);

        //Si la cita está cerrada, reprogramarCita informa del estado.
        if (actual.getEstado() == EstadoCita.PROGRAMADA
                || actual.getEstado() == EstadoCita.CONFIRMADA) {

            LocalDateTime nuevaFecha =
                    administracion.fechaHoraDeHuecoLibre(
                            nuevoHuecoId);

            exigirPoliticaDeCambio(actual, nuevaFecha);
        }

        //Misma reprogramación que CU25: hueco nuevo ocupado,
        //hueco anterior libre y cita de nuevo PROGRAMADA.
        return administracion.reprogramarCita(
                citaId, nuevoHuecoId);
    }

    //D-034: el cambio respeta el plazo de cancelación de la cita actual
    //y la antelación mínima y la ventana máxima del hueco nuevo.
    private void exigirPoliticaDeCambio(
            Cita actual,
            LocalDateTime nuevaFecha)
            throws CambioFueraDePoliticaException {

        PoliticaCita politica = politicas.consultar();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        int horasCancelacion =
                politica.getCancelacionMinimaHoras();

        if (actual.getFechaHora() != null
                && ahora.isAfter(actual.getFechaHora()
                        .minusHours(horasCancelacion))) {
            throw new CambioFueraDePoliticaException(
                    "La cita solo se puede cambiar con al menos "
                    + horasCancelacion + " horas de antelación");
        }

        int horasMinimas = politica.getAntelacionMinimaHoras();

        if (nuevaFecha.isBefore(ahora.plusHours(horasMinimas))) {
            throw new CambioFueraDePoliticaException(
                    "El nuevo hueco debe empezar con al menos "
                    + horasMinimas + " horas de antelación");
        }

        int diasMaximos = politica.getAntelacionMaximaDias();

        if (nuevaFecha.isAfter(ahora.plusDays(diasMaximos))) {
            throw new CambioFueraDePoliticaException(
                    "El nuevo hueco no puede estar a más de "
                    + diasMaximos + " días");
        }
    }

    //D-034: la cancelación exige una antelación mínima en horas.
    private void exigirPlazo(Cita cita)
            throws CancelacionFueraDePlazoException {

        if (cita.getFechaHora() == null) {
            return;
        }

        int horas = politicas.consultar()
                .getCancelacionMinimaHoras();

        LocalDateTime limite =
                cita.getFechaHora().minusHours(horas);

        if (LocalDateTime.now(reloj).isAfter(limite)) {
            throw new CancelacionFueraDePlazoException(
                    "La cita solo se puede cancelar con al menos "
                    + horas + " horas de antelación");
        }
    }

}
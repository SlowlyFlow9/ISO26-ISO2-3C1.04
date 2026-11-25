package cita;

import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasServiceImpl
        implements CitasService {

    private final CitasRepositorio repositorio;
    private final IdentidadPaciente identidadPaciente;

    public CitasServiceImpl(
            CitasRepositorio repositorio,
            IdentidadPaciente identidadPaciente) {

        this.repositorio = repositorio;
        this.identidadPaciente = identidadPaciente;
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

}
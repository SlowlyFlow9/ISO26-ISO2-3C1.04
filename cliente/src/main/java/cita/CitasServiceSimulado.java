package cita;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasServiceSimulado
        implements CitasService {

    private final List<Cita> citas =
            new ArrayList<>();

    private final Map<String, Long> pacientesPorUsuario =
            new HashMap<>();

    private long siguienteId = 1;

    public void vincularPaciente(
            String nombreUsuario,
            long pacienteId) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()
                || pacienteId <= 0) {

            throw new IllegalArgumentException(
                    "La vinculación de paciente no es válida");
        }

        pacientesPorUsuario.put(
                nombreUsuario,
                pacienteId);
    }

    public Cita agregarCita(Cita datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "La cita es obligatoria");
        }

        Cita almacenada = datos.copia();

        if (almacenada.getId() == null) {
            almacenada.setId(siguienteId++);
        }

        citas.add(almacenada);

        return almacenada.copia();
    }

    @Override
    public List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        exigirPaciente(solicitante);

        long pacienteId =
                obtenerPacienteId(solicitante);

        return citas.stream()
                .filter(cita ->
                        cita.getPacienteId()
                                == pacienteId)
                .map(Cita::copia)
                .toList();
    }

    @Override
    public Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException {

        exigirPaciente(solicitante);

        long pacienteId =
                obtenerPacienteId(solicitante);

        for (Cita cita : citas) {

            if (cita.getId() == citaId
                    && cita.getPacienteId()
                            == pacienteId) {

                return cita.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id "
                        + citaId);
    }

    private long obtenerPacienteId(
            Usuario usuario)
            throws PacienteNoVinculadoException {

        Long pacienteId =
                pacientesPorUsuario.get(
                        usuario.nombre());

        if (pacienteId == null) {
            throw new PacienteNoVinculadoException(
                    "El usuario no está vinculado "
                            + "a ningún paciente");
        }

        return pacienteId;
    }

    private static void exigirPaciente(
            Usuario usuario)
            throws SinPermisoException {

        if (usuario == null
                || usuario.rol() != Rol.PACIENTE) {

            throw new SinPermisoException(
                    "Esta operación solo está permitida "
                            + "al rol PACIENTE");
        }
    }

    //confirma una cita propia y actualiza el estado almacenado en el servicio simulado
    @Override
    public synchronized Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException {

        // Reutilizamos las comprobaciones de CU01: rol, vinculación y propiedad de la cita.
        Cita actual = consultarMiCita(
                solicitante, citaId);

        if (actual.getEstado() != EstadoCita.PROGRAMADA) {
            throw new CitaNoConfirmableException(
                    "La cita " + citaId
                    + " no se puede confirmar porque está "
                    + actual.getEstado());
        }

        // Actualizamos la cita guardada, no solo la copia obtenida durante la consulta.
        for (Cita almacenada : citas) {

            if (almacenada.getId() != null
                    && almacenada.getId() == citaId) {

                almacenada.setEstado(
                        EstadoCita.CONFIRMADA);

                return almacenada.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id " + citaId);
    }

}

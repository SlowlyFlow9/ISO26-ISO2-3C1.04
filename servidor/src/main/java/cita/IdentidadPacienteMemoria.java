package cita;

import java.util.HashMap;
import java.util.Map;

import paciente.Usuario;

public class IdentidadPacienteMemoria
        implements IdentidadPaciente {

    private final Map<String, Long> pacientesPorUsuario =
            new HashMap<>();

    public void vincular(
            String nombreUsuario,
            long pacienteId) {

        pacientesPorUsuario.put(
                nombreUsuario,
                pacienteId);
    }

    @Override
    public long obtenerPacienteId(
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
}
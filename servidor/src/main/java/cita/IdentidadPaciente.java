package cita;

import paciente.Usuario;

public interface IdentidadPaciente {

    long obtenerPacienteId(
            Usuario usuario)
            throws PacienteNoVinculadoException;
}
package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06 - Consultar historial de pruebas realizadas.
 */

public interface HistorialPruebasService {

    List<Cita> listarMiHistorial(Usuario solicitante)
            throws SinPermisoException,
                   PacienteNoVinculadoException;
}

package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06: Contrato para consultar el historial de pruebas.
 */

public interface HistorialPruebasService {

    List<Cita> listarMiHistorial(Usuario solicitante)
            throws SinPermisoException,
                   PacienteNoVinculadoException;
}

package cita;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface PoliticaCitaService {

    PoliticaCita consultar(Usuario solicitante)
            throws SinPermisoException;

    PoliticaCita modificar(
            Usuario solicitante,
            PoliticaCita politica)
            throws SinPermisoException;
}

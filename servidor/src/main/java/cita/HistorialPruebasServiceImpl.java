package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06: implementacion de la consulta del historial
 */

public class HistorialPruebasServiceImpl
        implements HistorialPruebasService {

    private final CitasService citasService;

    public HistorialPruebasServiceImpl(
            CitasService citasService) {

        this.citasService = citasService;
    }

    @Override
    public List<Cita> listarMiHistorial(
            Usuario solicitante)
            throws SinPermisoException,
                   PacienteNoVinculadoException {

        // CU01 ya comprueba los permisos y la pertenencia de las
        //citas al paciente
        List<Cita> citas =
                citasService.listarMisCitas(solicitante);

        //CU06 devuelve solo las pruebas realizadas
        return citas.stream()
                .filter(cita ->
                        cita.getEstado() == EstadoCita.REALIZADA)
                .map(Cita::copia)
                .toList();
    }
}

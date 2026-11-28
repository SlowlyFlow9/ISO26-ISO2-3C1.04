package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06: Servicio simulado para pruebas del cliente.
 */
public class HistorialPruebasServiceSimulado
        implements HistorialPruebasService {

    private final CitasService citas;

    public HistorialPruebasServiceSimulado(
            CitasService citas) {

        if (citas == null) {
            throw new IllegalArgumentException(
                    "El servicio de citas es obligatorio");
        }

        this.citas = citas;
    }

    @Override
    public List<Cita> listarMiHistorial(
            Usuario solicitante)
            throws SinPermisoException,
                   PacienteNoVinculadoException {

        return citas.listarMisCitas(solicitante)
                .stream()
                .filter(cita ->
                        cita.getEstado() == EstadoCita.REALIZADA)
                .map(Cita::copia)
                .toList();
    }
}

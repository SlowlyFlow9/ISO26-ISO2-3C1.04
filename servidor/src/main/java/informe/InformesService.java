package informe;

import java.util.List;

public class InformesService {
    private final InformesRepositorio repositorio;

    public InformesService(InformesRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void crearInforme(Usuario solicitante, Informe datos) {
        // D-023: Validación de permisos
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden crear informes");
        }

        // D-013: Validaciones de negocio en el servidor
        if (datos.getTexto() == null || datos.getTexto().trim().isEmpty()) {
            throw new IllegalArgumentException("El texto del informe no puede estar vacío");
        }

        if (datos.getIdPaciente() == null || datos.getIdPaciente().trim().isEmpty()) {
            throw new IllegalArgumentException("El informe debe estar asociado a un paciente");
        }

        repositorio.guardar(datos);
    }

    public List<Informe> consultarInformes(Usuario solicitante, String idPaciente) {
        // D-023: Validación de permisos
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden consultar informes");
        }
        
        if (idPaciente == null || idPaciente.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede estar vacío");
        }

        return repositorio.obtenerPorPaciente(idPaciente);
    }
}
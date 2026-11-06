package informe;

// IMPORTANTE: Cuando pegues esto, te saldrá un error en 'Usuario'. 
// Usa VS Code (Ctrl + . o clic en la bombilla) para importar la clase Usuario 
// y las excepciones (SinPermisoException, DatosInvalidosException) desde los paquetes de tu equipo.

public class InformesService {
    private final InformesRepositorio repositorio;

    public InformesService(InformesRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void crearInforme(Usuario solicitante, Informe datos) {
        // D-023: Validación de permisos
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden crear informes"); 
            // Cambia RuntimeException por SinPermisoException cuando la importes
        }
        
        // D-013: Validaciones de negocio en el servidor
        if (datos.getTexto() == null || datos.getTexto().trim().isEmpty()) {
            throw new IllegalArgumentException("El texto del informe no puede estar vacío");
            // Cambia IllegalArgumentException por DatosInvalidosException
        }
        
        if (datos.getIdPaciente() == null || datos.getIdPaciente().trim().isEmpty()) {
            throw new IllegalArgumentException("El informe debe estar asociado a un paciente");
        }

        repositorio.guardar(datos);
    }
}
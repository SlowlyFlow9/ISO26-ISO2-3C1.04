package prueba;

import informe.Usuario;

public class PruebasService {
    private final PruebasRepositorio repositorio;

    public PruebasService(PruebasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void marcarComoRealizada(Usuario solicitante, String idPrueba) {
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden modificar el estado de las pruebas");
        }

        if (idPrueba == null || idPrueba.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la prueba es obligatorio");
        }

        PruebaMedica prueba = repositorio.obtenerPorId(idPrueba);
        if (prueba == null) {
            throw new IllegalArgumentException("No existe ninguna prueba con ese ID");
        }

        prueba.setEstado("REALIZADA");
    }
}
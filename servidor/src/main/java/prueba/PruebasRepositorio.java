package prueba;

public interface PruebasRepositorio {
    PruebaMedica obtenerPorId(String id);
    void guardar(PruebaMedica prueba);
}
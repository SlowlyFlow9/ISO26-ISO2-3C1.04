package prueba;
import java.util.List;

public interface PruebasRepositorio {
    PruebaMedica obtenerPorId(String id);
    void guardar(PruebaMedica prueba);
    List<PruebaMedica> obtenerTodas(); 
}
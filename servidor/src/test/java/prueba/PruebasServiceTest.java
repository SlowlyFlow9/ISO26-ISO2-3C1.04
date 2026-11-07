package prueba;

import informe.Usuario;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PruebasServiceTest {

    @Test
    public void testMedicoPuedeMarcarPruebaRealizada() {
        PruebasRepositorioMemoria repo = new PruebasRepositorioMemoria();
        PruebasService service = new PruebasService(repo);
        
        Usuario medico = new Usuario();
        medico.setRol("Médico");
        
        // Preparamos una prueba pendiente
        repo.guardar(new PruebaMedica("PRUEBA-001", "PENDIENTE"));
        
        assertDoesNotThrow(() -> service.marcarComoRealizada(medico, "PRUEBA-001"));
        
        // Verificamos que el estado ha cambiado
        PruebaMedica actualizada = repo.obtenerPorId("PRUEBA-001");
        assertEquals("REALIZADA", actualizada.getEstado());
    }

    @Test
    public void testNoMedicoNoPuedeMarcarPrueba() {
        PruebasRepositorioMemoria repo = new PruebasRepositorioMemoria();
        PruebasService service = new PruebasService(repo);
        
        Usuario enfermero = new Usuario();
        enfermero.setRol("Enfermero"); // Simulamos otro rol
        
        repo.guardar(new PruebaMedica("PRUEBA-002", "PENDIENTE"));
        
        assertThrows(RuntimeException.class, () -> service.marcarComoRealizada(enfermero, "PRUEBA-002"));
    }
}
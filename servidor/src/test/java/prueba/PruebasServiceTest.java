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
        
        repo.guardar(new PruebaMedica("PRUEBA-001", "PENDIENTE"));
        assertDoesNotThrow(() -> service.marcarComoRealizada(medico, "PRUEBA-001"));
        
        PruebaMedica actualizada = repo.obtenerPorId("PRUEBA-001");
        assertEquals("REALIZADA", actualizada.getEstado());
    }

    @Test
    public void testNoMedicoNoPuedeMarcarPrueba() {
        PruebasRepositorioMemoria repo = new PruebasRepositorioMemoria();
        PruebasService service = new PruebasService(repo);
        Usuario enfermero = new Usuario();
        enfermero.setRol("Enfermero");
        
        repo.guardar(new PruebaMedica("PRUEBA-002", "PENDIENTE"));
        assertThrows(RuntimeException.class, () -> service.marcarComoRealizada(enfermero, "PRUEBA-002"));
    }

    // Nuevos tests para el CU20
    @Test
    public void testMedicoPuedeRegistrarIncidencia() {
        PruebasRepositorioMemoria repo = new PruebasRepositorioMemoria();
        PruebasService service = new PruebasService(repo);
        Usuario medico = new Usuario();
        medico.setRol("Médico");
        
        repo.guardar(new PruebaMedica("PRUEBA-003", "REALIZADA"));
        assertDoesNotThrow(() -> service.registrarIncidencia(medico, "PRUEBA-003", "El paciente se movió durante la placa"));
        
        PruebaMedica actualizada = repo.obtenerPorId("PRUEBA-003");
        assertEquals("El paciente se movió durante la placa", actualizada.getIncidencia());
    }

    @Test
    public void testNoMedicoNoPuedeRegistrarIncidencia() {
        PruebasRepositorioMemoria repo = new PruebasRepositorioMemoria();
        PruebasService service = new PruebasService(repo);
        Usuario celador = new Usuario();
        celador.setRol("Celador");
        
        repo.guardar(new PruebaMedica("PRUEBA-004", "REALIZADA"));
        assertThrows(RuntimeException.class, () -> service.registrarIncidencia(celador, "PRUEBA-004", "Fallo técnico"));
    }
}
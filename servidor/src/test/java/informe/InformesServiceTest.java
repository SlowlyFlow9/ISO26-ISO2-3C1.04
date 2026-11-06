package informe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

public class InformesServiceTest {

    @Test
    public void testMedicoPuedeCrearInforme() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);
        
        // Ajusta la creación del usuario según los constructores o setters de tu proyecto
        Usuario medico = new Usuario(); 
        medico.setRol("Médico");
        
        Informe informe = new Informe("1", "PAC-123", "Paciente estable", LocalDate.now());
        
        // Verifica que no lanza excepción y se guarda correctamente
        assertDoesNotThrow(() -> service.crearInforme(medico, informe));
        assertEquals(1, repo.getInformes().size());
    }

    @Test
    public void testNoMedicoNoPuedeCrearInforme() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);
        
        Usuario admin = new Usuario(); 
        admin.setRol("Personal administrativo");
        
        Informe informe = new Informe("1", "PAC-123", "Paciente estable", LocalDate.now());
        
        // Verifica que salta la excepción de seguridad (D-023)
        assertThrows(RuntimeException.class, () -> service.crearInforme(admin, informe));
    }
}
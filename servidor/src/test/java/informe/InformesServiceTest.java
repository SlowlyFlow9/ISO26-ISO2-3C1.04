package informe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

public class InformesServiceTest {

    @Test
    public void testMedicoPuedeCrearInforme() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);

        Usuario medico = new Usuario();
        medico.setRol("Médico");

        Informe informe = new Informe("1", "PAC-123", "Prueba", LocalDate.now());

        assertDoesNotThrow(() -> service.crearInforme(medico, informe));
        assertEquals(1, repo.getInformes().size());
    }

    @Test
    public void testNoMedicoNoPuedeCrearInforme() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);

        Usuario admin = new Usuario();
        admin.setRol("Personal administrativo");

        Informe informe = new Informe("1", "PAC-123", "Prueba", LocalDate.now());

        assertThrows(RuntimeException.class, () -> service.crearInforme(admin, informe));
    }

    @Test
    public void testMedicoPuedeConsultarInformes() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);
        
        Usuario medico = new Usuario(); 
        medico.setRol("Médico");
        
        // Metemos un informe previo
        repo.guardar(new Informe("1", "PAC-123", "Texto de prueba", LocalDate.now()));
        
        // Comprobamos que el médico puede recuperarlo
        List<Informe> resultado = service.consultarInformes(medico, "PAC-123");
        assertEquals(1, resultado.size());
        assertEquals("Texto de prueba", resultado.get(0).getTexto());
    }

    @Test
    public void testNoMedicoNoPuedeConsultarInformes() {
        InformesRepositorioMemoria repo = new InformesRepositorioMemoria();
        InformesService service = new InformesService(repo);
        
        Usuario admin = new Usuario(); 
        admin.setRol("Personal administrativo");
        
        // Comprobamos que bloquea el acceso
        assertThrows(RuntimeException.class, () -> service.consultarInformes(admin, "PAC-123"));
    }
}
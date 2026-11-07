package imagen;

import informe.Usuario;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class ImagenesServiceTest {

    @Test
    public void testMedicoPuedeConsultarImagenes() {
        ImagenesRepositorioMemoria repo = new ImagenesRepositorioMemoria();
        ImagenesService service = new ImagenesService(repo);
        
        Usuario medico = new Usuario();
        medico.setRol("Médico");
        
        // Inyectamos una imagen de prueba directamente al repositorio
        repo.guardar(new ImagenPrueba("1", "PRUEBA-123", "/imagenes/placa_torax.png"));
        
        List<ImagenPrueba> resultado = service.consultarImagenes(medico, "PRUEBA-123");
        assertEquals(1, resultado.size());
        assertEquals("/imagenes/placa_torax.png", resultado.get(0).getRuta());
    }

    @Test
    public void testNoMedicoNoPuedeConsultarImagenes() {
        ImagenesRepositorioMemoria repo = new ImagenesRepositorioMemoria();
        ImagenesService service = new ImagenesService(repo);
        
        Usuario admin = new Usuario();
        admin.setRol("Personal administrativo");
        
        assertThrows(RuntimeException.class, () -> service.consultarImagenes(admin, "PRUEBA-123"));
    }
}
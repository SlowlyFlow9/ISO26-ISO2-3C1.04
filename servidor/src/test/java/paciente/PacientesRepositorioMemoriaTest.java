package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacientesRepositorioMemoriaTest {
    private PacientesRepositorioMemoria repositorio;

    @BeforeEach
    void setUp() { repositorio = new PacientesRepositorioMemoria(); }

    private Paciente paciente(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setDni(dni);
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    @Test
    @DisplayName("Guardar asigna ids consecutivos")
    void guardarAsignaIdsConsecutivos() {
        // Preparar
        Paciente primero = paciente("1A", null);
        Paciente segundo = paciente("2B", null);
        // Ejecutar
        Paciente g1 = repositorio.guardar(primero);
        Paciente g2 = repositorio.guardar(segundo);
        // Comprobar
        assertEquals(1L, g1.getId());
        assertEquals(2L, g2.getId());
    }

    @Test
    @DisplayName("Guardar un paciente con id existente lo sustituye")
    void guardarConIdExistenteSustituye() {
        // Preparar
        Paciente p = repositorio.guardar(paciente("1A", null));
        p.setNombre("Nuevo");
        // Ejecutar
        repositorio.guardar(p);
        // Comprobar
        assertEquals("Nuevo", repositorio.buscarPorId(1L).orElseThrow().getNombre());
    }

    @Test
    @DisplayName("Se busca por DNI sin distinguir mayúsculas")
    void buscaPorDniSinDistinguirMayusculas() {
        // Preparar
        repositorio.guardar(paciente("1A", "Ta"));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorDni("1a").isPresent());
    }

    @Test
    @DisplayName("Se busca por tarjeta sin distinguir mayúsculas")
    void buscaPorTarjetaSinDistinguirMayusculas() {
        // Preparar
        repositorio.guardar(paciente("1A", "Ta"));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorTarjeta("tA").isPresent());
    }

    @Test
    @DisplayName("Un DNI inexistente no se encuentra")
    void dniInexistenteNoSeEncuentra() {
        // Preparar
        repositorio.guardar(paciente("1A", "Ta"));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorDni("9Z").isEmpty());
    }

    @Test
    @DisplayName("Una tarjeta inexistente no se encuentra")
    void tarjetaInexistenteNoSeEncuentra() {
        // Preparar
        repositorio.guardar(paciente("1A", "Ta"));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorTarjeta("otra").isEmpty());
    }

    @Test
    @DisplayName("Un id inexistente no se encuentra")
    void idInexistenteNoSeEncuentra() {
        // Preparar
        repositorio.guardar(paciente("1A", "Ta"));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorId(5L).isEmpty());
    }

    @Test
    @DisplayName("Un paciente sin tarjeta no coincide con ninguna búsqueda por tarjeta")
    void sinTarjetaNoCoincideConNingunaBusqueda() {
        // Preparar
        repositorio.guardar(paciente("1A", null));
        // Ejecutar y comprobar
        assertTrue(repositorio.buscarPorTarjeta("x").isEmpty());
    }
}

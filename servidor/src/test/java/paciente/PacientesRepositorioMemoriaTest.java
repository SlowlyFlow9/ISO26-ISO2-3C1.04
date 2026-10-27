package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PacientesRepositorioMemoriaTest {

    private Paciente paciente(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setDni(dni);
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    @Test
    void guardarAsignaIdsConsecutivos() {
        PacientesRepositorioMemoria r = new PacientesRepositorioMemoria();
        assertEquals(1L, r.guardar(paciente("1A", null)).getId());
        assertEquals(2L, r.guardar(paciente("2B", null)).getId());
    }

    @Test
    void guardarConIdExistenteSustituye() {
        PacientesRepositorioMemoria r = new PacientesRepositorioMemoria();
        Paciente p = r.guardar(paciente("1A", null));
        p.setNombre("Nuevo");
        r.guardar(p);
        assertEquals("Nuevo", r.buscarPorId(1L).orElseThrow().getNombre());
    }

    @Test
    void buscaPorDniYTarjetaSinDistinguirMayusculas() {
        PacientesRepositorioMemoria r = new PacientesRepositorioMemoria();
        r.guardar(paciente("1A", "Ta"));
        assertTrue(r.buscarPorDni("1a").isPresent());
        assertTrue(r.buscarPorTarjeta("tA").isPresent());
        assertTrue(r.buscarPorDni("9Z").isEmpty());
        assertTrue(r.buscarPorTarjeta("otra").isEmpty());
        assertTrue(r.buscarPorId(5L).isEmpty());
    }

    @Test
    void sinTarjetaNoCoincideConNingunaBusqueda() {
        PacientesRepositorioMemoria r = new PacientesRepositorioMemoria();
        r.guardar(paciente("1A", null));
        assertTrue(r.buscarPorTarjeta("x").isEmpty());
    }
}

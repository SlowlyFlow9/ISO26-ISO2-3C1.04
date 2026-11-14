package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacientesServiceSimuladoTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceSimulado servicio;

    @BeforeEach
    void setUp() { servicio = new PacientesServiceSimulado(); }

    private Paciente paciente(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    @Test
    @DisplayName("El alta asigna id, fecha de alta y estado activo")
    void altaAsignaIdFechaAltaYActivo() throws Exception {
        // Preparar
        Paciente entrada = paciente("12345678Z", "T1");
        entrada.setActivo(false);
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, entrada);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r.getId()),
                () -> assertNotNull(r.getFechaAlta()),
                () -> assertTrue(r.isActivo()));
    }

    @Test
    @DisplayName("Un DNI repetido, sin distinguir mayúsculas, lanza duplicado")
    void dniRepetidoLanzaDuplicado() throws Exception {
        // Preparar
        servicio.crear(ADMIN, paciente("12345678Z", "T1"));
        Paciente repetido = paciente("12345678z", "T2");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.crear(ADMIN, repetido));
    }

    @Test
    @DisplayName("Una tarjeta sanitaria repetida lanza duplicado")
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        // Preparar
        servicio.crear(ADMIN, paciente("12345678Z", "T1"));
        Paciente repetido = paciente("87654321X", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.crear(ADMIN, repetido));
    }

    @Test
    @DisplayName("Varios pacientes sin tarjeta (nula o vacía) no son duplicados")
    void variosSinTarjetaNoSonDuplicados() throws Exception {
        // Preparar
        Paciente sinTarjeta = paciente("12345678Z", null);
        Paciente tarjetaVacia = paciente("87654321X", "");
        // Ejecutar
        Paciente r1 = servicio.crear(ADMIN, sinTarjeta);
        Paciente r2 = servicio.crear(ADMIN, tarjetaVacia);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r1.getId()),
                () -> assertEquals(2L, r2.getId()));
    }

    @Test
    @DisplayName("Los datos inválidos lanzan la excepción con la lista de errores")
    void datosInvalidosLanzanLaExcepcionConLosErrores() {
        // Preparar
        Paciente p = paciente("123", "T1");
        // Ejecutar
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
        // Comprobar
        assertEquals(1, e.getErrores().size());
    }

    @Test
    @DisplayName("Una fecha de nacimiento futura es inválida")
    void fechaNacimientoFuturaEsInvalida() {
        // Preparar
        Paciente p = paciente("12345678Z", "T1");
        p.setFechaNacimiento(LocalDate.now().plusDays(1));
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    @Test
    @DisplayName("El rol PACIENTE no puede dar de alta")
    void pacienteNoPuedeDarDeAlta() { assertSinPermiso(Rol.PACIENTE); }

    @Test
    @DisplayName("El rol MEDICO no puede dar de alta")
    void medicoNoPuedeDarDeAlta() { assertSinPermiso(Rol.MEDICO); }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede dar de alta")
    void administradorNoPuedeDarDeAlta() { assertSinPermiso(Rol.ADMINISTRADOR); }

    private void assertSinPermiso(Rol rol) {
        // Preparar
        Usuario usuario = new Usuario("x", rol);
        Paciente p = paciente("12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.crear(usuario, p));
    }
}

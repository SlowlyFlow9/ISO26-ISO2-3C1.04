package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PantallasPacienteHelpersCoberturaTest {

    @Test
    @DisplayName(
            "El combo de alta genera valores ascendentes")
    void comboAltaAscendente()
            throws Exception {

        JComboBox<?> combo =
                (JComboBox<?>) invocar(
                        PantallaAltaPaciente.class,
                        "combo",
                        new Class<?>[] {
                                String.class,
                                int.class,
                                int.class
                        },
                        "Mes",
                        1,
                        3);

        assertEquals(4, combo.getItemCount());
        assertEquals("Mes", combo.getItemAt(0));
        assertEquals("1", combo.getItemAt(1));
        assertEquals("3", combo.getItemAt(3));
    }

    @Test
    @DisplayName(
            "El combo de alta genera valores descendentes")
    void comboAltaDescendente()
            throws Exception {

        JComboBox<?> combo =
                (JComboBox<?>) invocar(
                        PantallaAltaPaciente.class,
                        "combo",
                        new Class<?>[] {
                                String.class,
                                int.class,
                                int.class
                        },
                        "Año",
                        3,
                        1);

        assertEquals(4, combo.getItemCount());
        assertEquals("3", combo.getItemAt(1));
        assertEquals("1", combo.getItemAt(3));
    }

    @Test
    @DisplayName(
            "Campo de alta añade etiqueta y texto")
    void campoAlta()
            throws Exception {

        JPanel panel = new JPanel();
        JTextField texto = new JTextField();

        invocar(
                PantallaAltaPaciente.class,
                "campo",
                new Class<?>[] {
                        JPanel.class,
                        String.class,
                        JTextField.class
                },
                panel,
                "Nombre",
                texto);

        assertEquals(2, panel.getComponentCount());
    }

    @Test
    @DisplayName(
            "El resumen de alta muestra todos los datos")
    void resumenAlta()
            throws Exception {

        Paciente p = paciente();

        String resumen =
                (String) invocar(
                        PantallaAltaPaciente.class,
                        "resumen",
                        new Class<?>[] {
                                Paciente.class
                        },
                        p);

        assertTrue(resumen.contains("Ana Pérez"));
        assertTrue(resumen.contains("12345678Z"));
        assertTrue(resumen.contains("600111222"));
        assertTrue(resumen.contains("2026-11-16"));
    }

    @Test
    @DisplayName(
            "Los valores opcionales vacíos se representan con guion")
    void valorAlta()
            throws Exception {

        assertEquals(
                "-",
                invocar(
                        PantallaAltaPaciente.class,
                        "valor",
                        new Class<?>[] { String.class },
                        (Object) null));

        assertEquals(
                "-",
                invocar(
                        PantallaAltaPaciente.class,
                        "valor",
                        new Class<?>[] { String.class },
                        "  "));

        assertEquals(
                "correo@test.es",
                invocar(
                        PantallaAltaPaciente.class,
                        "valor",
                        new Class<?>[] { String.class },
                        "correo@test.es"));
    }

    @Test
    @DisplayName(
            "La pantalla de alta está disponible solo para personal administrativo")
    void disponibilidadAlta() {

        assertTrue(
                PantallaAltaPaciente.disponiblePara(
                        new Usuario(
                                "personal",
                                Rol.PERSONAL_ADMINISTRATIVO)));

        assertFalse(
                PantallaAltaPaciente.disponiblePara(
                        new Usuario(
                                "medico",
                                Rol.MEDICO)));
    }

    @Test
    @DisplayName(
            "Texto de lista de modificación muestra nombre y DNI")
    void textoListaModificar()
            throws Exception {

        Paciente p = paciente();

        String texto =
                (String) invocar(
                        PantallaModificarPaciente.class,
                        "textoLista",
                        new Class<?>[] {
                                Paciente.class
                        },
                        p);

        assertEquals(
                "Ana Pérez  -  12345678Z",
                texto);
    }

    @Test
    @DisplayName(
            "Texto nulo se convierte en cadena vacía")
    void textoNuloModificar()
            throws Exception {

        assertEquals(
                "",
                invocar(
                        PantallaModificarPaciente.class,
                        "texto",
                        new Class<?>[] {
                                String.class
                        },
                        (Object) null));

        assertEquals(
                "abc",
                invocar(
                        PantallaModificarPaciente.class,
                        "texto",
                        new Class<?>[] {
                                String.class
                        },
                        "abc"));
    }

    @Test
    @DisplayName(
            "El resumen de modificación conserva los datos")
    void resumenModificar()
            throws Exception {

        String resumen =
                (String) invocar(
                        PantallaModificarPaciente.class,
                        "resumen",
                        new Class<?>[] {
                                Paciente.class
                        },
                        paciente());

        assertTrue(resumen.contains("Ana Pérez"));
        assertTrue(resumen.contains("12345678Z"));
    }

    @Test
    @DisplayName(
            "Se genera un paciente de ejemplo")
    void crearPacienteEjemplo()
            throws Exception {

        Paciente p =
                (Paciente) invocar(
                        PantallaModificarPaciente.class,
                        "ejemplo",
                        new Class<?>[] {
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        },
                        "Luis",
                        "García",
                        "87654321X",
                        "T2");

        assertEquals("Luis", p.getNombre());
        assertEquals("García", p.getApellidos());
        assertEquals("87654321X", p.getDni());
        assertEquals("T2", p.getNumeroTarjetaSanitaria());
        assertEquals(
                LocalDate.of(1990, 5, 1),
                p.getFechaNacimiento());
    }

    @Test
    @DisplayName(
            "Se puede crear el servicio simulado con datos")
    void servicioSimuladoConDatos()
            throws Exception {

        Object servicio =
                invocar(
                        PantallaModificarPaciente.class,
                        "simuladoConDatos",
                        new Class<?>[0]);

        assertNotNull(servicio);
        assertTrue(
                servicio instanceof PacientesService);
    }

    private Paciente paciente() {

        Paciente p = new Paciente();

        p.setId(7L);
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");

        p.setFechaNacimiento(
                LocalDate.of(1990, 5, 1));

        p.setTelefono("600111222");
        p.setEmail(null);
        p.setDireccion("");
        p.setNumeroTarjetaSanitaria("T1");

        p.setFechaAlta(
                LocalDate.of(2026, 11, 16));

        return p;
    }

    private static Object invocar(
            Class<?> clase,
            String nombre,
            Class<?>[] tipos,
            Object... argumentos)
            throws Exception {

        Method metodo =
                clase.getDeclaredMethod(
                        nombre,
                        tipos);

        metodo.setAccessible(true);

        return metodo.invoke(
                null,
                argumentos);
    }
}
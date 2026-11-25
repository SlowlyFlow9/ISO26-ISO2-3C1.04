package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaAgendasRealizacionTest {

    private static final Usuario MEDICO =
            new Usuario("Juan", Rol.MEDICO, 1L);

    private JButton buscarBoton(
            Container contenedor,
            String texto) {

        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton boton
                    && texto.equals(boton.getText())) {
                return boton;
            }
            if (componente instanceof Container hijo) {
                JButton encontrado = buscarBoton(hijo, texto);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private JTable buscarTabla(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JTable tabla) {
                return tabla;
            }
            if (componente instanceof Container hijo) {
                JTable encontrada = buscarTabla(hijo);
                if (encontrada != null) {
                    return encontrada;
                }
            }
        }
        return null;
    }

    private List<JTextField> buscarCampos(Container contenedor) {
        List<JTextField> resultado = new ArrayList<>();
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JTextField campo) {
                resultado.add(campo);
            }
            if (componente instanceof Container hijo) {
                resultado.addAll(buscarCampos(hijo));
            }
        }
        return resultado;
    }

    @Test
    void soloPermiteAccesoAMedicosIdentificados() {
        assertTrue(PantallaAgendasRealizacion.disponiblePara(MEDICO));
        assertFalse(PantallaAgendasRealizacion.disponiblePara(
                new Usuario("Juan", Rol.PACIENTE, 1L)));
        assertFalse(PantallaAgendasRealizacion.disponiblePara(
                new Usuario("Juan", Rol.MEDICO)));
        assertThrows(IllegalArgumentException.class,
                () -> new PantallaAgendasRealizacion(
                        new AgendasServiceSimulado(),
                        new Usuario("Juan", Rol.PACIENTE, 1L)));
    }

    @Test
    void muestraSoloDatosDeAgendaYAccionesDeGestion()
            throws Exception {

        AgendasServiceSimulado servicio =
                new AgendasServiceSimulado();
        AgendaMedica agenda = new AgendaMedica();
        agenda.setFecha(LocalDate.of(2026, 11, 17));
        agenda.setHoraInicio(LocalTime.of(8, 0));
        agenda.setHoraFin(LocalTime.of(15, 0));
        servicio.crearPropia(MEDICO, agenda);

        AtomicReference<PantallaAgendasRealizacion> referencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                referencia.set(new PantallaAgendasRealizacion(
                        servicio, MEDICO)));

        PantallaAgendasRealizacion pantalla = referencia.get();
        JTable tabla = buscarTabla(pantalla);

        assertNotNull(tabla);
        assertEquals(1, tabla.getRowCount());
        assertEquals(4, buscarCampos(pantalla).size());
        assertEquals("Fecha", tabla.getColumnName(1));
        assertNotNull(buscarBoton(pantalla, "Crear"));
        assertNotNull(buscarBoton(pantalla, "Modificar"));
        assertNotNull(buscarBoton(pantalla, "Eliminar"));
        assertNotNull(buscarBoton(pantalla, "Actualizar"));
    }
}

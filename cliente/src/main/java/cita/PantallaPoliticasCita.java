package cita;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class PantallaPoliticasCita extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PoliticaCitaService servicio;
    private final Usuario usuario;
    private final JTextField antelacionMinima =
            new JTextField(10);
    private final JTextField antelacionMaxima =
            new JTextField(10);
    private final JTextField cancelacionMinima =
            new JTextField(10);
    private final JTextField maxCitasActivas =
            new JTextField(10);
    private final JButton guardar = new JButton("Guardar politica");
    private final JLabel mensaje = new JLabel(" ");

    public PantallaPoliticasCita(
            PoliticaCitaService servicio,
            Usuario usuario) {
        this.servicio = servicio;
        this.usuario = usuario;
        construir();
        if (disponiblePara(usuario)) {
            cargar();
        }
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.ADMINISTRADOR;
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        mensaje.setName("mensaje");
        if (!disponiblePara(usuario)) {
            mensaje.setText(
                    "Esta configuracion solo esta disponible "
                            + "para el Administrador del Sistema");
            add(mensaje, BorderLayout.CENTER);
            return;
        }

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        campo(formulario,
                "Antelacion minima para reservar (horas)",
                "antelacionMinimaHoras",
                antelacionMinima);
        campo(formulario,
                "Antelacion maxima para reservar (dias)",
                "antelacionMaximaDias",
                antelacionMaxima);
        campo(formulario,
                "Antelacion minima para cancelar (horas)",
                "cancelacionMinimaHoras",
                cancelacionMinima);
        campo(formulario,
                "Maximo de citas activas por paciente",
                "maxCitasActivasPorPaciente",
                maxCitasActivas);
        guardar.setName("guardar");
        guardar.addActionListener(e -> guardar());
        add(formulario, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(guardar, BorderLayout.WEST);
        pie.add(mensaje, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    private static void campo(
            JPanel formulario,
            String etiqueta,
            String nombre,
            JTextField campo) {
        campo.setName(nombre);
        formulario.add(new JLabel(etiqueta));
        formulario.add(campo);
    }

    private void cargar() {
        try {
            mostrar(servicio.consultar(usuario));
            mensaje.setText("Politica actual cargada");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mensaje.setText("Error: " + e.getMessage());
        }
    }

    private void mostrar(PoliticaCita politica) {
        if (politica == null
                || politica.getAntelacionMinimaHoras() == null
                || politica.getAntelacionMaximaDias() == null
                || politica.getCancelacionMinimaHoras() == null
                || politica.getMaxCitasActivasPorPaciente() == null) {
            mensaje.setText("Error: politica recibida no valida");
            return;
        }
        antelacionMinima.setText(
                politica.getAntelacionMinimaHoras().toString());
        antelacionMaxima.setText(
                politica.getAntelacionMaximaDias().toString());
        cancelacionMinima.setText(
                politica.getCancelacionMinimaHoras().toString());
        maxCitasActivas.setText(
                politica.getMaxCitasActivasPorPaciente().toString());
    }

    private void guardar() {
        PoliticaCita politica;
        try {
            politica = ValidadorPoliticaCita.parsear(
                    antelacionMinima.getText(),
                    antelacionMaxima.getText(),
                    cancelacionMinima.getText(),
                    maxCitasActivas.getText());
        } catch (FormatoPoliticaCitaException e) {
            mensaje.setText("Error: " + e.getMessage());
            return;
        }

        try {
            mostrar(servicio.modificar(usuario, politica));
            mensaje.setText("Politica guardada");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mensaje.setText("Error: " + e.getMessage());
        }
    }
}

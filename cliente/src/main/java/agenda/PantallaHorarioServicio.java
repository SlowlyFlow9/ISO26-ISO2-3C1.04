package agenda;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class PantallaHorarioServicio extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HorarioServicioService servicio;
    private final Usuario usuario;
    private final Map<DayOfWeek, JTextField> campos =
            new EnumMap<>(DayOfWeek.class);
    private final JLabel mensaje = new JLabel(" ");
    private final JButton guardar = new JButton("Guardar horario");

    public PantallaHorarioServicio(
            HorarioServicioService servicio,
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
        for (DayOfWeek dia : DayOfWeek.values()) {
            String nombre = dia.getDisplayName(
                    TextStyle.FULL, Locale.forLanguageTag("es"));
            JTextField campo = new JTextField(24);
            campo.setName("intervalos" + dia.name());
            campos.put(dia, campo);
            formulario.add(new JLabel(nombre));
            formulario.add(campo);
        }
        guardar.setName("guardar");
        guardar.addActionListener(e -> guardar());
        add(new JLabel(
                "Intervalos HH:mm-HH:mm separados por comas; "
                        + "deja vacio un dia cerrado"),
                BorderLayout.NORTH);
        add(new JScrollPane(formulario), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(guardar, BorderLayout.WEST);
        pie.add(mensaje, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    private void cargar() {
        try {
            mostrar(servicio.consultar(usuario));
            mensaje.setText("Horario actual cargado");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mensaje.setText("Error: " + e.getMessage());
        }
    }

    private void mostrar(HorarioServicio horario) {
        if (horario == null || horario.getDias() == null) {
            mensaje.setText("Error: respuesta de horario no valida");
            return;
        }
        for (HorarioDia dia : horario.getDias()) {
            if (dia == null || dia.getDia() == null) {
                continue;
            }
            JTextField campo = campos.get(dia.getDia());
            if (campo != null) {
                campo.setText(formatear(dia.getIntervalos()));
            }
        }
    }

    private static String formatear(
            List<IntervaloHorario> intervalos) {
        if (intervalos == null) {
            return "";
        }
        return intervalos.stream()
                .map(intervalo -> intervalo.getHoraInicio()
                        + "-" + intervalo.getHoraFin())
                .reduce((primero, siguiente) ->
                        primero + ", " + siguiente)
                .orElse("");
    }

    private void guardar() {
        HorarioServicio horario = HorarioServicio.vacio();
        try {
            for (HorarioDia dia : horario.getDias()) {
                dia.setIntervalos(new ArrayList<>(
                        ValidadorHorarioServicio.parsearIntervalos(
                                campos.get(dia.getDia()).getText())));
            }
        } catch (FormatoHorarioServicioException e) {
            mensaje.setText("Error: " + e.getMessage());
            return;
        }

        try {
            HorarioServicio actualizado =
                    servicio.modificar(usuario, horario);
            mostrar(actualizado);
            mensaje.setText("Horario guardado");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mensaje.setText("Error: " + e.getMessage());
        }
    }
}

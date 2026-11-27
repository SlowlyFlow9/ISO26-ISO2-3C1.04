package solicitud;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import paciente.Rol;
import paciente.Usuario;

public class PantallaListaEspera extends JPanel {

    private final ListaEsperaService servicio;
    private final Usuario administrativo;
    private final JComboBox<Prioridad> filtroPrioridad =
            comboConTodas();
    private final JButton botonConsultar =
            new JButton("Consultar");
    private final JButton botonLimpiar =
            new JButton("Limpiar filtro");
    private final JLabel estado = new JLabel(" ");
    private final ModeloTablaListaEspera modelo =
            new ModeloTablaListaEspera();
    private final JTable tabla = new JTable(modelo);
    private boolean ocupado;

    public PantallaListaEspera(
            ListaEsperaService servicio,
            Usuario administrativo) {

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }

        this.servicio = servicio;
        this.administrativo = administrativo;
        construir();

        if (disponiblePara(administrativo)) {
            consultar();
        } else {
            estado.setText(
                    "Solo el personal administrativo puede consultar la lista.");
        }
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));
        filtros.add(new JLabel("Prioridad"));
        filtros.add(filtroPrioridad);
        filtros.add(botonConsultar);
        filtros.add(botonLimpiar);

        add(filtros, BorderLayout.NORTH);

        tabla.setAutoCreateRowSorter(false);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.add(estado, BorderLayout.CENTER);
        add(inferior, BorderLayout.SOUTH);

        botonConsultar.addActionListener(e -> consultar());
        botonLimpiar.addActionListener(e -> {
            filtroPrioridad.setSelectedItem(null);
            consultar();
        });

        actualizarControles();
    }

    private void consultar() {
        if (!disponiblePara(administrativo) || ocupado) {
            return;
        }

        Prioridad prioridad =
                (Prioridad) filtroPrioridad.getSelectedItem();
        ocupado = true;
        estado.setText("Consultando lista de espera...");
        actualizarControles();

        new SwingWorker<List<EntradaListaEspera>, Void>() {
            @Override
            protected List<EntradaListaEspera> doInBackground() {
                return servicio.consultar(administrativo, prioridad);
            }

            @Override
            protected void done() {
                ocupado = false;
                actualizarControles();
                try {
                    List<EntradaListaEspera> entradas = get();
                    modelo.setEntradas(entradas);
                    estado.setText(entradas.isEmpty()
                            ? "No hay solicitudes pendientes con ese filtro."
                            : entradas.size()
                                    + " solicitud(es) en la lista de espera.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    estado.setText("Se interrumpió la consulta de la lista.");
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    String mensaje = causa == null
                            || causa.getMessage() == null
                            ? "Error desconocido"
                            : causa.getMessage();
                    estado.setText("No se pudo consultar la lista de espera.");
                    javax.swing.JOptionPane.showMessageDialog(
                            PantallaListaEspera.this,
                            mensaje,
                            "Error en la lista de espera",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void actualizarControles() {
        boolean habilitado =
                disponiblePara(administrativo) && !ocupado;
        filtroPrioridad.setEnabled(habilitado);
        botonConsultar.setEnabled(habilitado);
        botonLimpiar.setEnabled(habilitado);
    }

    private static JComboBox<Prioridad> comboConTodas() {
        JComboBox<Prioridad> combo = new JComboBox<>();
        combo.addItem(null);
        for (Prioridad prioridad : Prioridad.values()) {
            combo.addItem(prioridad);
        }
        combo.setSelectedItem(null);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista,
                    Object valor,
                    int indice,
                    boolean seleccionado,
                    boolean foco) {
                super.getListCellRendererComponent(
                        lista, valor, indice, seleccionado, foco);
                if (valor == null) {
                    setText("Todas");
                }
                return this;
            }
        });
        return combo;
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.PERSONAL_ADMINISTRATIVO;
    }
}

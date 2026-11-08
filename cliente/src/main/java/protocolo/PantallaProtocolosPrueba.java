package protocolo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class PantallaProtocolosPrueba extends JFrame {

    private final ProtocolosPruebaService servicio;
    private final Usuario usuario;
    private final List<TipoPruebaOpcion> tiposPrueba;

    private final JComboBox<TipoPruebaOpcion> filtroTipo =
            new JComboBox<>();

    private final JComboBox<String> filtroEstado =
            new JComboBox<>(
                    new String[] {
                            "Todos",
                            "Activos",
                            "Inactivos"
                    });

    private final DefaultListModel<String> modeloLista =
            new DefaultListModel<>();

    private final JList<String> lista =
            new JList<>(modeloLista);

    private List<ProtocoloPrueba> resultados =
            new ArrayList<>();

    private Long idEnEdicion;

    private final JTextField nombre =
            new JTextField(20);

    private final JTextField descripcion =
            new JTextField(20);

    private final JComboBox<TipoPruebaOpcion> tipo =
            new JComboBox<>();

    private final JTextField instrucciones =
            new JTextField(20);

    private final JCheckBox contraste =
            new JCheckBox();

    private final JCheckBox consentimiento =
            new JCheckBox();

    private final JTextField seguridad =
            new JTextField(20);

    private final JButton nuevo =
            new JButton("Nuevo protocolo");

    private final JButton guardar =
            new JButton("Guardar");

    private final JButton desactivar =
            new JButton("Desactivar");

    public PantallaProtocolosPrueba(
            ProtocolosPruebaService servicio,
            Usuario usuario,
            List<TipoPruebaOpcion> tiposPrueba) {

        super("Gestión de protocolos de pruebas");

        this.servicio = servicio;
        this.usuario = usuario;
        this.tiposPrueba = tiposPrueba;

        prepararTipos();

        JPanel filtros =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtros.add(new JLabel("Tipo de prueba:"));
        filtros.add(filtroTipo);

        filtros.add(new JLabel("Estado:"));
        filtros.add(filtroEstado);

        JButton filtrar = new JButton("Filtrar");
        filtrar.addActionListener(e -> cargarListado());

        filtros.add(filtrar);

        lista.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        lista.setVisibleRowCount(8);

        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        JPanel formulario =
                new JPanel(new GridLayout(0, 2, 6, 6));

        campo(formulario, "Nombre *", nombre);
        campo(formulario, "Descripción *", descripcion);

        formulario.add(new JLabel("Tipo de prueba *"));
        formulario.add(tipo);

        campo(
                formulario,
                "Instrucciones de realización",
                instrucciones);

        formulario.add(new JLabel("Requiere contraste"));
        formulario.add(contraste);

        formulario.add(
                new JLabel("Requiere consentimiento"));

        formulario.add(consentimiento);

        campo(
                formulario,
                "Requisitos de seguridad",
                seguridad);

        JPanel botones =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        nuevo.addActionListener(e -> nuevo());
        guardar.addActionListener(e -> guardar());
        desactivar.addActionListener(e -> desactivar());

        botones.add(nuevo);
        botones.add(guardar);
        botones.add(desactivar);

        boolean administrador =
                puedeGestionar(usuario);

        nuevo.setVisible(administrador);
        guardar.setVisible(administrador);
        desactivar.setVisible(administrador);

        JPanel centro =
                new JPanel(new BorderLayout(8, 8));

        centro.add(
                new JScrollPane(lista),
                BorderLayout.WEST);

        centro.add(formulario, BorderLayout.CENTER);

        JPanel principal =
                new JPanel(new BorderLayout(8, 8));

        principal.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 10, 10, 10));

        principal.add(filtros, BorderLayout.NORTH);
        principal.add(centro, BorderLayout.CENTER);
        principal.add(botones, BorderLayout.SOUTH);

        add(principal);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        limpiarFormulario();
        cargarListado();
    }

    private void prepararTipos() {

        filtroTipo.addItem(
                new TipoPruebaOpcion(
                        null,
                        "Todos",
                        true));

        for (TipoPruebaOpcion t : tiposPrueba) {

            filtroTipo.addItem(t);

            if (t.activo()) {
                tipo.addItem(t);
            }
        }
    }

    private void cargarListado() {

        TipoPruebaOpcion seleccionado =
                (TipoPruebaOpcion)
                        filtroTipo.getSelectedItem();

        Long tipoId =
                seleccionado == null
                        ? null
                        : seleccionado.id();

        Boolean activo = null;

        if (filtroEstado.getSelectedIndex() == 1) {
            activo = true;
        }

        if (filtroEstado.getSelectedIndex() == 2) {
            activo = false;
        }

        resultados =
                servicio.listar(
                        usuario,
                        tipoId,
                        activo);

        modeloLista.clear();

        for (ProtocoloPrueba p : resultados) {
            modeloLista.addElement(textoLista(p));
        }

        limpiarFormulario();
    }

    private void cargarSeleccionado() {

        int indice = lista.getSelectedIndex();

        if (indice < 0) {
            return;
        }

        ProtocoloPrueba p = resultados.get(indice);

        idEnEdicion = p.getId();

        nombre.setText(texto(p.getNombre()));
        descripcion.setText(texto(p.getDescripcion()));
        instrucciones.setText(
                texto(p.getInstruccionesRealizacion()));

        contraste.setSelected(
                p.isRequiereContraste());

        consentimiento.setSelected(
                p.isRequiereConsentimiento());

        seguridad.setText(
                texto(p.getRequisitosSeguridad()));

        seleccionarTipo(p.getTipoPruebaId());

        desactivar.setEnabled(
                puedeGestionar(usuario)
                        && p.isActivo());
    }

    private void nuevo() {
        lista.clearSelection();
        limpiarFormulario();
        nombre.requestFocus();
    }

    private void guardar() {

        ProtocoloPrueba datos = leerFormulario();

        List<String> errores =
                ValidadorProtocoloPrueba.validar(datos);

        if (!errores.isEmpty()) {
            error(String.join("\n", errores));
            return;
        }

        try {

            if (idEnEdicion == null) {

                servicio.crear(usuario, datos);

                mensaje(
                        "Protocolo creado correctamente");

            } else {

                servicio.modificar(
                        usuario,
                        idEnEdicion,
                        datos);

                mensaje(
                        "Protocolo modificado correctamente");
            }

            cargarListado();

        } catch (DatosProtocoloInvalidosException ex) {

            error(String.join(
                    "\n",
                    ex.getErrores()));

        } catch (SinPermisoException
                 | TipoPruebaNoEncontradoException
                 | ProtocoloNoEncontradoException ex) {

            error(ex.getMessage());
        }
    }

    private void desactivar() {

        if (idEnEdicion == null) {
            return;
        }

        int respuesta =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Seguro que quieres desactivar este protocolo?",
                        "Confirmar desactivación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            servicio.desactivar(
                    usuario,
                    idEnEdicion);

            mensaje(
                    "Protocolo desactivado correctamente");

            cargarListado();

        } catch (SinPermisoException
                 | ProtocoloNoEncontradoException ex) {

            error(ex.getMessage());
        }
    }

    private ProtocoloPrueba leerFormulario() {

        ProtocoloPrueba p = new ProtocoloPrueba();

        p.setNombre(nombre.getText());
        p.setDescripcion(descripcion.getText());

        TipoPruebaOpcion seleccion =
                (TipoPruebaOpcion)
                        tipo.getSelectedItem();

        if (seleccion != null) {
            p.setTipoPruebaId(seleccion.id());
        }

        p.setInstruccionesRealizacion(
                instrucciones.getText());

        p.setRequiereContraste(
                contraste.isSelected());

        p.setRequiereConsentimiento(
                consentimiento.isSelected());

        p.setRequisitosSeguridad(
                seguridad.getText());

        return p;
    }

    private void limpiarFormulario() {

        idEnEdicion = null;

        nombre.setText("");
        descripcion.setText("");
        instrucciones.setText("");
        seguridad.setText("");

        contraste.setSelected(false);
        consentimiento.setSelected(false);

        if (tipo.getItemCount() > 0) {
            tipo.setSelectedIndex(0);
        }

        desactivar.setEnabled(false);
    }

    private void seleccionarTipo(Long id) {

        for (int i = 0; i < tipo.getItemCount(); i++) {

            TipoPruebaOpcion opcion =
                    tipo.getItemAt(i);

            if (opcion.id().equals(id)) {
                tipo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String textoLista(ProtocoloPrueba p) {

        return p.getNombre()
                + " | "
                + nombreTipo(p.getTipoPruebaId())
                + " | "
                + (p.isActivo()
                        ? "Activo"
                        : "Inactivo");
    }

    private String nombreTipo(Long id) {

        for (TipoPruebaOpcion t : tiposPrueba) {

            if (t.id().equals(id)) {
                return t.nombre();
            }
        }

        return "Tipo " + id;
    }

    private static void campo(
            JPanel panel,
            String etiqueta,
            JTextField campo) {

        panel.add(new JLabel(etiqueta));
        panel.add(campo);
    }

    private static String texto(String texto) {
        return texto == null ? "" : texto;
    }

    private void mensaje(String mensaje) {

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Gestión de protocolos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String mensaje) {

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static boolean puedeGestionar(
            Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.ADMINISTRADOR;
    }

    public static void main(String[] args) {

        Usuario usuario =
                new Usuario(
                        System.getProperty("user.name"),
                        Rol.ADMINISTRADOR);

        List<TipoPruebaOpcion> tipos =
                List.of(
                        new TipoPruebaOpcion(
                                1L,
                                "Radiografía",
                                true),

                        new TipoPruebaOpcion(
                                2L,
                                "TAC",
                                true),

                        new TipoPruebaOpcion(
                                3L,
                                "Resonancia magnética",
                                true));

        ProtocolosPruebaService servicio =
                new ProtocolosPruebaServiceSimulado(
                        List.of(1L, 2L, 3L));

        SwingUtilities.invokeLater(
                () -> new PantallaProtocolosPrueba(
                        servicio,
                        usuario,
                        tipos).setVisible(true));
    }
}
package paciente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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

/** CU23: buscador de pacientes y formulario de modificación. Solo valida formato (D-013); lo demás lo decide el servicio. */
public class PantallaModificarPaciente extends JFrame {
    private final PacientesService servicio;
    private final Usuario usuario;

    private final JTextField criterio = new JTextField(20);
    private final DefaultListModel<String> modeloLista = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modeloLista);
    private List<Paciente> resultados = new ArrayList<>();
    private long idEnEdicion = -1;

    private final JTextField nombre = new JTextField(20);
    private final JTextField apellidos = new JTextField(20);
    private final JTextField dni = new JTextField(20);
    private final JComboBox<String> dia = combo("Día", 1, 31);
    private final JComboBox<String> mes = combo("Mes", 1, 12);
    private final JComboBox<String> anio = combo("Año", LocalDate.now().getYear(), 1900);
    private final JTextField telefono = new JTextField(20);
    private final JTextField email = new JTextField(20);
    private final JTextField direccion = new JTextField(20);
    private final JTextField tarjeta = new JTextField(20);
    private final List<JTextField> campos = List.of(nombre, apellidos, dni, telefono, email, direccion, tarjeta);
    private final JButton guardar = new JButton("Guardar cambios");
    private final JLabel aviso = new JLabel(" ");

    public PantallaModificarPaciente(PacientesService servicio, Usuario usuario) {
        super("Modificar datos del paciente");
        this.servicio = servicio;
        this.usuario = usuario;

        JPanel buscador = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscador.add(new JLabel("Nombre, apellidos o DNI"));
        buscador.add(criterio);
        JButton buscar = new JButton("Buscar");
        buscar.addActionListener(e -> buscar());
        criterio.addActionListener(e -> buscar());
        buscador.add(buscar);

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(6);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) abrirSeleccionado();
        });

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        campo(formulario, "Nombre *", nombre);
        campo(formulario, "Apellidos *", apellidos);
        campo(formulario, "DNI/NIE *", dni);
        JPanel fecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fecha.add(dia);
        fecha.add(mes);
        fecha.add(anio);
        formulario.add(new JLabel("Fecha de nacimiento *"));
        formulario.add(fecha);
        campo(formulario, "Teléfono *", telefono);
        campo(formulario, "Email", email);
        campo(formulario, "Dirección", direccion);
        campo(formulario, "Tarjeta sanitaria", tarjeta);
        guardar.addActionListener(e -> guardar());
        formulario.add(new JLabel());
        formulario.add(guardar);

        aviso.setForeground(Color.RED);
        habilitar(false);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(new JScrollPane(lista), BorderLayout.NORTH);
        centro.add(formulario, BorderLayout.CENTER);
        JPanel principal = new JPanel(new BorderLayout(8, 8));
        principal.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        principal.add(buscador, BorderLayout.NORTH);
        principal.add(centro, BorderLayout.CENTER);
        principal.add(aviso, BorderLayout.SOUTH);
        add(principal);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /** Desplegable con un primer elemento "sin elegir" y los números de desde a hasta (en orden descendente si desde > hasta). */
    private static JComboBox<String> combo(String titulo, int desde, int hasta) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem(titulo);
        int paso = desde <= hasta ? 1 : -1;
        for (int i = desde; i != hasta + paso; i += paso) combo.addItem(String.valueOf(i));
        return combo;
    }

    private static void campo(JPanel panel, String etiqueta, JTextField texto) {
        panel.add(new JLabel(etiqueta));
        panel.add(texto);
    }

    private void buscar() {
        modeloLista.clear();
        resultados = new ArrayList<>();
        limpiarFormulario();
        if (criterio.getText().isBlank()) {
            error("Escribe un nombre, unos apellidos o un DNI para buscar");
            return;
        }
        try {
            resultados = servicio.buscar(usuario, criterio.getText());
        } catch (ServicioNoDisponibleException ex) {
            error(ex.getMessage());
            return;
        }
        for (Paciente p : resultados) modeloLista.addElement(textoLista(p));
        if (resultados.isEmpty())
            JOptionPane.showMessageDialog(this, "No se ha encontrado ningún paciente", "Búsqueda", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String textoLista(Paciente p) {
        return p.getNombre() + " " + p.getApellidos() + "  -  " + p.getDni();
    }

    /** Al elegir un paciente de la lista se pide al servicio y el formulario se abre con sus datos actuales. */
    private void abrirSeleccionado() {
        int i = lista.getSelectedIndex();
        if (i < 0) return;
        try {
            mostrar(servicio.consultar(usuario, resultados.get(i).getId()));
        } catch (PacienteNoEncontradoException | ServicioNoDisponibleException ex) {
            error(ex.getMessage());
        }
    }

    private void mostrar(Paciente p) {
        idEnEdicion = p.getId();
        nombre.setText(texto(p.getNombre()));
        apellidos.setText(texto(p.getApellidos()));
        dni.setText(texto(p.getDni()));
        telefono.setText(texto(p.getTelefono()));
        email.setText(texto(p.getEmail()));
        direccion.setText(texto(p.getDireccion()));
        tarjeta.setText(texto(p.getNumeroTarjetaSanitaria()));
        LocalDate f = p.getFechaNacimiento();
        if (f == null || f.getYear() < 1900 || f.getYear() > LocalDate.now().getYear()) {
            dia.setSelectedIndex(0);
            mes.setSelectedIndex(0);
            anio.setSelectedIndex(0);
        } else {
            dia.setSelectedIndex(f.getDayOfMonth());
            mes.setSelectedIndex(f.getMonthValue());
            anio.setSelectedIndex(LocalDate.now().getYear() - f.getYear() + 1);
        }
        habilitar(p.isActivo());
        aviso.setText(p.isActivo() ? " " : "Este paciente está dado de baja y no se puede modificar");
    }

    private void guardar() {
        if (idEnEdicion < 0) return;
        Paciente p = leerFormulario();
        if (p == null) return;
        List<String> errores = ValidadorPaciente.validar(p);
        if (!errores.isEmpty()) {
            error(String.join("\n", errores));
            return;
        }
        try {
            // ponytail: la llamada bloquea la interfaz hasta que responde el servidor; mover a un hilo aparte si la espera molesta
            Paciente actualizado = servicio.modificar(usuario, idEnEdicion, p);
            mostrar(actualizado);
            int i = lista.getSelectedIndex();
            if (i >= 0) {
                resultados.set(i, actualizado);
                modeloLista.set(i, textoLista(actualizado));
            }
            JOptionPane.showMessageDialog(this, "Datos actualizados correctamente\n\n" + resumen(actualizado),
                    "Paciente modificado", JOptionPane.INFORMATION_MESSAGE);
        } catch (PacienteYaInactivoException ex) {
            habilitar(false);
            aviso.setText("Este paciente está dado de baja y no se puede modificar");
            error(ex.getMessage());
        } catch (DatosInvalidosException ex) {
            error(String.join("\n", ex.getErrores()));
        } catch (SinPermisoException | PacienteNoEncontradoException | PacienteDuplicadoException
                 | ServicioNoDisponibleException ex) {
            error(ex.getMessage());
        }
    }

    /** Lee el formulario; devuelve null (tras avisar) si la fecha elegida no existe. */
    private Paciente leerFormulario() {
        Paciente p = new Paciente();
        p.setNombre(nombre.getText());
        p.setApellidos(apellidos.getText());
        p.setDni(dni.getText());
        p.setTelefono(telefono.getText());
        p.setEmail(email.getText());
        p.setDireccion(direccion.getText());
        p.setNumeroTarjetaSanitaria(tarjeta.getText());
        if (dia.getSelectedIndex() > 0 && mes.getSelectedIndex() > 0 && anio.getSelectedIndex() > 0) {
            try {
                p.setFechaNacimiento(LocalDate.of(Integer.parseInt((String) anio.getSelectedItem()),
                        mes.getSelectedIndex(), dia.getSelectedIndex()));
            } catch (DateTimeException ex) {
                error("La fecha de nacimiento no existe");
                return null;
            }
        }
        return p;
    }

    private void habilitar(boolean activo) {
        for (JTextField f : campos) f.setEditable(activo);
        dia.setEnabled(activo);
        mes.setEnabled(activo);
        anio.setEnabled(activo);
        guardar.setEnabled(activo);
    }

    private void limpiarFormulario() {
        idEnEdicion = -1;
        for (JTextField f : campos) f.setText("");
        dia.setSelectedIndex(0);
        mes.setSelectedIndex(0);
        anio.setSelectedIndex(0);
        habilitar(false);
        aviso.setText(" ");
    }

    private static String texto(String s) { return s == null ? "" : s; }

    private static String resumen(Paciente p) {
        return "Id: " + p.getId()
                + "\nNombre: " + p.getNombre() + " " + p.getApellidos()
                + "\nDNI/NIE: " + p.getDni()
                + "\nFecha de nacimiento: " + p.getFechaNacimiento()
                + "\nTeléfono: " + p.getTelefono()
                + "\nEmail: " + valor(p.getEmail())
                + "\nDirección: " + valor(p.getDireccion())
                + "\nTarjeta sanitaria: " + valor(p.getNumeroTarjetaSanitaria());
    }

    private static String valor(String s) { return s == null || s.isBlank() ? "-" : s; }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se puede modificar", JOptionPane.ERROR_MESSAGE);
    }

    /** La pantalla de modificación solo está disponible para el Personal administrativo (D-016). */
    public static boolean disponiblePara(Usuario usuario) { return usuario.puedeDarDeAltaPacientes(); }

    /** Uso: PantallaModificarPaciente [ROL] [URL del servidor | simulado]; por defecto PERSONAL_ADMINISTRATIVO y http://localhost:8080. */
    public static void main(String[] args) {
        Rol rol = args.length > 0 ? Rol.valueOf(args[0]) : Rol.PERSONAL_ADMINISTRATIVO;
        String url = args.length > 1 ? args[1] : "http://localhost:8080";
        Usuario usuario = new Usuario(System.getProperty("user.name"), rol);
        if (!disponiblePara(usuario)) {
            JOptionPane.showMessageDialog(null, "El rol " + rol + " no puede modificar pacientes", "Sin permiso", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PacientesService servicio = url.equals("simulado") ? simuladoConDatos() : new PacientesServiceHttp(url);
        SwingUtilities.invokeLater(() -> new PantallaModificarPaciente(servicio, usuario).setVisible(true));
    }

    /** Servicio simulado con tres pacientes de ejemplo para probar la pantalla sin servidor. */
    private static PacientesService simuladoConDatos() {
        PacientesServiceSimulado s = new PacientesServiceSimulado();
        Usuario admin = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
        try {
            s.crear(admin, ejemplo("Ana", "Pérez", "12345678Z", "T1"));
            s.crear(admin, ejemplo("Luis", "García", "87654321X", "T2"));
            s.crear(admin, ejemplo("Marta", "López", "11111111H", "T3"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return s;
    }

    private static Paciente ejemplo(String nombre, String apellidos, String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        p.setApellidos(apellidos);
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }
}

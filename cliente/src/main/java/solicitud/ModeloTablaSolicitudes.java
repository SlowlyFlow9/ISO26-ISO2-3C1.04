package solicitud;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * CU13 - Cliente. Datos de la tabla de la pantalla de gestion de solicitudes.
 */
public class ModeloTablaSolicitudes extends AbstractTableModel {

    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] COLUMNAS = {
        "ID", "Paciente", "Tipo de prueba", "Prioridad", "Fecha de solicitud", "Estado", "Seguimiento"
    };

    private final List<SolicitudPrueba> solicitudes = new ArrayList<>();

    //Nombre de cada tipo de prueba por su id, para mostrarlo en lugar del numero
    private java.util.function.LongFunction<String> nombreTipo = id -> "ID " + id;

    public void setSolicitudes(List<SolicitudPrueba> nuevas) {
        solicitudes.clear();
        if (nuevas != null) solicitudes.addAll(nuevas);
        fireTableDataChanged();
    }

    public void setNombreTipo(java.util.function.LongFunction<String> nombreTipo) {
        this.nombreTipo = nombreTipo;
        fireTableDataChanged();
    }

    public SolicitudPrueba getSolicitud(int fila) {
        return fila < 0 || fila >= solicitudes.size() ? null : solicitudes.get(fila);
    }

    //Fila de una solicitud por su id; -1 si no esta en la tabla
    public int filaDe(long id) {
        for (int i = 0; i < solicitudes.size(); i++) {
            if (solicitudes.get(i).getId() == id) return i;
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return solicitudes.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        SolicitudPrueba s = solicitudes.get(fila);
        return switch (columna) {
            case 0 -> s.getId();
            case 1 -> s.getPacienteId();
            case 2 -> nombreTipo.apply(s.getTipoPruebaId());
            case 3 -> s.getPrioridad();
            case 4 -> s.getFechaSolicitud() == null ? "" : s.getFechaSolicitud().format(FORMATO_FECHA);
            case 5 -> s.getEstado();
            case 6 -> s.esSeguimiento() ? "Sí" : "No";
            default -> "";
        };
    }

    //Texto con el detalle completo de una solicitud
    public static String detalle(SolicitudPrueba s, String nombreTipo) {
        if (s == null) return "Selecciona una solicitud para ver su detalle.";

        StringBuilder texto = new StringBuilder()
                .append("Solicitud ").append(s.getId()).append('\n')
                .append("Paciente: ").append(s.getPacienteId()).append('\n')
                .append("Médico: ").append(s.getMedicoId()).append('\n')
                .append("Tipo de prueba: ").append(nombreTipo).append('\n')
                .append("Prioridad: ").append(s.getPrioridad()).append('\n')
                .append("Estado: ").append(s.getEstado()).append('\n')
                .append("Fecha de solicitud: ").append(s.getFechaSolicitud() == null
                        ? "" : s.getFechaSolicitud().format(FORMATO_FECHA)).append('\n')
                .append("Indicación clínica: ").append(s.getIndicacionClinica()).append('\n')
                .append("Justificación: ").append(s.getJustificacion());

        if (s.esSeguimiento()) {
            texto.append("\nSeguimiento: cada ").append(s.getIntervalo()).append(' ').append(s.getUnidad())
                    .append(", ").append(s.getNumeroRepeticiones()).append(" repeticiones");
            if (s.getSolicitudOrigenId() != null) {
                texto.append("\nPrescripción original: ").append(s.getSolicitudOrigenId());
            }
        }
        if (s.getEstado() == EstadoSolicitud.ANULADA) {
            texto.append("\nMotivo de la anulación: ").append(s.getMotivoAnulacion());
            if (s.getFechaAnulacion() != null) {
                texto.append("\nFecha de la anulación: ").append(s.getFechaAnulacion().format(FORMATO_FECHA));
            }
        }
        return texto.toString();
    }
}

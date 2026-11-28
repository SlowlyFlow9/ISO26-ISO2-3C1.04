package solicitud;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

/**
 * CU28 - Cliente. Datos de la tabla de la pantalla de gestion de prioridades
 * y textos del historial de cambios.
 */
public class ModeloTablaPrioridades extends AbstractTableModel {

    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] COLUMNAS = {
        "Solicitud", "Paciente", "Tipo de prueba", "Prioridad", "Fecha de solicitud"
    };

    private final List<SolicitudPrioridad> solicitudes = new ArrayList<>();

    public void setSolicitudes(List<SolicitudPrioridad> nuevas) {
        solicitudes.clear();
        if (nuevas != null) solicitudes.addAll(nuevas);
        fireTableDataChanged();
    }

    public SolicitudPrioridad getSolicitud(int fila) {
        return fila < 0 || fila >= solicitudes.size() ? null : solicitudes.get(fila);
    }

    //Fila de una solicitud por su id; -1 si no esta en la tabla
    public int filaDe(long solicitudId) {
        for (int i = 0; i < solicitudes.size(); i++) {
            if (solicitudes.get(i).solicitudId() == solicitudId) return i;
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
        SolicitudPrioridad s = solicitudes.get(fila);
        return switch (columna) {
            case 0 -> s.solicitudId();
            case 1 -> s.pacienteId();
            case 2 -> "ID " + s.tipoPruebaId();
            case 3 -> s.prioridad();
            case 4 -> s.fechaSolicitud() == null ? "" : s.fechaSolicitud().format(FORMATO_FECHA);
            default -> "";
        };
    }

    /**
     * Texto del historial de cambios de una solicitud. Las reglas se muestran por su nombre
     * cuando se conocen; si no, por su id.
     */
    public static String textoHistorial(long solicitudId, List<CambioPrioridad> cambios,
            Map<Long, String> nombresReglas) {

        if (cambios == null || cambios.isEmpty()) {
            return "La solicitud " + solicitudId + " no tiene cambios de prioridad.";
        }
        StringBuilder texto = new StringBuilder("Cambios de prioridad de la solicitud ")
                .append(solicitudId).append(':');
        for (CambioPrioridad c : cambios) {
            texto.append("\n\n")
                    .append(c.fechaCambio() == null ? "" : c.fechaCambio().format(FORMATO_FECHA))
                    .append(" · ").append(c.realizadoPor()).append('\n')
                    .append(c.prioridadAnterior()).append(" → ").append(c.prioridadNueva()).append('\n')
                    .append("Regla: ").append(nombresReglas.getOrDefault(c.reglaPrioridadId(),
                            "ID " + c.reglaPrioridadId())).append('\n')
                    .append("Motivo: ").append(c.motivo());
        }
        return texto.toString();
    }
}

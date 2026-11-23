package solicitud;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ModeloTablaListaEspera extends AbstractTableModel {

    private static final String[] COLUMNAS = {
        "Solicitud", "Paciente", "Tipo de prueba", "Prioridad", "Fecha de solicitud"
    };

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<EntradaListaEspera> entradas = List.of();

    public void setEntradas(List<EntradaListaEspera> nuevas) {
        if (nuevas == null) {
            throw new IllegalArgumentException(
                    "La lista de espera no puede ser nula");
        }

        entradas = List.copyOf(nuevas);
        fireTableDataChanged();
    }

    public EntradaListaEspera getEntrada(int fila) {
        return fila < 0 || fila >= entradas.size()
                ? null
                : entradas.get(fila);
    }

    @Override
    public int getRowCount() {
        return entradas.size();
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
        EntradaListaEspera entrada = entradas.get(fila);
        return switch (columna) {
            case 0 -> entrada.solicitudId();
            case 1 -> entrada.pacienteId();
            case 2 -> "ID " + entrada.tipoPruebaId();
            case 3 -> entrada.prioridad();
            case 4 -> entrada.fechaSolicitud() == null
                    ? ""
                    : entrada.fechaSolicitud().format(FORMATO_FECHA);
            default -> "";
        };
    }
}

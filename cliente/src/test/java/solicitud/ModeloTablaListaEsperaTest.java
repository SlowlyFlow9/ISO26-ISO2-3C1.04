package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModeloTablaListaEsperaTest {

    @Test
    @DisplayName("La tabla muestra los campos administrativos de cada entrada")
    void muestraDatosDeLaLista() {
        ModeloTablaListaEspera modelo =
                new ModeloTablaListaEspera();
        modelo.setEntradas(List.of(new EntradaListaEspera(
                7,
                12,
                4,
                Prioridad.URGENTE,
                LocalDateTime.of(2026, 11, 20, 10, 30))));

        assertAll(
                () -> assertEquals(1, modelo.getRowCount()),
                () -> assertEquals(5, modelo.getColumnCount()),
                () -> assertEquals("Solicitud", modelo.getColumnName(0)),
                () -> assertEquals(7L, modelo.getValueAt(0, 0)),
                () -> assertEquals(12L, modelo.getValueAt(0, 1)),
                () -> assertEquals("ID 4", modelo.getValueAt(0, 2)),
                () -> assertEquals(Prioridad.URGENTE, modelo.getValueAt(0, 3)),
                () -> assertEquals(
                        "20/11/2026 10:30",
                        modelo.getValueAt(0, 4)));
    }
}

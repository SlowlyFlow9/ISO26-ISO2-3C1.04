package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelosTipoPruebaTest {

    @Test
    @DisplayName("TipoPrueba conserva todos sus datos")
    void tipoPruebaConservaDatos() {
        TipoPrueba tipo = new TipoPrueba(
                5L,
                "TAC",
                "Tomografía",
                30,
                true,
                "Ayuno",
                false);

        assertAll(
                () -> assertEquals(5L, tipo.getId()),
                () -> assertEquals("TAC", tipo.getNombre()),
                () -> assertEquals("Tomografía", tipo.getDescripcion()),
                () -> assertEquals(30, tipo.getDuracionEstimada()),
                () -> assertTrue(tipo.isRequierePreparacion()),
                () -> assertEquals("Ayuno", tipo.getInstruccionesPaciente()),
                () -> assertFalse(tipo.isActivo())
        );
    }

    @Test
    @DisplayName("DatosTipoPrueba conserva todos sus datos")
    void datosTipoPruebaConservaDatos() {
        DatosTipoPrueba datos = new DatosTipoPrueba(
                "RM",
                "Resonancia",
                45,
                true,
                "Retirar objetos metálicos");

        assertAll(
                () -> assertEquals("RM", datos.getNombre()),
                () -> assertEquals("Resonancia", datos.getDescripcion()),
                () -> assertEquals(45, datos.getDuracionEstimada()),
                () -> assertTrue(datos.isRequierePreparacion()),
                () -> assertEquals(
                        "Retirar objetos metálicos",
                        datos.getInstruccionesPaciente())
        );
    }

    @Test
    @DisplayName("Filtro nulo utiliza texto vacío y todos los estados")
    void filtroNuloUsaValoresPorDefecto() {
        FiltroTipoPrueba filtro =
                new FiltroTipoPrueba(null, null);

        assertAll(
                () -> assertEquals("", filtro.getTexto()),
                () -> assertEquals(
                        FiltroTipoPrueba.Estado.TODOS,
                        filtro.getEstado())
        );
    }

    @Test
    @DisplayName("El texto del filtro elimina espacios exteriores")
    void filtroRecortaTexto() {
        FiltroTipoPrueba filtro =
                new FiltroTipoPrueba(
                        "  resonancia  ",
                        FiltroTipoPrueba.Estado.ACTIVOS);

        assertAll(
                () -> assertEquals(
                        "resonancia",
                        filtro.getTexto()),
                () -> assertEquals(
                        FiltroTipoPrueba.Estado.ACTIVOS,
                        filtro.getEstado())
        );
    }
}
package protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProtocoloPruebaModeloTest {

    @Test
    @DisplayName(
            "ProtocoloPrueba conserva todos sus datos")
    void modeloConservaDatos() {

        ProtocoloPrueba p =
                new ProtocoloPrueba();

        p.setId(5L);
        p.setNombre("TAC");
        p.setDescripcion("Descripción");
        p.setTipoPruebaId(2L);
        p.setInstruccionesRealizacion("Ayuno");
        p.setRequiereContraste(true);
        p.setRequiereConsentimiento(true);
        p.setRequisitosSeguridad("Alergias");
        p.setActivo(false);

        assertEquals(5L, p.getId());
        assertEquals("TAC", p.getNombre());
        assertEquals(
                "Descripción",
                p.getDescripcion());
        assertEquals(
                2L,
                p.getTipoPruebaId());
        assertEquals(
                "Ayuno",
                p.getInstruccionesRealizacion());
        assertTrue(
                p.isRequiereContraste());
        assertTrue(
                p.isRequiereConsentimiento());
        assertEquals(
                "Alergias",
                p.getRequisitosSeguridad());
        assertFalse(p.isActivo());
    }

    @Test
    @DisplayName(
            "Copia conserva los datos pero es otro objeto")
    void copiaProtocolo() {

        ProtocoloPrueba original =
                new ProtocoloPrueba();

        original.setId(1L);
        original.setNombre("RM");
        original.setDescripcion("Resonancia");
        original.setTipoPruebaId(2L);
        original.setActivo(true);

        ProtocoloPrueba copia =
                original.copia();

        assertNotSame(original, copia);
        assertEquals(
                original.getId(),
                copia.getId());
        assertEquals(
                original.getNombre(),
                copia.getNombre());
        assertEquals(
                original.getDescripcion(),
                copia.getDescripcion());
        assertEquals(
                original.getTipoPruebaId(),
                copia.getTipoPruebaId());
        assertTrue(copia.isActivo());
    }

    @Test
    @DisplayName(
            "DatosProtocoloInvalidosException conserva los errores")
    void excepcionConservaErrores() {

        DatosProtocoloInvalidosException ex =
                new DatosProtocoloInvalidosException(
                        List.of(
                                "Error 1",
                                "Error 2"));

        assertEquals(
                2,
                ex.getErrores().size());

        assertEquals(
                "Error 1; Error 2",
                ex.getMessage());
    }
}
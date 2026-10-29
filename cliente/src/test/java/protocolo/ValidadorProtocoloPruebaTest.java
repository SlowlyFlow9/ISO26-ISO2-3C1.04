package protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidadorProtocoloPruebaTest {

    private ProtocoloPrueba valido() {

        ProtocoloPrueba p =
                new ProtocoloPrueba();

        p.setNombre("Protocolo TAC");
        p.setDescripcion("Protocolo para TAC");
        p.setTipoPruebaId(1L);

        return p;
    }

    @Test
    void protocoloValidoNoTieneErrores() {

        assertTrue(
                ValidadorProtocoloPrueba
                        .validar(valido())
                        .isEmpty());
    }

    @Test
    void nombreVacioGeneraError() {

        ProtocoloPrueba p = valido();

        p.setNombre(" ");

        assertEquals(
                1,
                ValidadorProtocoloPrueba
                        .validar(p)
                        .size());
    }

    @Test
    void obligatoriosVaciosGeneranErrores() {

        ProtocoloPrueba p =
                new ProtocoloPrueba();

        assertEquals(
                3,
                ValidadorProtocoloPrueba
                        .validar(p)
                        .size());
    }
}
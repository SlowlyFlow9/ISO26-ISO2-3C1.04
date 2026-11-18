package protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoberturaRestanteProtocoloTest {

    @Test
    @DisplayName(
            "Validar protocolo nulo devuelve un error")
    void protocoloNulo() {

        var errores =
                ValidadorProtocoloPrueba
                        .validar(null);

        assertEquals(1, errores.size());

        assertEquals(
                "Los datos del protocolo son obligatorios",
                errores.get(0));
    }

    @Test
    @DisplayName(
            "TipoPruebaOpcion conserva todos sus datos")
    void opcionConservaDatos() {

        TipoPruebaOpcion opcion =
                new TipoPruebaOpcion(
                        3L,
                        "Resonancia",
                        true);

        assertEquals(3L, opcion.id());
        assertEquals(
                "Resonancia",
                opcion.nombre());
        assertTrue(opcion.activo());

        assertEquals(
                "Resonancia",
                opcion.toString());
    }

    @Test
    @DisplayName(
            "Excepciones de protocolo conservan el mensaje")
    void excepcionesConservanMensaje() {

        assertEquals(
                "No existe",
                new ProtocoloNoEncontradoException(
                        "No existe")
                        .getMessage());

        assertEquals(
                "Tipo inexistente",
                new TipoPruebaNoEncontradoException(
                        "Tipo inexistente")
                        .getMessage());
    }
}
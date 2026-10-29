package protocolo;

import java.util.ArrayList;
import java.util.List;

public final class ValidadorProtocoloPrueba {

    private ValidadorProtocoloPrueba() {
    }

    public static List<String> validar(ProtocoloPrueba protocolo) {
        List<String> errores = new ArrayList<>();

        if (protocolo == null) {
            errores.add("Los datos del protocolo son obligatorios");
            return errores;
        }

        if (vacio(protocolo.getNombre())) {
            errores.add("El nombre es obligatorio");
        }

        if (vacio(protocolo.getDescripcion())) {
            errores.add("La descripción es obligatoria");
        }

        if (protocolo.getTipoPruebaId() == null) {
            errores.add("El tipo de prueba es obligatorio");
        }

        return errores;
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
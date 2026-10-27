package paciente;

import java.util.ArrayList;
import java.util.List;

/** Validación de formato en el cliente (D-013): solo campos obligatorios vacíos. */
public final class ValidadorPaciente {

    private ValidadorPaciente() { }

    /** Devuelve un mensaje por cada campo obligatorio vacío; lista vacía si el formato es correcto. */
    public static List<String> validar(Paciente p) {
        List<String> errores = new ArrayList<>();
        if (vacio(p.getNombre())) errores.add("El nombre es obligatorio");
        if (vacio(p.getApellidos())) errores.add("Los apellidos son obligatorios");
        if (vacio(p.getDni())) errores.add("El DNI/NIE es obligatorio");
        if (p.getFechaNacimiento() == null) errores.add("La fecha de nacimiento es obligatoria");
        if (vacio(p.getNumeroTarjetaSanitaria())) errores.add("La tarjeta sanitaria es obligatoria");
        return errores;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}

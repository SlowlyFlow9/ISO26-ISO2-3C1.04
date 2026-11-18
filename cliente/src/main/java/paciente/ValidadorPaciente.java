package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validación en el cliente (D-013, issue #37): campos obligatorios vacíos, formato de DNI/NIE y email, y fecha de nacimiento futura. */
public final class ValidadorPaciente {
    // ponytail: solo se comprueba el formato del DNI/NIE, no la letra de control
    private static final Pattern DNI_NIE = Pattern.compile("[0-9]{8}[A-Za-z]|[XYZxyz][0-9]{7}[A-Za-z]");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern TELEFONO = Pattern.compile("\\+?[0-9 ]{9,15}");

    private ValidadorPaciente() { }

    /** Devuelve un mensaje por cada error de formato; lista vacía si el formato es correcto. */
    public static List<String> validar(Paciente p) {
        List<String> errores = new ArrayList<>();
        if (vacio(p.getNombre())) errores.add("El nombre es obligatorio");
        if (vacio(p.getApellidos())) errores.add("Los apellidos son obligatorios");
        if (vacio(p.getDni())) errores.add("El DNI/NIE es obligatorio");
        else if (!DNI_NIE.matcher(p.getDni().trim()).matches()) errores.add("El DNI/NIE no tiene un formato válido");
        if (p.getFechaNacimiento() == null) errores.add("La fecha de nacimiento es obligatoria");
        else if (p.getFechaNacimiento().isAfter(LocalDate.now())) errores.add("La fecha de nacimiento no puede ser futura");
        if (vacio(p.getTelefono())) errores.add("El teléfono es obligatorio");
        else if (!TELEFONO.matcher(p.getTelefono().trim()).matches()) errores.add("El teléfono no tiene un formato válido");
        if (!vacio(p.getEmail()) && !EMAIL.matcher(p.getEmail().trim()).matches()) errores.add("El email no es válido");
        return errores;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}

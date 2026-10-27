package paciente;

import java.util.List;

/** Faltan campos obligatorios o alguno tiene formato o valor inválido (CU22). */
public class DatosInvalidosException extends Exception {
    private final List<String> errores;

    public DatosInvalidosException(List<String> errores) {
        super(String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() { return errores; }
}

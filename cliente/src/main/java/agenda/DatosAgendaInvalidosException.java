package agenda;

import java.util.List;

public class DatosAgendaInvalidosException extends Exception {

    private final List<String> errores;

    public DatosAgendaInvalidosException(List<String> errores) {
        super(String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
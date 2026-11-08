package protocolo;

import java.util.List;

public class DatosProtocoloInvalidosException extends Exception {

    private final List<String> errores;

    public DatosProtocoloInvalidosException(List<String> errores) {
        super(String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
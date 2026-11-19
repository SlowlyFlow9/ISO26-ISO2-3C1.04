package cita;

public class PacienteNoVinculadoException
        extends Exception {

    public PacienteNoVinculadoException(
            String mensaje) {

        super(mensaje);
    }
}
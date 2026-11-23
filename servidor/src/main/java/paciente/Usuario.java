package paciente;

/** Usuario que realiza una operación; el cliente lo envía en cada petición (D-023). */
public record Usuario(String nombre, Rol rol, Long medicoId) {

    public Usuario(String nombre, Rol rol) {
        this(nombre, rol, null);
    }
}

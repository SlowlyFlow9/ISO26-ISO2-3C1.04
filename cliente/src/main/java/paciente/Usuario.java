package paciente;

/** Usuario que usa el cliente y que se envía al servidor en cada petición (D-023). */
public record Usuario(String nombre, Rol rol, Long medicoId) {

    public Usuario(String nombre, Rol rol) {
        this(nombre, rol, null);
    }

    /** Única regla del cliente sobre quién puede dar de alta pacientes (D-016). */
    public boolean puedeDarDeAltaPacientes() { return rol == Rol.PERSONAL_ADMINISTRATIVO; }
}

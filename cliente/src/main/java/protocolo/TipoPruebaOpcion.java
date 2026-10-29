package protocolo;

public record TipoPruebaOpcion(Long id, String nombre, boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
package tipoprueba;

/** Filtro del listado: texto del nombre y estado (todos, activos, inactivos). */
public class FiltroTipoPrueba {
    public enum Estado { TODOS, ACTIVOS, INACTIVOS }

    private final String texto;
    private final Estado estado;

    public FiltroTipoPrueba(String texto, Estado estado) {
        this.texto = texto == null ? "" : texto.trim();
        this.estado = estado == null ? Estado.TODOS : estado;
    }

    public String getTexto() { return texto; }
    public Estado getEstado() { return estado; }
}

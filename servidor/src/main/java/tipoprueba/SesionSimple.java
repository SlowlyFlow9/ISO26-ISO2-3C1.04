package tipoprueba;

/** Implementación sencilla de ContextoUsuario, modificable (para pruebas y arranque). */
public class SesionSimple implements ContextoUsuario {
    private String usuario;
    private Rol rol;

    public SesionSimple(String usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    public void setRol(Rol rol) { this.rol = rol; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    @Override public String usuario() { return usuario; }
    @Override public Rol rol() { return rol; }
}

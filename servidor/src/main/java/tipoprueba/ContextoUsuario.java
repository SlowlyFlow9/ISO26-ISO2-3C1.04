package tipoprueba;

/** Usuario que ejecuta la operación. Control de rol sencillo hasta It. 12 (D-016). */
public interface ContextoUsuario {
    String usuario();

    Rol rol();
}

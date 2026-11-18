package paciente;

/**
 * Único punto de comprobación de rol (D-016, D-023): en la It. 12 se sustituye por el control basado en permisos.
 * ponytail: el rol lo declara el cliente, sin autenticación real hasta el SSO.
 */
public final class ControlAcceso {

    private ControlAcceso() { }

    public static void exigir(Usuario usuario, Rol requerido) throws SinPermisoException {
        if (usuario == null || usuario.rol() != requerido)
            throw new SinPermisoException("Esta operación solo está permitida al rol " + requerido);
    }
}

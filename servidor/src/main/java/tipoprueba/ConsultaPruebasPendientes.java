package tipoprueba;

/**
 * Puerto hacia las pruebas prescritas (CU11/CU12, It. 3). Hasta entonces se usa SinPruebasPendientes.
 */
@FunctionalInterface
public interface ConsultaPruebasPendientes {
    int contarPendientes(long idTipoPrueba);
}

package tipoprueba;

import java.util.List;

/** Contrato del servicio de tipos de prueba (CU33). Lo consumen el cliente, CU34, CU11/12 y CU35. */
public interface TiposPruebaService {

    TipoPrueba crear(DatosTipoPrueba datos) throws ServicioException;

    TipoPrueba consultar(long id) throws ServicioException;

    List<TipoPrueba> listar(FiltroTipoPrueba filtro) throws ServicioException;

    TipoPrueba modificar(long id, DatosTipoPrueba datos) throws ServicioException;

    /** Baja lógica. Falla con PruebasPendientesException si hay pruebas pendientes. */
    default void desactivar(long id) throws ServicioException {
        desactivar(id, false);
    }

    /** Baja lógica; con confirmado = true se permite aunque haya pruebas pendientes. */
    void desactivar(long id, boolean confirmado) throws ServicioException;

    /** Pruebas pendientes de este tipo, para que la interfaz pueda avisar antes de desactivar. */
    int contarPruebasPendientes(long id) throws ServicioException;
}

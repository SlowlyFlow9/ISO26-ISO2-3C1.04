package tipoprueba;

import java.util.List;

/** Contrato que consume la interfaz (lo implementa el subissue de servidor, #29). */
public interface TiposPruebaService {

    TipoPrueba crear(DatosTipoPrueba datos) throws ServicioException;

    TipoPrueba consultar(long id) throws ServicioException;

    List<TipoPrueba> listar(FiltroTipoPrueba filtro) throws ServicioException;

    TipoPrueba modificar(long id, DatosTipoPrueba datos) throws ServicioException;

    /** Baja lógica, sin borrado físico (D-008). */
    void desactivar(long id) throws ServicioException;

    /**
     * Número de pruebas pendientes de este tipo, para avisar antes de desactivar.
     * PROPUESTA de ampliación del contrato: acordar con el subissue de servidor.
     */
    default int contarPruebasPendientes(long id) throws ServicioException {
        return 0;
    }
}

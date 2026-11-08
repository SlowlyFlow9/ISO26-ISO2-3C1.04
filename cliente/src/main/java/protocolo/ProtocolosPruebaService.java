package protocolo;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ProtocolosPruebaService {

    ProtocoloPrueba crear(Usuario solicitante, ProtocoloPrueba datos)
            throws SinPermisoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException;

    ProtocoloPrueba consultar(Usuario solicitante, long id)
            throws ProtocoloNoEncontradoException;

    List<ProtocoloPrueba> listar(
            Usuario solicitante,
            Long tipoPruebaId,
            Boolean activo);

    ProtocoloPrueba modificar(
            Usuario solicitante,
            long id,
            ProtocoloPrueba datos)
            throws SinPermisoException,
            ProtocoloNoEncontradoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException;

    void desactivar(Usuario solicitante, long id)
            throws SinPermisoException,
            ProtocoloNoEncontradoException;
}
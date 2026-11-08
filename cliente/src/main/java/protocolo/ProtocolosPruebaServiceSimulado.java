package protocolo;

import java.util.ArrayList;
import java.util.List;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ProtocolosPruebaServiceSimulado
        implements ProtocolosPruebaService {

    private final List<ProtocoloPrueba> protocolos = new ArrayList<>();
    private final List<Long> tiposPruebaExistentes;
    private long siguienteId = 1;

    public ProtocolosPruebaServiceSimulado(
            List<Long> tiposPruebaExistentes) {

        this.tiposPruebaExistentes =
                new ArrayList<>(tiposPruebaExistentes);
    }

    @Override
    public ProtocoloPrueba crear(
            Usuario solicitante,
            ProtocoloPrueba datos)
            throws SinPermisoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException {

        exigirAdministrador(solicitante);

        List<String> errores =
                ValidadorProtocoloPrueba.validar(datos);

        if (!errores.isEmpty()) {
            throw new DatosProtocoloInvalidosException(errores);
        }

        comprobarTipo(datos.getTipoPruebaId());

        ProtocoloPrueba creado = datos.copia();
        creado.setId(siguienteId++);
        creado.setActivo(true);

        protocolos.add(creado);

        return creado.copia();
    }

    @Override
    public ProtocoloPrueba consultar(
            Usuario solicitante,
            long id)
            throws ProtocoloNoEncontradoException {

        for (ProtocoloPrueba protocolo : protocolos) {
            if (protocolo.getId() == id) {
                return protocolo.copia();
            }
        }

        throw new ProtocoloNoEncontradoException(
                "No existe el protocolo con id " + id);
    }

    @Override
    public List<ProtocoloPrueba> listar(
            Usuario solicitante,
            Long tipoPruebaId,
            Boolean activo) {

        List<ProtocoloPrueba> resultado = new ArrayList<>();

        for (ProtocoloPrueba protocolo : protocolos) {

            if (tipoPruebaId != null
                    && !tipoPruebaId.equals(
                            protocolo.getTipoPruebaId())) {
                continue;
            }

            if (activo != null
                    && activo != protocolo.isActivo()) {
                continue;
            }

            resultado.add(protocolo.copia());
        }

        return resultado;
    }

    @Override
    public ProtocoloPrueba modificar(
            Usuario solicitante,
            long id,
            ProtocoloPrueba datos)
            throws SinPermisoException,
            ProtocoloNoEncontradoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException {

        exigirAdministrador(solicitante);

        List<String> errores =
                ValidadorProtocoloPrueba.validar(datos);

        if (!errores.isEmpty()) {
            throw new DatosProtocoloInvalidosException(errores);
        }

        comprobarTipo(datos.getTipoPruebaId());

        for (ProtocoloPrueba protocolo : protocolos) {

            if (protocolo.getId() == id) {

                protocolo.setNombre(datos.getNombre().trim());
                protocolo.setDescripcion(
                        datos.getDescripcion().trim());

                protocolo.setTipoPruebaId(
                        datos.getTipoPruebaId());

                protocolo.setInstruccionesRealizacion(
                        limpiar(
                                datos.getInstruccionesRealizacion()));

                protocolo.setRequiereContraste(
                        datos.isRequiereContraste());

                protocolo.setRequiereConsentimiento(
                        datos.isRequiereConsentimiento());

                protocolo.setRequisitosSeguridad(
                        limpiar(
                                datos.getRequisitosSeguridad()));

                return protocolo.copia();
            }
        }

        throw new ProtocoloNoEncontradoException(
                "No existe el protocolo con id " + id);
    }

    @Override
    public void desactivar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            ProtocoloNoEncontradoException {

        exigirAdministrador(solicitante);

        for (ProtocoloPrueba protocolo : protocolos) {
            if (protocolo.getId() == id) {
                protocolo.setActivo(false);
                return;
            }
        }

        throw new ProtocoloNoEncontradoException(
                "No existe el protocolo con id " + id);
    }

    private void comprobarTipo(Long tipoPruebaId)
            throws TipoPruebaNoEncontradoException {

        if (!tiposPruebaExistentes.contains(tipoPruebaId)) {
            throw new TipoPruebaNoEncontradoException(
                    "El tipo de prueba seleccionado no existe");
        }
    }

    private static void exigirAdministrador(
            Usuario usuario)
            throws SinPermisoException {

        if (usuario == null
                || usuario.rol() != Rol.ADMINISTRADOR) {

            throw new SinPermisoException(
                    "Solo el Administrador del Sistema puede realizar esta operación");
        }
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }
}
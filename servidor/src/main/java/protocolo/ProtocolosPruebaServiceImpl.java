package protocolo;

import java.util.ArrayList;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ProtocolosPruebaServiceImpl
        implements ProtocolosPruebaService {

    private final ProtocolosPruebaRepositorio repositorio;
    private final TiposPruebaConsulta tiposPrueba;

    public ProtocolosPruebaServiceImpl(
            ProtocolosPruebaRepositorio repositorio,
            TiposPruebaConsulta tiposPrueba) {

        this.repositorio = repositorio;
        this.tiposPrueba = tiposPrueba;
    }

    @Override
    public synchronized ProtocoloPrueba crear(
            Usuario solicitante,
            ProtocoloPrueba datos)
            throws SinPermisoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException {

        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);

        validar(datos);
        comprobarTipoPrueba(datos.getTipoPruebaId());

        ProtocoloPrueba creado = new ProtocoloPrueba();

        creado.setNombre(datos.getNombre().trim());
        creado.setDescripcion(datos.getDescripcion().trim());
        creado.setTipoPruebaId(datos.getTipoPruebaId());
        creado.setInstruccionesRealizacion(
                limpiar(datos.getInstruccionesRealizacion()));
        creado.setRequiereContraste(
                datos.isRequiereContraste());
        creado.setRequiereConsentimiento(
                datos.isRequiereConsentimiento());
        creado.setRequisitosSeguridad(
                limpiar(datos.getRequisitosSeguridad()));
        creado.setActivo(true);

        return repositorio.guardar(creado);
    }

    @Override
    public ProtocoloPrueba consultar(
            Usuario solicitante,
            long id)
            throws ProtocoloNoEncontradoException {

        return repositorio.buscarPorId(id)
                .orElseThrow(() ->
                        new ProtocoloNoEncontradoException(
                                "No existe el protocolo con id " + id));
    }

    @Override
    public List<ProtocoloPrueba> listar(
            Usuario solicitante,
            Long tipoPruebaId,
            Boolean activo) {

        return repositorio.listar(tipoPruebaId, activo);
    }

    @Override
    public synchronized ProtocoloPrueba modificar(
            Usuario solicitante,
            long id,
            ProtocoloPrueba datos)
            throws SinPermisoException,
            DatosProtocoloInvalidosException,
            TipoPruebaNoEncontradoException,
            ProtocoloNoEncontradoException {

        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);

        ProtocoloPrueba actual = consultar(solicitante, id);

        validar(datos);
        comprobarTipoPrueba(datos.getTipoPruebaId());

        actual.setNombre(datos.getNombre().trim());
        actual.setDescripcion(datos.getDescripcion().trim());
        actual.setTipoPruebaId(datos.getTipoPruebaId());
        actual.setInstruccionesRealizacion(
                limpiar(datos.getInstruccionesRealizacion()));
        actual.setRequiereContraste(
                datos.isRequiereContraste());
        actual.setRequiereConsentimiento(
                datos.isRequiereConsentimiento());
        actual.setRequisitosSeguridad(
                limpiar(datos.getRequisitosSeguridad()));

        return repositorio.guardar(actual);
    }

    @Override
    public synchronized void desactivar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            ProtocoloNoEncontradoException {

        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);

        ProtocoloPrueba protocolo = consultar(solicitante, id);

        protocolo.setActivo(false);

        repositorio.guardar(protocolo);
    }

    private void validar(ProtocoloPrueba protocolo)
            throws DatosProtocoloInvalidosException {

        List<String> errores = new ArrayList<>();

        if (protocolo == null) {
            errores.add("Los datos del protocolo son obligatorios");
        } else {
            if (vacio(protocolo.getNombre())) {
                errores.add("El nombre es obligatorio");
            }

            if (vacio(protocolo.getDescripcion())) {
                errores.add("La descripción es obligatoria");
            }

            if (protocolo.getTipoPruebaId() == null) {
                errores.add("El tipo de prueba es obligatorio");
            }
        }

        if (!errores.isEmpty()) {
            throw new DatosProtocoloInvalidosException(errores);
        }
    }

    private void comprobarTipoPrueba(Long tipoPruebaId)
            throws TipoPruebaNoEncontradoException {

        if (tipoPruebaId == null
                || !tiposPrueba.existe(tipoPruebaId)) {

            throw new TipoPruebaNoEncontradoException(
                    "No existe el tipo de prueba con id "
                            + tipoPruebaId);
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String limpiar(String texto) {
        return vacio(texto) ? null : texto.trim();
    }
}
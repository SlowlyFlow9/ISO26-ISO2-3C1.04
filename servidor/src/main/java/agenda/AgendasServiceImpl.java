package agenda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class AgendasServiceImpl
        implements AgendasService {

    private final AgendasRepositorio repositorio;

    public AgendasServiceImpl(
            AgendasRepositorio repositorio) {

        this.repositorio = repositorio;
    }

    @Override
    public synchronized AgendaMedica crear(
            Usuario solicitante,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);

        validar(datos);

        AgendaMedica creada = new AgendaMedica();

        creada.setMedicoId(datos.getMedicoId());
        creada.setFecha(datos.getFecha());
        creada.setHoraInicio(datos.getHoraInicio());
        creada.setHoraFin(datos.getHoraFin());
        creada.setObservaciones(
                limpiar(datos.getObservaciones()));
        creada.setActivo(true);

        return repositorio.guardar(creada);
    }

    @Override
    public AgendaMedica consultar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);

        return repositorio.buscarPorId(id)
                .orElseThrow(() ->
                        new AgendaNoEncontradaException(
                                "No existe la agenda con id "
                                        + id));
    }

    @Override
    public List<AgendaMedica> listar(
            Usuario solicitante,
            Long medicoId,
            LocalDate fecha,
            Boolean activo)
            throws SinPermisoException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);

        return repositorio.listar(
                medicoId,
                fecha,
                activo);
    }

    @Override
    public synchronized AgendaMedica modificar(
            Usuario solicitante,
            long id,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException,
            AgendaNoEncontradaException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);

        AgendaMedica actual = buscar(id);

        validar(datos);

        actual.setMedicoId(datos.getMedicoId());
        actual.setFecha(datos.getFecha());
        actual.setHoraInicio(datos.getHoraInicio());
        actual.setHoraFin(datos.getHoraFin());
        actual.setObservaciones(
                limpiar(datos.getObservaciones()));

        return repositorio.guardar(actual);
    }

    @Override
    public synchronized void eliminar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException {

        ControlAcceso.exigir(
                solicitante,
                Rol.PERSONAL_ADMINISTRATIVO);

        AgendaMedica agenda = buscar(id);

        agenda.setActivo(false);

        repositorio.guardar(agenda);
    }

    private AgendaMedica buscar(long id)
            throws AgendaNoEncontradaException {

        return repositorio.buscarPorId(id)
                .orElseThrow(() ->
                        new AgendaNoEncontradaException(
                                "No existe la agenda con id "
                                        + id));
    }

    private void validar(
            AgendaMedica agenda)
            throws DatosAgendaInvalidosException {

        List<String> errores = new ArrayList<>();

        if (agenda == null) {
            errores.add(
                    "Los datos de la agenda son obligatorios");
        } else {

            if (agenda.getMedicoId() <= 0) {
                errores.add(
                        "El médico es obligatorio");
            }

            if (agenda.getFecha() == null) {
                errores.add(
                        "La fecha es obligatoria");
            }

            if (agenda.getHoraInicio() == null) {
                errores.add(
                        "La hora de inicio es obligatoria");
            }

            if (agenda.getHoraFin() == null) {
                errores.add(
                        "La hora de fin es obligatoria");
            }

            if (agenda.getHoraInicio() != null
                    && agenda.getHoraFin() != null
                    && !agenda.getHoraFin()
                            .isAfter(agenda.getHoraInicio())) {

                errores.add(
                        "La hora de fin debe ser posterior "
                                + "a la hora de inicio");
            }
        }

        if (!errores.isEmpty()) {
            throw new DatosAgendaInvalidosException(
                    errores);
        }
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }
}
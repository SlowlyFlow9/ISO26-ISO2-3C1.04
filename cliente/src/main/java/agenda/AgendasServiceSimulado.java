package agenda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class AgendasServiceSimulado
        implements AgendasService {

    private final List<AgendaMedica> agendas =
            new ArrayList<>();

    private long siguienteId = 1;

    @Override
    public AgendaMedica crear(
            Usuario solicitante,
            AgendaMedica datos)
            throws SinPermisoException,
            DatosAgendaInvalidosException {

        exigirPersonalAdministrativo(solicitante);

        List<String> errores =
                ValidadorAgendaMedica.validar(datos);

        if (!errores.isEmpty()) {
            throw new DatosAgendaInvalidosException(
                    errores);
        }

        AgendaMedica creada = datos.copia();
        creada.setId(siguienteId++);
        creada.setObservaciones(
                limpiar(datos.getObservaciones()));
        creada.setActivo(true);

        agendas.add(creada);

        return creada.copia();
    }

    @Override
    public AgendaMedica consultar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException {

        exigirPersonalAdministrativo(solicitante);

        for (AgendaMedica agenda : agendas) {
            if (agenda.getId() == id) {
                return agenda.copia();
            }
        }

        throw new AgendaNoEncontradaException(
                "No existe la agenda con id " + id);
    }

    @Override
    public List<AgendaMedica> listar(
            Usuario solicitante,
            Long medicoId,
            LocalDate fecha,
            Boolean activo)
            throws SinPermisoException {

        exigirPersonalAdministrativo(solicitante);

        List<AgendaMedica> resultado =
                new ArrayList<>();

        for (AgendaMedica agenda : agendas) {

            if (medicoId != null
                    && medicoId != agenda.getMedicoId()) {
                continue;
            }

            if (fecha != null
                    && !fecha.equals(agenda.getFecha())) {
                continue;
            }

            if (activo != null
                    && activo != agenda.isActivo()) {
                continue;
            }

            resultado.add(agenda.copia());
        }

        return resultado;
    }

    @Override
    public AgendaMedica modificar(
            Usuario solicitante,
            long id,
            AgendaMedica datos)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            DatosAgendaInvalidosException {

        exigirPersonalAdministrativo(solicitante);

        List<String> errores =
                ValidadorAgendaMedica.validar(datos);

        if (!errores.isEmpty()) {
            throw new DatosAgendaInvalidosException(
                    errores);
        }

        for (AgendaMedica agenda : agendas) {

            if (agenda.getId() == id) {

                agenda.setMedicoId(
                        datos.getMedicoId());

                agenda.setFecha(
                        datos.getFecha());

                agenda.setHoraInicio(
                        datos.getHoraInicio());

                agenda.setHoraFin(
                        datos.getHoraFin());

                agenda.setObservaciones(
                        limpiar(
                                datos.getObservaciones()));

                return agenda.copia();
            }
        }

        throw new AgendaNoEncontradaException(
                "No existe la agenda con id " + id);
    }

    @Override
    public void eliminar(
            Usuario solicitante,
            long id)
            throws SinPermisoException,
            AgendaNoEncontradaException {

        exigirPersonalAdministrativo(solicitante);

        for (AgendaMedica agenda : agendas) {

            if (agenda.getId() == id) {
                agenda.setActivo(false);
                return;
            }
        }

        throw new AgendaNoEncontradaException(
                "No existe la agenda con id " + id);
    }

    private static void exigirPersonalAdministrativo(
            Usuario usuario)
            throws SinPermisoException {

        if (usuario == null
                || usuario.rol()
                        != Rol.PERSONAL_ADMINISTRATIVO) {

            throw new SinPermisoException(
                    "Solo el Personal Administrativo "
                            + "puede gestionar agendas médicas");
        }
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }
}
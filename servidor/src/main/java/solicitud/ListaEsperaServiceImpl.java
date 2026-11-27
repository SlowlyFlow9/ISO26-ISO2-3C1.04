package solicitud;

import java.util.Comparator;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ListaEsperaServiceImpl implements ListaEsperaService {

    private static final Comparator<SolicitudPrueba> ORDEN =
            Comparator.comparing(SolicitudPrueba::getPrioridad)
                    .thenComparing(SolicitudPrueba::getFechaSolicitud,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(SolicitudPrueba::getId);

    private final SolicitudesRepositorio solicitudes;

    public ListaEsperaServiceImpl(SolicitudesRepositorio solicitudes) {
        if (solicitudes == null) {
            throw new IllegalArgumentException(
                    "El repositorio de solicitudes es obligatorio");
        }

        this.solicitudes = solicitudes;
    }

    @Override
    public List<EntradaListaEspera> consultar(
            Usuario solicitante,
            Prioridad prioridad) throws SinPermisoException {

        ControlAcceso.exigir(
                solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        return solicitudes.todas().stream()
                .filter(solicitud ->
                        solicitud.getEstado()
                                == EstadoSolicitud.PENDIENTE_CITACION)
                .filter(solicitud ->
                        prioridad == null
                                || solicitud.getPrioridad() == prioridad)
                .sorted(ORDEN)
                .map(solicitud -> new EntradaListaEspera(
                        solicitud.getId(),
                        solicitud.getPacienteId(),
                        solicitud.getTipoPruebaId(),
                        solicitud.getPrioridad(),
                        solicitud.getFechaSolicitud()))
                .toList();
    }
}

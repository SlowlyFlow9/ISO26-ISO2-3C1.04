package solicitud;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU28 - Implementacion de la gestion de prioridades (D-038).
 * Lee las reglas de CU37 sin modificarlas y guarda cada cambio en el historial (D-017).
 */
public class PrioridadesServiceImpl implements PrioridadesService {

    //Orden de la lista: prioridad (urgente, preferente, ordinaria), fecha de solicitud e id
    private static final Comparator<SolicitudPrueba> ORDEN =
            Comparator.comparing(SolicitudPrueba::getPrioridad)
                    .thenComparing(SolicitudPrueba::getFechaSolicitud,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(SolicitudPrueba::getId);

    private final SolicitudesRepositorio solicitudes;
    private final ReglasPrioridadRepositorio reglas;
    private final CambiosPrioridadRepositorio cambios;

    public PrioridadesServiceImpl(
            SolicitudesRepositorio solicitudes,
            ReglasPrioridadRepositorio reglas,
            CambiosPrioridadRepositorio cambios) {

        if (solicitudes == null || reglas == null || cambios == null) {
            throw new IllegalArgumentException("Los repositorios son obligatorios");
        }
        this.solicitudes = solicitudes;
        this.reglas = reglas;
        this.cambios = cambios;
    }

    @Override
    public List<SolicitudPrioridad> consultarPendientes(
            Usuario solicitante,
            Prioridad prioridad) throws SinPermisoException {

        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        return solicitudes.todas().stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE_CITACION)
                .filter(s -> prioridad == null || s.getPrioridad() == prioridad)
                .sorted(ORDEN)
                .map(SolicitudPrioridad::de)
                .toList();
    }

    @Override
    public List<ReglaPrioridadAplicable> consultarReglasActivas(
            Usuario solicitante) throws SinPermisoException {

        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        return reglas.todas().stream()
                .filter(ReglaPrioridad::isActivo)
                .sorted(Comparator.comparingInt(ReglaPrioridad::getOrden))
                .map(ReglaPrioridadAplicable::de)
                .toList();
    }

    @Override
    public synchronized SolicitudPrioridad cambiarPrioridad(
            Usuario solicitante,
            long solicitudId,
            DatosCambioPrioridad datos) throws SinPermisoException {

        //1. Comprobar el rol
        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        //2. Datos obligatorios
        if (datos == null || datos.prioridad() == null) {
            throw new IllegalArgumentException("Falta la nueva prioridad");
        }
        if (datos.reglaPrioridadId() == null) {
            throw new IllegalArgumentException("Falta la regla de prioridad aplicada");
        }
        if (datos.motivo() == null || datos.motivo().isBlank()) {
            throw new IllegalArgumentException("Falta el motivo del cambio");
        }

        //3. La solicitud debe existir y estar pendiente de citacion (D-031)
        SolicitudPrueba actual = solicitudes.buscarPorId(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));

        if (actual.getEstado() != EstadoSolicitud.PENDIENTE_CITACION) {
            throw new SolicitudNoGestionableException(
                    "La solicitud no esta pendiente de citacion");
        }

        //4. La regla de CU37 debe existir y estar activa
        ReglaPrioridad regla = reglas.buscarPorId(datos.reglaPrioridadId())
                .orElseThrow(() -> new NoSuchElementException("Regla de prioridad no encontrada"));

        if (!regla.isActivo()) {
            throw new IllegalArgumentException("La regla de prioridad no esta activa");
        }

        //5. La nueva prioridad debe ser distinta de la actual
        if (actual.getPrioridad() == datos.prioridad()) {
            throw new IllegalArgumentException("La nueva prioridad es igual a la actual");
        }

        //6. Guardar la solicitud con la nueva prioridad y registrar el cambio
        SolicitudPrueba actualizada = solicitudes.guardar(actual.conPrioridad(datos.prioridad()));

        cambios.guardar(new CambioPrioridad(
                0,
                solicitudId,
                actual.getPrioridad(),
                datos.prioridad(),
                regla.getId(),
                datos.motivo().trim(),
                solicitante.nombre(),
                LocalDateTime.now()));

        return SolicitudPrioridad.de(actualizada);
    }

    @Override
    public List<CambioPrioridad> consultarHistorial(
            Usuario solicitante,
            long solicitudId) throws SinPermisoException {

        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        if (solicitudes.buscarPorId(solicitudId).isEmpty()) {
            throw new NoSuchElementException("Solicitud no encontrada");
        }

        return cambios.listarPorSolicitud(solicitudId);
    }
}

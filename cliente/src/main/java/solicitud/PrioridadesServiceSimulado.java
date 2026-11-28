package solicitud;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU28 - Servicio de prioridades en memoria para usar el cliente sin el servidor.
 * Aplica las reglas de D-038 y lanza las mismas excepciones que PrioridadesServiceHttp.
 */
public class PrioridadesServiceSimulado implements PrioridadesService {

    //Orden de la lista: prioridad (urgente, preferente, ordinaria), fecha de solicitud e id
    static final Comparator<SolicitudPrioridad> ORDEN =
            Comparator.comparing(SolicitudPrioridad::prioridad)
                    .thenComparing(SolicitudPrioridad::fechaSolicitud,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(SolicitudPrioridad::solicitudId);

    //Una solicitud guardada y si sigue pendiente de citacion
    private record Guardada(SolicitudPrioridad solicitud, boolean pendiente) { }

    //Una regla de CU37 y si esta activa
    private record Regla(ReglaPrioridadAplicable regla, boolean activa) { }

    private final Map<Long, Guardada> solicitudes = new LinkedHashMap<>();
    private final Map<Long, Regla> reglas = new LinkedHashMap<>();
    private final List<CambioPrioridad> historial = new ArrayList<>();
    private long siguienteCambio = 1;

    public PrioridadesServiceSimulado() { }

    /** Añade una solicitud; si no esta pendiente simula una solicitud ya citada, realizada o anulada. */
    public synchronized PrioridadesServiceSimulado conSolicitud(SolicitudPrioridad solicitud, boolean pendiente) {
        solicitudes.put(solicitud.solicitudId(), new Guardada(solicitud, pendiente));
        return this;
    }

    /** Añade una regla de prioridad de CU37, activa o retirada. */
    public synchronized PrioridadesServiceSimulado conRegla(ReglaPrioridadAplicable regla, boolean activa) {
        reglas.put(regla.id(), new Regla(regla, activa));
        return this;
    }

    //Datos de ejemplo para probar la pantalla sin servidor
    public static PrioridadesServiceSimulado conDatosDeEjemplo() {
        LocalDateTime hoy = LocalDateTime.now().withNano(0);
        return new PrioridadesServiceSimulado()
                .conRegla(new ReglaPrioridadAplicable(1, "Paciente oncológico",
                        "Pacientes en tratamiento oncológico activo", 1), true)
                .conRegla(new ReglaPrioridadAplicable(2, "Empeoramiento clínico",
                        "El médico informa de un empeoramiento", 2), true)
                .conRegla(new ReglaPrioridadAplicable(3, "Espera prolongada",
                        "Más de 60 días en lista de espera", 3), true)
                .conRegla(new ReglaPrioridadAplicable(4, "Regla antigua",
                        "Regla retirada por el administrador", 4), false)
                .conSolicitud(new SolicitudPrioridad(1, 101, 1, Prioridad.URGENTE, hoy.minusDays(1)), true)
                .conSolicitud(new SolicitudPrioridad(2, 102, 2, Prioridad.PREFERENTE, hoy.minusDays(4)), true)
                .conSolicitud(new SolicitudPrioridad(3, 103, 3, Prioridad.ORDINARIA, hoy.minusDays(70)), true)
                .conSolicitud(new SolicitudPrioridad(4, 104, 1, Prioridad.ORDINARIA, hoy.minusDays(10)), true)
                .conSolicitud(new SolicitudPrioridad(5, 105, 2, Prioridad.URGENTE, hoy.minusDays(2)), false);
    }

    @Override
    public synchronized List<SolicitudPrioridad> consultarPendientes(Usuario solicitante, Prioridad prioridad) {
        comprobarPermiso(solicitante);
        return solicitudes.values().stream()
                .filter(Guardada::pendiente)
                .map(Guardada::solicitud)
                .filter(s -> prioridad == null || s.prioridad() == prioridad)
                .sorted(ORDEN)
                .toList();
    }

    @Override
    public synchronized List<ReglaPrioridadAplicable> consultarReglasActivas(Usuario solicitante) {
        comprobarPermiso(solicitante);
        return reglas.values().stream()
                .filter(Regla::activa)
                .map(Regla::regla)
                .sorted(Comparator.comparingInt(ReglaPrioridadAplicable::orden))
                .toList();
    }

    @Override
    public synchronized SolicitudPrioridad cambiarPrioridad(Usuario solicitante, long solicitudId,
            DatosCambioPrioridad datos) {
        comprobarPermiso(solicitante);
        if (datos == null || datos.prioridad() == null
                || datos.motivo() == null || datos.motivo().isBlank()) {
            throw new IllegalArgumentException("Los datos del cambio de prioridad no son válidos.");
        }

        Guardada guardada = solicitudes.get(solicitudId);
        if (guardada == null) {
            throw new NoSuchElementException("No se ha encontrado la solicitud o la regla de prioridad.");
        }
        if (!guardada.pendiente()) {
            throw new IllegalStateException("La solicitud ya no está pendiente de citación, "
                    + "por lo que no se puede cambiar su prioridad.");
        }

        Regla regla = reglas.get(datos.reglaPrioridadId());
        if (regla == null) {
            throw new NoSuchElementException("No se ha encontrado la solicitud o la regla de prioridad.");
        }
        if (!regla.activa()) {
            throw new IllegalArgumentException("La regla de prioridad no está activa.");
        }

        SolicitudPrioridad actual = guardada.solicitud();
        if (actual.prioridad() == datos.prioridad()) {
            throw new IllegalArgumentException("La nueva prioridad debe ser distinta de la actual.");
        }

        //Solo cambia la prioridad; el resto de la solicitud se conserva (D-038)
        SolicitudPrioridad actualizada = new SolicitudPrioridad(actual.solicitudId(), actual.pacienteId(),
                actual.tipoPruebaId(), datos.prioridad(), actual.fechaSolicitud());
        solicitudes.put(solicitudId, new Guardada(actualizada, true));

        historial.add(new CambioPrioridad(siguienteCambio++, solicitudId, actual.prioridad(),
                datos.prioridad(), datos.reglaPrioridadId(), datos.motivo().trim(),
                solicitante.nombre(), LocalDateTime.now().withNano(0)));
        return actualizada;
    }

    @Override
    public synchronized List<CambioPrioridad> consultarHistorial(Usuario solicitante, long solicitudId) {
        comprobarPermiso(solicitante);
        if (!solicitudes.containsKey(solicitudId)) {
            throw new NoSuchElementException("No se ha encontrado la solicitud o la regla de prioridad.");
        }
        List<CambioPrioridad> cambios = new ArrayList<>();
        for (CambioPrioridad cambio : historial) {
            if (cambio.solicitudId() == solicitudId) cambios.add(0, cambio);
        }
        return cambios;
    }

    //Solo el personal administrativo gestiona prioridades (D-016)
    private static void comprobarPermiso(Usuario usuario) {
        if (usuario == null || usuario.rol() != Rol.PERSONAL_ADMINISTRATIVO) {
            throw new SecurityException("No tienes permiso para gestionar prioridades.");
        }
    }
}

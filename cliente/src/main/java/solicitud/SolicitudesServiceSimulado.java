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
import tipoprueba.TipoPrueba;

/**
 * Servicio de solicitudes en memoria para usar el cliente sin el servidor.
 * Aplica las mismas reglas que el servidor del CU13 (D-031 y D-036) y lanza
 * las mismas excepciones que SolicitudesServiceHttp.
 */
public class SolicitudesServiceSimulado implements SolicitudesService {

    //Orden del listado: primero por prioridad (urgente, preferente, ordinaria) y despues por fecha
    static final Comparator<SolicitudPrueba> ORDEN =
            Comparator.comparing(SolicitudPrueba::getPrioridad)
                    .thenComparing(SolicitudPrueba::getFechaSolicitud,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final List<TipoPrueba> tipos;
    private final Map<Long, SolicitudPrueba> solicitudes = new LinkedHashMap<>();
    private long siguienteId = 1;

    public SolicitudesServiceSimulado(List<TipoPrueba> tipos, List<SolicitudPrueba> iniciales) {
        this.tipos = tipos == null ? List.of() : List.copyOf(tipos);
        if (iniciales != null) {
            for (SolicitudPrueba solicitud : iniciales) {
                solicitudes.put(solicitud.getId(), solicitud);
                siguienteId = Math.max(siguienteId, solicitud.getId() + 1);
            }
        }
    }

    //Datos de ejemplo para probar la pantalla sin servidor
    public static SolicitudesServiceSimulado conDatosDeEjemplo() {
        List<TipoPrueba> tipos = List.of(
                new TipoPrueba(1, "Radiografía", "Radiografía simple", 15, false, "", true),
                new TipoPrueba(2, "TAC", "Tomografía computarizada", 30, true, "Ayuno de 6 horas", true),
                new TipoPrueba(3, "Resonancia magnética", "Resonancia", 45, false, "", true));

        LocalDateTime hoy = LocalDateTime.now().withNano(0);
        List<SolicitudPrueba> iniciales = List.of(
                new SolicitudPrueba(1, 101, 7, 1, "Dolor torácico", "Descartar neumonía",
                        Prioridad.URGENTE, hoy.minusDays(1), EstadoSolicitud.PENDIENTE_CITACION),
                new SolicitudPrueba(2, 102, 7, 2, "Cefalea persistente", "Descartar lesión",
                        Prioridad.PREFERENTE, hoy.minusDays(3), EstadoSolicitud.CITADA),
                new SolicitudPrueba(3, 101, 7, 3, "Control de lesión", "Seguimiento de la evolución",
                        Prioridad.ORDINARIA, hoy.minusDays(5), EstadoSolicitud.PENDIENTE_CITACION,
                        3, UnidadPeriodicidad.MESES, 4, null),
                new SolicitudPrueba(4, 103, 7, 1, "Traumatismo en rodilla", "Valorar fractura",
                        Prioridad.ORDINARIA, hoy.minusDays(10), EstadoSolicitud.REALIZADA),
                new SolicitudPrueba(5, 104, 7, 2, "Dolor abdominal", "Duplicada por error",
                        Prioridad.PREFERENTE, hoy.minusDays(2), EstadoSolicitud.ANULADA,
                        null, null, null, null, "Solicitud duplicada", hoy.minusDays(1)));

        return new SolicitudesServiceSimulado(tipos, iniciales);
    }

    @Override
    public List<TipoPrueba> consultarTiposPrueba(Usuario medico) {
        comprobarMedico(medico);
        return tipos;
    }

    @Override
    public synchronized SolicitudPrueba crearSolicitud(Usuario medico, SolicitudPrueba datos) {
        comprobarMedico(medico);
        if (datos == null) {
            throw new IllegalArgumentException("Faltan los datos de la solicitud");
        }
        //El servidor asigna el id, la fecha y el estado inicial (D-026)
        SolicitudPrueba creada = new SolicitudPrueba(siguienteId++, datos.getPacienteId(),
                datos.getMedicoId(), datos.getTipoPruebaId(), datos.getIndicacionClinica(),
                datos.getJustificacion(), datos.getPrioridad(), LocalDateTime.now().withNano(0),
                EstadoSolicitud.PENDIENTE_CITACION, datos.getIntervalo(), datos.getUnidad(),
                datos.getNumeroRepeticiones(), datos.getSolicitudOrigenId());
        solicitudes.put(creada.getId(), creada);
        return creada;
    }

    @Override
    public synchronized SolicitudPrueba consultarSolicitud(Usuario medico, long id) {
        comprobarMedico(medico);
        return buscar(id);
    }

    @Override
    public synchronized List<SolicitudPrueba> consultarSolicitudes(Usuario medico, FiltroSolicitudes filtro) {
        comprobarMedico(medico);
        FiltroSolicitudes criterios = filtro == null ? FiltroSolicitudes.vacio() : filtro;
        List<SolicitudPrueba> resultado = new ArrayList<>();
        for (SolicitudPrueba solicitud : solicitudes.values()) {
            if (criterios.cumple(solicitud)) resultado.add(solicitud);
        }
        resultado.sort(ORDEN);
        return resultado;
    }

    @Override
    public synchronized SolicitudPrueba modificarSolicitud(Usuario medico, long id,
            DatosModificacionSolicitud datos) {
        comprobarMedico(medico);
        SolicitudPrueba actual = buscarGestionable(id);
        validarModificacion(actual, datos);

        //Solo cambian los datos clinicos y la periodicidad; el resto se conserva
        SolicitudPrueba modificada = new SolicitudPrueba(actual.getId(), actual.getPacienteId(),
                actual.getMedicoId(), actual.getTipoPruebaId(), datos.indicacionClinica().trim(),
                datos.justificacion().trim(), datos.prioridad(), actual.getFechaSolicitud(),
                actual.getEstado(),
                actual.esSeguimiento() ? datos.intervalo() : null,
                actual.esSeguimiento() ? datos.unidad() : null,
                actual.esSeguimiento() ? datos.numeroRepeticiones() : null,
                actual.getSolicitudOrigenId());
        solicitudes.put(id, modificada);
        return modificada;
    }

    @Override
    public synchronized SolicitudPrueba anularSolicitud(Usuario medico, long id, String motivo) {
        comprobarMedico(medico);
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la anulacion es obligatorio");
        }
        SolicitudPrueba actual = buscarGestionable(id);

        SolicitudPrueba anulada = new SolicitudPrueba(actual.getId(), actual.getPacienteId(),
                actual.getMedicoId(), actual.getTipoPruebaId(), actual.getIndicacionClinica(),
                actual.getJustificacion(), actual.getPrioridad(), actual.getFechaSolicitud(),
                EstadoSolicitud.ANULADA, actual.getIntervalo(), actual.getUnidad(),
                actual.getNumeroRepeticiones(), actual.getSolicitudOrigenId(),
                motivo.trim(), LocalDateTime.now().withNano(0));
        solicitudes.put(id, anulada);
        return anulada;
    }

    // ---- Reglas ----

    private static void comprobarMedico(Usuario usuario) {
        if (usuario == null || usuario.rol() != Rol.MEDICO) {
            throw new SecurityException("No tienes permiso para realizar esta operacion.");
        }
    }

    private SolicitudPrueba buscar(long id) {
        SolicitudPrueba solicitud = solicitudes.get(id);
        if (solicitud == null) {
            throw new NoSuchElementException("No se ha encontrado la solicitud.");
        }
        return solicitud;
    }

    //D-031: solo se gestiona una solicitud pendiente de citacion
    private SolicitudPrueba buscarGestionable(long id) {
        SolicitudPrueba solicitud = buscar(id);
        if (!solicitud.sePuedeGestionar()) {
            throw new IllegalStateException("La solicitud ya no esta pendiente de citacion, "
                    + "por lo que no se puede modificar ni anular.");
        }
        return solicitud;
    }

    private static void validarModificacion(SolicitudPrueba actual, DatosModificacionSolicitud datos) {
        if (datos == null || estaVacio(datos.indicacionClinica()) || estaVacio(datos.justificacion())
                || datos.prioridad() == null) {
            throw new IllegalArgumentException("Los datos de la solicitud no son validos. "
                    + "Comprueba los campos obligatorios.");
        }
        boolean traePeriodicidad = datos.intervalo() != null || datos.unidad() != null
                || datos.numeroRepeticiones() != null;
        if (!actual.esSeguimiento()) {
            if (traePeriodicidad) {
                throw new IllegalArgumentException("Solo se puede indicar la periodicidad en un seguimiento.");
            }
            return;
        }
        //D-030: la periodicidad se informa completa y con valores mayores que 0
        if (datos.intervalo() == null || datos.unidad() == null || datos.numeroRepeticiones() == null
                || datos.intervalo() < 1 || datos.numeroRepeticiones() < 1) {
            throw new IllegalArgumentException("La periodicidad del seguimiento no es valida.");
        }
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}

package solicitud;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import paciente.ControlAcceso;
import paciente.Paciente;
import paciente.PacientesRepositorio;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

import tipoprueba.RepositorioTiposPrueba;
import tipoprueba.TipoPrueba;

/**
 * Implementacion del servicio de solicitudes.
 * CU11 - Prescribir pruebas diagnosticas. CU12 - Pruebas de seguimiento. CU13 - Gestionar solicitudes.
 */
public class SolicitudesServiceImpl implements SolicitudesService {

    private final PacientesRepositorio pacientes;
    private final RepositorioTiposPrueba tiposPrueba;
    private final SolicitudesRepositorio solicitudes;

    //Constructor
    public SolicitudesServiceImpl(
            PacientesRepositorio pacientes,
            RepositorioTiposPrueba tiposPrueba,
            SolicitudesRepositorio solicitudes) {

        if (pacientes == null || tiposPrueba == null
                || solicitudes == null) {
            throw new IllegalArgumentException(
                "Los repositorios son obligatorios");
        }

        this.pacientes = pacientes;
        this.tiposPrueba = tiposPrueba;
        this.solicitudes = solicitudes;
    }

    //Devuelve los tipos de prueba activos
    @Override
    public List<TipoPrueba> consultarTiposPrueba() {

        List<TipoPrueba> disponibles = new ArrayList<>();

        for (TipoPrueba tipo : tiposPrueba.todos()) {
            if (tipo.isActivo()) {
                disponibles.add(tipo);
            }
        }

        return disponibles;
    }

    //Busca una solicitud por su identificador
    @Override
    public Optional<SolicitudPrueba> consultarSolicitud(long id) {
        return solicitudes.buscarPorId(id);
    }

    // Registra una nueva prescripcion
    @Override
    public SolicitudPrueba crearSolicitud(
            Usuario solicitante,
            SolicitudPrueba datos) throws SinPermisoException {

        //1. Comprobar el rol
        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        //2. Validar los datos obligatorios
        if (datos == null) {
            throw new IllegalArgumentException(
                "La solicitud no puede estar vacia");
        }

        if (datos.getId() != 0) {
            throw new IllegalArgumentException(
                "Una solicitud nueva debe tener id 0");
        }

        if (datos.getPacienteId() <= 0
                || datos.getMedicoId() <= 0
                || datos.getTipoPruebaId() <= 0) {
            throw new IllegalArgumentException(
                "Los identificadores deben ser positivos");
        }

        if (datos.getIndicacionClinica() == null
                || datos.getIndicacionClinica().isBlank()) {
            throw new IllegalArgumentException(
                "Falta la indicacion clinica");
        }

        if (datos.getJustificacion() == null
                || datos.getJustificacion().isBlank()) {
            throw new IllegalArgumentException(
                "Falta la justificacion");
        }

        if (datos.getPrioridad() == null) {
            throw new IllegalArgumentException(
                "Falta la prioridad");
        }

        //CU12: comprobar el formato de la periodicidad, si la hay
        validarPeriodicidad(datos);

        //3. Comprobar el paciente
        Paciente paciente = pacientes.buscarPorId(
                datos.getPacienteId()).orElseThrow(
                () -> new NoSuchElementException(
                    "Paciente no encontrado"));

        if (!paciente.isActivo()) {
            throw new IllegalArgumentException(
                "El paciente esta dado de baja");
        }

        //4. Comprobar el tipo de prueba
        TipoPrueba tipo = tiposPrueba.buscarPorId(
                datos.getTipoPruebaId()).orElseThrow(
                () -> new NoSuchElementException(
                    "Tipo de prueba no encontrado"));

        if (!tipo.isActivo()) {
            throw new IllegalArgumentException(
                "El tipo de prueba esta dado de baja");
        }

        //CU12: la prescripcion original, si se indica, debe existir y ser del mismo paciente
        if (datos.getSolicitudOrigenId() != null) {

            SolicitudPrueba origen = solicitudes.buscarPorId(
                    datos.getSolicitudOrigenId()).orElseThrow(
                    () -> new NoSuchElementException(
                        "Prescripcion original no encontrada"));

            if (origen.getPacienteId() != datos.getPacienteId()) {
                throw new IllegalArgumentException(
                    "La prescripcion original es de otro paciente");
            }
        }

        //5. Crear la solicitud
        SolicitudPrueba nueva = new SolicitudPrueba(
                0,
                datos.getPacienteId(),
                datos.getMedicoId(),
                datos.getTipoPruebaId(),
                datos.getIndicacionClinica().trim(),
                datos.getJustificacion().trim(),
                datos.getPrioridad(),
                LocalDateTime.now(),
                EstadoSolicitud.PENDIENTE_CITACION,
                datos.getIntervalo(),
                datos.getUnidad(),
                datos.getNumeroRepeticiones(),
                datos.getSolicitudOrigenId()
        );

        //6. Guardar y devolver la solicitud
        return solicitudes.guardar(nueva);
    }

    // ---- CU13: gestion de solicitudes (D-031 y D-032) ----

    //Orden del listado: primero por prioridad (urgente, preferente, ordinaria) y despues por fecha de solicitud
    private static final Comparator<SolicitudPrueba> ORDEN =
            Comparator.comparing(SolicitudPrueba::getPrioridad)
                    .thenComparing(SolicitudPrueba::getFechaSolicitud,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(SolicitudPrueba::getId);

    //Devuelve las solicitudes que cumplen el filtro, ordenadas
    @Override
    public List<SolicitudPrueba> consultarSolicitudes(
            Usuario solicitante,
            FiltroSolicitudes filtro) throws SinPermisoException {

        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        FiltroSolicitudes criterios = filtro == null
                ? FiltroSolicitudes.vacio()
                : filtro;

        List<SolicitudPrueba> resultado = new ArrayList<>();

        for (SolicitudPrueba solicitud : solicitudes.todas()) {
            if (criterios.cumple(solicitud)) {
                resultado.add(solicitud);
            }
        }

        resultado.sort(ORDEN);

        return resultado;
    }

    //Modifica los datos clinicos de una solicitud pendiente de citacion
    @Override
    public SolicitudPrueba modificarSolicitud(
            Usuario solicitante,
            long id,
            DatosModificacionSolicitud datos) throws SinPermisoException {

        //1. Comprobar el rol
        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        //2. Validar los datos clinicos
        if (datos == null) {
            throw new IllegalArgumentException(
                "Faltan los datos de la modificacion");
        }

        if (datos.indicacionClinica() == null
                || datos.indicacionClinica().isBlank()) {
            throw new IllegalArgumentException(
                "Falta la indicacion clinica");
        }

        if (datos.justificacion() == null
                || datos.justificacion().isBlank()) {
            throw new IllegalArgumentException(
                "Falta la justificacion");
        }

        if (datos.prioridad() == null) {
            throw new IllegalArgumentException(
                "Falta la prioridad");
        }

        //3. La solicitud debe existir y estar pendiente de citacion
        SolicitudPrueba actual = buscarGestionable(id);

        //4. La periodicidad solo se puede indicar en un seguimiento, y entonces completa (D-030)
        boolean traePeriodicidad = datos.intervalo() != null
                || datos.unidad() != null
                || datos.numeroRepeticiones() != null;

        if (!actual.esSeguimiento() && traePeriodicidad) {
            throw new IllegalArgumentException(
                "Solo se puede indicar la periodicidad en un seguimiento");
        }

        if (actual.esSeguimiento()) {
            comprobarPeriodicidad(datos.intervalo(),
                    datos.unidad(), datos.numeroRepeticiones(), true);
        }

        //5. Solo cambian los datos clinicos y la periodicidad; el resto se conserva
        SolicitudPrueba modificada = new SolicitudPrueba(
                actual.getId(),
                actual.getPacienteId(),
                actual.getMedicoId(),
                actual.getTipoPruebaId(),
                datos.indicacionClinica().trim(),
                datos.justificacion().trim(),
                datos.prioridad(),
                actual.getFechaSolicitud(),
                actual.getEstado(),
                actual.esSeguimiento() ? datos.intervalo() : null,
                actual.esSeguimiento() ? datos.unidad() : null,
                actual.esSeguimiento() ? datos.numeroRepeticiones() : null,
                actual.getSolicitudOrigenId()
        );

        return solicitudes.guardar(modificada);
    }

    //Anula una solicitud pendiente de citacion
    @Override
    public SolicitudPrueba anularSolicitud(
            Usuario solicitante,
            long id,
            String motivo) throws SinPermisoException {

        //1. Comprobar el rol
        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        //2. El motivo es obligatorio
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                "Falta el motivo de la anulacion");
        }

        //3. La solicitud debe existir y estar pendiente de citacion
        SolicitudPrueba actual = buscarGestionable(id);

        //4. Pasa a ANULADA guardando el motivo y la fecha
        SolicitudPrueba anulada = new SolicitudPrueba(
                actual.getId(),
                actual.getPacienteId(),
                actual.getMedicoId(),
                actual.getTipoPruebaId(),
                actual.getIndicacionClinica(),
                actual.getJustificacion(),
                actual.getPrioridad(),
                actual.getFechaSolicitud(),
                EstadoSolicitud.ANULADA,
                actual.getIntervalo(),
                actual.getUnidad(),
                actual.getNumeroRepeticiones(),
                actual.getSolicitudOrigenId(),
                motivo.trim(),
                LocalDateTime.now()
        );

        return solicitudes.guardar(anulada);
    }

    //D-031: busca la solicitud y comprueba que se puede modificar o anular
    private SolicitudPrueba buscarGestionable(long id) {

        SolicitudPrueba solicitud = solicitudes.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException(
                    "Solicitud no encontrada"));

        if (!solicitud.sePuedeGestionar()) {
            throw new SolicitudNoGestionableException(
                "La solicitud no esta pendiente de citacion");
        }

        return solicitud;
    }

    //CU12 (D-030): la periodicidad se informa completa o no se informa; intervalo y repeticiones mayores que 0
    private static void validarPeriodicidad(SolicitudPrueba datos) {

        boolean algunDato = datos.getIntervalo() != null
                || datos.getUnidad() != null
                || datos.getNumeroRepeticiones() != null;

        comprobarPeriodicidad(datos.getIntervalo(), datos.getUnidad(),
                datos.getNumeroRepeticiones(), algunDato);

        if (datos.getSolicitudOrigenId() != null
                && datos.getSolicitudOrigenId() <= 0) {
            throw new IllegalArgumentException(
                "La prescripcion original debe tener un identificador positivo");
        }
    }

    //Si la periodicidad es obligatoria, debe estar completa y con intervalo y repeticiones mayores que 0 (D-030)
    private static void comprobarPeriodicidad(Integer intervalo,
            UnidadPeriodicidad unidad, Integer repeticiones, boolean obligatoria) {

        if (!obligatoria) {
            return;
        }

        if (intervalo == null || unidad == null || repeticiones == null) {
            throw new IllegalArgumentException(
                "La periodicidad esta incompleta");
        }

        if (intervalo < 1) {
            throw new IllegalArgumentException(
                "El intervalo debe ser mayor que 0");
        }

        if (repeticiones < 1) {
            throw new IllegalArgumentException(
                "El numero de repeticiones debe ser mayor que 0");
        }
    }
}

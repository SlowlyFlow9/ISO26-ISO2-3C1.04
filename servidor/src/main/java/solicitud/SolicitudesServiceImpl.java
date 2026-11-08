package solicitud;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * CU11 - Prescribir pruebas diagnosticas.
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
                EstadoSolicitud.PENDIENTE_CITACION
        );

        //6. Guardar y devolver la solicitud
        return solicitudes.guardar(nueva);
    }
}

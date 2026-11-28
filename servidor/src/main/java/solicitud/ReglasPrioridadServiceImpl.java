package solicitud;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ReglasPrioridadServiceImpl
        implements ReglasPrioridadService {

    private final ReglasPrioridadRepositorio repositorio;

    public ReglasPrioridadServiceImpl(
            ReglasPrioridadRepositorio repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "El repositorio es obligatorio");
        }
        this.repositorio = repositorio;
    }

    @Override
    public synchronized ReglaPrioridad crear(
            Usuario solicitante,
            DatosReglaPrioridad datos)
            throws SinPermisoException,
            DatosReglaPrioridadInvalidosException,
            ReglaPrioridadDuplicadaException {
        exigirAdministrador(solicitante);
        validar(datos);
        comprobarUnicidad(datos, 0);
        LocalDateTime ahora = LocalDateTime.now();
        return repositorio.guardar(new ReglaPrioridad(
                0,
                datos.getNombre().trim(),
                recortar(datos.getDescripcion()),
                datos.getOrden(),
                true,
                solicitante.nombre(),
                ahora));
    }

    @Override
    public List<ReglaPrioridad> listar(Usuario solicitante)
            throws SinPermisoException {
        exigirAdministrador(solicitante);
        return repositorio.todas().stream()
                .sorted(Comparator.comparingInt(ReglaPrioridad::getOrden))
                .toList();
    }

    @Override
    public ReglaPrioridad consultar(Usuario solicitante, long id)
            throws SinPermisoException,
            ReglaPrioridadNoEncontradaException {
        exigirAdministrador(solicitante);
        return obtener(id);
    }

    @Override
    public synchronized ReglaPrioridad modificar(
            Usuario solicitante,
            long id,
            DatosReglaPrioridad datos)
            throws SinPermisoException,
            DatosReglaPrioridadInvalidosException,
            ReglaPrioridadDuplicadaException,
            ReglaPrioridadNoEncontradaException {
        exigirAdministrador(solicitante);
        ReglaPrioridad actual = obtener(id);
        validar(datos);
        comprobarUnicidad(datos, id);

        ReglaPrioridad modificada = actual.conDatos(
                datos.getNombre().trim(),
                recortar(datos.getDescripcion()),
                datos.getOrden(),
                solicitante.nombre(),
                LocalDateTime.now());
        return repositorio.guardar(modificada);
    }

    @Override
    public synchronized void desactivar(Usuario solicitante, long id)
            throws SinPermisoException,
            ReglaPrioridadNoEncontradaException {
        exigirAdministrador(solicitante);
        ReglaPrioridad actual = obtener(id);
        if (!actual.isActivo()) {
            return;
        }
        repositorio.guardar(actual.desactivada(
                solicitante.nombre(), LocalDateTime.now()));
    }

    private static void exigirAdministrador(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);
    }

    private ReglaPrioridad obtener(long id)
            throws ReglaPrioridadNoEncontradaException {
        return repositorio.buscarPorId(id).orElseThrow(
                () -> new ReglaPrioridadNoEncontradaException(
                        "No existe la regla de prioridad con id " + id));
    }

    private void comprobarUnicidad(
            DatosReglaPrioridad datos,
            long idPropio)
            throws ReglaPrioridadDuplicadaException {
        ReglaPrioridad mismoNombre = repositorio
                .buscarPorNombre(datos.getNombre())
                .orElse(null);
        if (mismoNombre != null && mismoNombre.getId() != idPropio) {
            throw new ReglaPrioridadDuplicadaException(
                    "Ya existe una regla con ese nombre");
        }

        ReglaPrioridad mismoOrden = repositorio
                .buscarPorOrden(datos.getOrden())
                .orElse(null);
        if (mismoOrden != null && mismoOrden.getId() != idPropio) {
            throw new ReglaPrioridadDuplicadaException(
                    "Ya existe una regla con ese orden");
        }
    }

    private static void validar(DatosReglaPrioridad datos)
            throws DatosReglaPrioridadInvalidosException {
        if (datos == null || vacio(datos.getNombre())) {
            throw new DatosReglaPrioridadInvalidosException(
                    "El nombre es obligatorio");
        }
        if (datos.getOrden() <= 0) {
            throw new DatosReglaPrioridadInvalidosException(
                    "El orden debe ser mayor que cero");
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private static String recortar(String texto) {
        return texto == null ? "" : texto.trim();
    }

}

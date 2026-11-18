package tipoprueba;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Implementación del servicio: reglas de negocio, permisos y auditoría del CU33. */
public class TiposPruebaServiceImpl implements TiposPruebaService {

    private final RepositorioTiposPrueba repositorio;
    private final ContextoUsuario contexto;
    private final ConsultaPruebasPendientes pendientes;
    private final RegistroAuditoria auditoria;

    public TiposPruebaServiceImpl(RepositorioTiposPrueba repositorio, ContextoUsuario contexto,
                                  ConsultaPruebasPendientes pendientes, RegistroAuditoria auditoria) {
        this.repositorio = repositorio;
        this.contexto = contexto;
        this.pendientes = pendientes;
        this.auditoria = auditoria;
    }

    @Override
    public synchronized TipoPrueba crear(DatosTipoPrueba datos) throws ServicioException {
        exigirAdministrador();
        validar(datos);
        comprobarNombreUnico(datos.getNombre(), 0);
        TipoPrueba creado = repositorio.guardar(nuevo(0, datos, true));
        auditar(EventoAuditoria.Accion.CREAR, creado.getId(), "nombre=" + creado.getNombre());
        return creado;
    }

    @Override
    public TipoPrueba consultar(long id) throws ServicioException {
        return obtener(id);
    }

    @Override
    public List<TipoPrueba> listar(FiltroTipoPrueba filtro) {
        FiltroTipoPrueba f = filtro == null ? new FiltroTipoPrueba("", null) : filtro;
        String texto = f.getTexto().toLowerCase(Locale.ROOT);
        List<TipoPrueba> resultado = new ArrayList<>();
        for (TipoPrueba t : repositorio.todos()) {
            boolean coincideNombre = t.getNombre().toLowerCase(Locale.ROOT).contains(texto);
            boolean coincideEstado = f.getEstado() == FiltroTipoPrueba.Estado.TODOS
                    || (f.getEstado() == FiltroTipoPrueba.Estado.ACTIVOS) == t.isActivo();
            if (coincideNombre && coincideEstado) {
                resultado.add(t);
            }
        }
        resultado.sort(Comparator.comparing(t -> t.getNombre().toLowerCase(Locale.ROOT)));
        return resultado;
    }

    @Override
    public synchronized TipoPrueba modificar(long id, DatosTipoPrueba datos) throws ServicioException {
        exigirAdministrador();
        TipoPrueba actual = obtener(id);
        validar(datos);
        comprobarNombreUnico(datos.getNombre(), id);
        TipoPrueba modificado = repositorio.guardar(nuevo(id, datos, actual.isActivo()));
        auditar(EventoAuditoria.Accion.MODIFICAR, id,
                "nombre: " + actual.getNombre() + " -> " + modificado.getNombre());
        return modificado;
    }

    @Override
    public synchronized void desactivar(long id, boolean confirmado) throws ServicioException {
        exigirAdministrador();
        TipoPrueba actual = obtener(id);
        if (!actual.isActivo()) {
            return; // ya está dado de baja: no hay nada que hacer
        }
        int numPendientes = pendientes.contarPendientes(id);
        if (numPendientes > 0 && !confirmado) {
            throw new PruebasPendientesException(numPendientes);
        }
        repositorio.guardar(actual.conActivo(false)); // baja lógica, sin borrado físico (D-008)
        auditar(EventoAuditoria.Accion.DESACTIVAR, id,
                "nombre=" + actual.getNombre() + ", pendientes=" + numPendientes);
    }

    @Override
    public int contarPruebasPendientes(long id) throws ServicioException {
        obtener(id);
        return pendientes.contarPendientes(id);
    }

    // ---------- reglas internas ----------

    private void exigirAdministrador() throws PermisoDenegadoException {
        if (contexto.rol() != Rol.ADMINISTRADOR) {
            throw new PermisoDenegadoException(
                    "Solo el Administrador del Sistema puede crear, modificar o desactivar tipos de prueba.");
        }
    }

    private TipoPrueba obtener(long id) throws TipoPruebaNoEncontradoException {
        return repositorio.buscarPorId(id).orElseThrow(
                () -> new TipoPruebaNoEncontradoException("No existe el tipo de prueba con id " + id + "."));
    }

    private void validar(DatosTipoPrueba d) throws DatosInvalidosException {
        if (d == null || vacio(d.getNombre())) {
            throw new DatosInvalidosException("El nombre es obligatorio.");
        }
        if (d.getDuracionEstimada() <= 0) {
            throw new DatosInvalidosException("La duración estimada debe ser mayor que 0 minutos.");
        }
        if (d.isRequierePreparacion() && vacio(d.getInstruccionesPaciente())) {
            throw new DatosInvalidosException(
                    "Si el tipo requiere preparación, las instrucciones al paciente son obligatorias.");
        }
    }

    /** idPropio = id del tipo que se modifica (0 si es un alta), para no chocar consigo mismo. */
    private void comprobarNombreUnico(String nombre, long idPropio) throws NombreDuplicadoException {
        java.util.Optional<TipoPrueba> existente = repositorio.buscarPorNombre(nombre);
        if (existente.isPresent() && existente.get().getId() != idPropio) {
            throw new NombreDuplicadoException(
                    "Ya existe un tipo de prueba con el nombre \"" + nombre.trim() + "\".");
        }
    }

    private TipoPrueba nuevo(long id, DatosTipoPrueba d, boolean activo) {
        return new TipoPrueba(id, d.getNombre().trim(), recortar(d.getDescripcion()), d.getDuracionEstimada(),
                d.isRequierePreparacion(), d.isRequierePreparacion() ? recortar(d.getInstruccionesPaciente()) : "",
                activo);
    }

    private void auditar(EventoAuditoria.Accion accion, long idTipo, String detalle) {
        auditoria.registrar(new EventoAuditoria(Instant.now(), contexto.usuario(), accion, idTipo, detalle));
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String recortar(String s) {
        return s == null ? "" : s.trim();
    }
}

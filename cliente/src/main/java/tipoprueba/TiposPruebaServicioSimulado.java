package tipoprueba;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Implementación en memoria para desarrollar y probar la interfaz sin servidor. */
public class TiposPruebaServicioSimulado implements TiposPruebaService {
    private final List<TipoPrueba> datos = new ArrayList<>();
    private long siguienteId = 1;

    public TiposPruebaServicioSimulado() {
        try {
            crear(new DatosTipoPrueba("Radiografía de tórax", "Rx convencional", 15, false, ""));
            crear(new DatosTipoPrueba("Resonancia cerebral", "RM craneal", 45, true, "Retirar objetos metálicos"));
            crear(new DatosTipoPrueba("Ecografía abdominal", "US abdominal", 30, true, "Acudir en ayunas"));
        } catch (ServicioException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public TipoPrueba crear(DatosTipoPrueba d) throws ServicioException {
        comprobarNombre(d.getNombre(), -1);
        TipoPrueba t = new TipoPrueba(siguienteId++, d.getNombre(), d.getDescripcion(),
                d.getDuracionEstimada(), d.isRequierePreparacion(), d.getInstruccionesPaciente(), true);
        datos.add(t);
        return t;
    }

    @Override
    public TipoPrueba consultar(long id) throws ServicioException {
        return datos.get(indice(id));
    }

    @Override
    public List<TipoPrueba> listar(FiltroTipoPrueba f) {
        List<TipoPrueba> res = new ArrayList<>();
        for (TipoPrueba t : datos) {
            boolean texto = t.getNombre().toLowerCase(Locale.ROOT).contains(f.getTexto().toLowerCase(Locale.ROOT));
            boolean estado = f.getEstado() == FiltroTipoPrueba.Estado.TODOS
                    || (f.getEstado() == FiltroTipoPrueba.Estado.ACTIVOS) == t.isActivo();
            if (texto && estado) {
                res.add(t);
            }
        }
        return res;
    }

    @Override
    public TipoPrueba modificar(long id, DatosTipoPrueba d) throws ServicioException {
        int i = indice(id);
        comprobarNombre(d.getNombre(), id);
        TipoPrueba t = new TipoPrueba(id, d.getNombre(), d.getDescripcion(), d.getDuracionEstimada(),
                d.isRequierePreparacion(), d.getInstruccionesPaciente(), datos.get(i).isActivo());
        datos.set(i, t);
        return t;
    }

    @Override
    public void desactivar(long id) throws ServicioException {
        int i = indice(id);
        TipoPrueba t = datos.get(i);
        datos.set(i, new TipoPrueba(id, t.getNombre(), t.getDescripcion(), t.getDuracionEstimada(),
                t.isRequierePreparacion(), t.getInstruccionesPaciente(), false));
    }

    @Override
    public int contarPruebasPendientes(long id) {
        return id == 2 ? 3 : 0; // simulado para poder ver el aviso
    }

    private void comprobarNombre(String nombre, long idExcluido) throws NombreDuplicadoException {
        for (TipoPrueba t : datos) {
            if (t.getId() != idExcluido && t.getNombre().equalsIgnoreCase(nombre)) {
                throw new NombreDuplicadoException("Ya existe un tipo de prueba con el nombre \"" + nombre + "\".");
            }
        }
    }

    private int indice(long id) throws ServicioException {
        for (int i = 0; i < datos.size(); i++) {
            if (datos.get(i).getId() == id) {
                return i;
            }
        }
        throw new ServicioException("No existe el tipo de prueba solicitado.");
    }
}

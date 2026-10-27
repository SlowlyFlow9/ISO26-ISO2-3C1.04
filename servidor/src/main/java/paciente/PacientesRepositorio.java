package paciente;

import java.util.List;
import java.util.Optional;

/** Almacenamiento de pacientes (D-024). CU23 y CU24 lo reutilizan. */
public interface PacientesRepositorio {

    /** Guarda el paciente; si no tiene id, le asigna uno nuevo. Devuelve el paciente guardado. */
    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorId(long id);

    /** Búsqueda por DNI/NIE sin distinguir mayúsculas. */
    Optional<Paciente> buscarPorDni(String dni);

    /** Búsqueda por tarjeta sanitaria sin distinguir mayúsculas. */
    Optional<Paciente> buscarPorTarjeta(String tarjeta);

        /** Pacientes activos cuyo nombre, apellidos o DNI contienen el texto, sin distinguir mayúsculas. */
    List<Paciente> buscar(String criterio);

    void registrarModificacion(RegistroModificacion registro);

    List<RegistroModificacion> modificaciones(long pacienteId);
}

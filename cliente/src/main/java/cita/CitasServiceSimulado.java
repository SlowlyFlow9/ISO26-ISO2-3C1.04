package cita;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasServiceSimulado
        implements CitasService {

    private final List<Cita> citas =
            new ArrayList<>();

    private final Map<String, Long> pacientesPorUsuario =
            new HashMap<>();

    private long siguienteId = 1;

    //CU04: antelación mínima de cancelación (D-034, valor inicial 24 h)
    private int cancelacionMinimaHoras = 24;
    private Clock reloj = Clock.systemDefaultZone();

    //CU03: huecos libres del servicio simulado y ventana de reserva (D-034)
    private final List<HuecoLibre> huecos = new ArrayList<>();
    private int antelacionMinimaHoras = 0;
    private int antelacionMaximaDias = 90;

    public void vincularPaciente(
            String nombreUsuario,
            long pacienteId) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()
                || pacienteId <= 0) {

            throw new IllegalArgumentException(
                    "La vinculación de paciente no es válida");
        }

        pacientesPorUsuario.put(
                nombreUsuario,
                pacienteId);
    }

    public Cita agregarCita(Cita datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "La cita es obligatoria");
        }

        Cita almacenada = datos.copia();

        if (almacenada.getId() == null) {
            almacenada.setId(siguienteId++);
        }

        citas.add(almacenada);

        return almacenada.copia();
    }

    @Override
    public List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        exigirPaciente(solicitante);

        long pacienteId =
                obtenerPacienteId(solicitante);

        return citas.stream()
                .filter(cita ->
                        cita.getPacienteId()
                                == pacienteId)
                .map(Cita::copia)
                .toList();
    }

    @Override
    public Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException {

        exigirPaciente(solicitante);

        long pacienteId =
                obtenerPacienteId(solicitante);

        for (Cita cita : citas) {

            if (cita.getId() == citaId
                    && cita.getPacienteId()
                            == pacienteId) {

                return cita.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id "
                        + citaId);
    }

    private long obtenerPacienteId(
            Usuario usuario)
            throws PacienteNoVinculadoException {

        Long pacienteId =
                pacientesPorUsuario.get(
                        usuario.nombre());

        if (pacienteId == null) {
            throw new PacienteNoVinculadoException(
                    "El usuario no está vinculado "
                            + "a ningún paciente");
        }

        return pacienteId;
    }

    private static void exigirPaciente(
            Usuario usuario)
            throws SinPermisoException {

        if (usuario == null
                || usuario.rol() != Rol.PACIENTE) {

            throw new SinPermisoException(
                    "Esta operación solo está permitida "
                            + "al rol PACIENTE");
        }
    }

    //confirma una cita propia y actualiza el estado almacenado en el servicio simulado
    @Override
    public synchronized Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException {

        // Reutilizamos las comprobaciones de CU01: rol, vinculación y propiedad de la cita.
        Cita actual = consultarMiCita(
                solicitante, citaId);

        if (actual.getEstado() != EstadoCita.PROGRAMADA) {
            throw new CitaNoConfirmableException(
                    "La cita " + citaId
                    + " no se puede confirmar porque está "
                    + actual.getEstado());
        }

        // Actualizamos la cita guardada, no solo la copia obtenida durante la consulta.
        for (Cita almacenada : citas) {

            if (almacenada.getId() != null
                    && almacenada.getId() == citaId) {

                almacenada.setEstado(
                        EstadoCita.CONFIRMADA);

                return almacenada.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id " + citaId);
    }

    //CU04: cambia el plazo mínimo de cancelación del servicio simulado
    public void fijarCancelacionMinimaHoras(int horas) {
        cancelacionMinimaHoras = horas;
    }

    //CU04: cambia el reloj del servicio simulado (para las pruebas)
    public void fijarReloj(Clock nuevoReloj) {
        reloj = nuevoReloj;
    }

    //CU04: cancela una cita propia con las mismas reglas que el servidor (D-038)
    @Override
    public synchronized Cita cancelarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            CancelacionFueraDePlazoException {

        // Reutilizamos las comprobaciones de CU01: rol, vinculación y propiedad de la cita.
        Cita actual = consultarMiCita(
                solicitante, citaId);

        if (actual.getEstado() != EstadoCita.PROGRAMADA
                && actual.getEstado() != EstadoCita.CONFIRMADA) {
            throw new CitaNoModificableException(
                    "La cita " + citaId + " está "
                    + actual.getEstado()
                    + " y no se puede modificar");
        }

        if (actual.getFechaHora() != null
                && LocalDateTime.now(reloj).isAfter(
                        actual.getFechaHora().minusHours(
                                cancelacionMinimaHoras))) {
            throw new CancelacionFueraDePlazoException(
                    "La cita solo se puede cancelar con al menos "
                    + cancelacionMinimaHoras
                    + " horas de antelación");
        }

        // Actualizamos la cita guardada, no solo la copia obtenida durante la consulta.
        for (Cita almacenada : citas) {

            if (almacenada.getId() != null
                    && almacenada.getId() == citaId) {

                almacenada.setEstado(
                        EstadoCita.CANCELADA);

                return almacenada.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id " + citaId);
    }

    //CU03: añade un hueco al servicio simulado
    public void agregarHuecoLibre(HuecoLibre hueco) {
        huecos.add(hueco);
    }

    //CU03: cambia la antelación mínima y la ventana máxima de reserva
    public void fijarVentana(int minimaHoras, int maximaDias) {
        antelacionMinimaHoras = minimaHoras;
        antelacionMaximaDias = maximaDias;
    }

    //CU03: lista los huecos sin cita activa que cumplen la ventana de reserva
    @Override
    public synchronized List<HuecoLibre> listarHuecosLibres(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        exigirPaciente(solicitante);
        obtenerPacienteId(solicitante);

        LocalDateTime ahora = LocalDateTime.now(reloj);

        return huecos.stream()
                .filter(hueco -> !ocupado(hueco.id()))
                .filter(hueco -> !hueco.fechaHora().isBefore(
                        ahora.plusHours(antelacionMinimaHoras))
                        && !hueco.fechaHora().isAfter(
                        ahora.plusDays(antelacionMaximaDias)))
                .sorted(java.util.Comparator
                        .comparing(HuecoLibre::fechaHora)
                        .thenComparingLong(HuecoLibre::id))
                .toList();
    }

    //CU03: cambia una cita propia a otro hueco con las mismas reglas que el servidor (D-039)
    @Override
    public synchronized Cita cambiarMiCita(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoNoDisponibleException,
            CambioFueraDePoliticaException {

        // Reutilizamos las comprobaciones de CU01: rol, vinculación y propiedad de la cita.
        Cita actual = consultarMiCita(
                solicitante, citaId);

        if (actual.getEstado() != EstadoCita.PROGRAMADA
                && actual.getEstado() != EstadoCita.CONFIRMADA) {
            throw new CitaNoModificableException(
                    "La cita " + citaId + " está "
                    + actual.getEstado()
                    + " y no se puede modificar");
        }

        HuecoLibre nuevo = huecos.stream()
                .filter(hueco -> hueco.id() == nuevoHuecoId)
                .findFirst()
                .orElseThrow(() ->
                        new HuecoNoDisponibleException(
                                "No existe el hueco "
                                + nuevoHuecoId));

        if (ocupado(nuevoHuecoId)) {
            throw new HuecoNoDisponibleException(
                    "El hueco " + nuevoHuecoId
                    + " ya tiene una cita");
        }

        exigirPolitica(actual, nuevo.fechaHora());

        // Actualizamos la cita guardada, no solo la copia obtenida durante la consulta.
        for (Cita almacenada : citas) {

            if (almacenada.getId() != null
                    && almacenada.getId() == citaId) {

                almacenada.setHuecoId(nuevo.id());
                almacenada.setMedicoId(nuevo.medicoId());
                almacenada.setFechaHora(nuevo.fechaHora());
                almacenada.setEstado(
                        EstadoCita.PROGRAMADA);

                return almacenada.copia();
            }
        }

        throw new CitaNoEncontradaException(
                "No existe la cita con id " + citaId);
    }

    private boolean ocupado(long huecoId) {

        return citas.stream().anyMatch(cita ->
                cita.getHuecoId() != null
                && cita.getHuecoId() == huecoId
                && (cita.getEstado() == EstadoCita.PROGRAMADA
                || cita.getEstado() == EstadoCita.CONFIRMADA));
    }

    private void exigirPolitica(
            Cita actual,
            LocalDateTime nuevaFecha)
            throws CambioFueraDePoliticaException {

        LocalDateTime ahora = LocalDateTime.now(reloj);

        if (actual.getFechaHora() != null
                && ahora.isAfter(actual.getFechaHora()
                        .minusHours(cancelacionMinimaHoras))) {
            throw new CambioFueraDePoliticaException(
                    "La cita solo se puede cambiar con al menos "
                    + cancelacionMinimaHoras
                    + " horas de antelación");
        }

        if (nuevaFecha.isBefore(
                ahora.plusHours(antelacionMinimaHoras))) {
            throw new CambioFueraDePoliticaException(
                    "El nuevo hueco debe empezar con al menos "
                    + antelacionMinimaHoras
                    + " horas de antelación");
        }

        if (nuevaFecha.isAfter(
                ahora.plusDays(antelacionMaximaDias))) {
            throw new CambioFueraDePoliticaException(
                    "El nuevo hueco no puede estar a más de "
                    + antelacionMaximaDias + " días");
        }
    }
}

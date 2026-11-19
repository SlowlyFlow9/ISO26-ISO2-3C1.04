package agenda;

import java.time.LocalTime;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU29 - Implementación del servicio de huecos de agenda.
 *
 * Gestiona permisos, validaciones de horarios,
 * solapamientos y cambios de disponibilidad.
 */
public class HuecosAgendaServiceImpl
        implements HuecosAgendaService {

    private final AgendasRepositorio agendas;
    private final HuecosAgendaRepositorio huecos;

    public HuecosAgendaServiceImpl(
            AgendasRepositorio agendas,
            HuecosAgendaRepositorio huecos) {

        if (agendas == null || huecos == null) {
            throw new IllegalArgumentException(
                    "Los repositorios son obligatorios");
        }

        this.agendas = agendas;
        this.huecos = huecos;
    }

    @Override
    public synchronized HuecoAgenda crear(
            Usuario solicitante,
            HuecoAgenda datos)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            DatosHuecoInvalidosException {

        //1. Comprobar los permisos
        ControlAcceso.exigir(
                solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        // 2. Comprobar los datos básicos
        if (datos == null) {
            throw new DatosHuecoInvalidosException(
                    "Los datos del hueco son obligatorios");
        }

        if (datos.getId() != null) {
            throw new DatosHuecoInvalidosException(
                    "Un hueco nuevo no debe tener ID");
        }

        if (datos.getAgendaId() <= 0) {
            throw new DatosHuecoInvalidosException(
                    "La agenda debe tener un ID positivo");
        }

        // 3. Comprobar que la agenda existe y está activa
        AgendaMedica agenda =
                buscarAgenda(datos.getAgendaId());

        comprobarAgendaActiva(agenda);

        // 4. Validar las horas
        LocalTime inicio = datos.getHoraInicio();
        LocalTime fin = datos.getHoraFin();

        if (inicio == null || fin == null
                || !fin.isAfter(inicio)) {
            throw new DatosHuecoInvalidosException(
                    "El intervalo horario no es válido");
        }

        //5. Comprobar los límites de la agenda
        if (inicio.isBefore(agenda.getHoraInicio())
                || fin.isAfter(agenda.getHoraFin())) {
            throw new DatosHuecoInvalidosException(
                    "El hueco está fuera del horario de la agenda");
        }

        if (datos.isBloqueado()) {
            throw new DatosHuecoInvalidosException(
                    "Un hueco nuevo debe comenzar disponible");
        }

        //6. Impedir solapamientos
        for (HuecoAgenda existente :
                huecos.listarPorAgenda(datos.getAgendaId())) {

            boolean solapado =
                    inicio.isBefore(existente.getHoraFin())
                    && existente.getHoraInicio().isBefore(fin);

            if (solapado) {
                throw new DatosHuecoInvalidosException(
                        "El hueco se solapa con otro existente");
            }
        }

        //7. Guardar un hueco nuevo
        HuecoAgenda nuevo = new HuecoAgenda();
        nuevo.setAgendaId(datos.getAgendaId());
        nuevo.setHoraInicio(inicio);
        nuevo.setHoraFin(fin);
        nuevo.setBloqueado(false);

        return copiar(huecos.guardar(nuevo));
    }

    @Override
    public List<HuecoAgenda> listarPorAgenda(
            Usuario solicitante,
            long agendaId)
            throws SinPermisoException,
            AgendaNoEncontradaException {

        ControlAcceso.exigir(
                solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        buscarAgenda(agendaId);

        return huecos.listarPorAgenda(agendaId)
                .stream()
                .map(HuecosAgendaServiceImpl::copiar)
                .toList();
    }

    @Override
    public synchronized HuecoAgenda bloquear(
            Usuario solicitante,
            long huecoId)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException {

        return cambiarBloqueo(solicitante, huecoId, true);
    }

    @Override
    public synchronized HuecoAgenda liberar(
            Usuario solicitante,
            long huecoId)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException {

        return cambiarBloqueo(solicitante, huecoId, false);
    }

    //cambia el estado de bloqueo de un hueco
    private HuecoAgenda cambiarBloqueo(
            Usuario solicitante,
            long huecoId,
            boolean bloquear)
            throws SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException {

        ControlAcceso.exigir(
                solicitante, Rol.PERSONAL_ADMINISTRATIVO);

        HuecoAgenda actual = huecos.buscarPorId(huecoId)
                .orElseThrow(() ->
                        new HuecoAgendaNoEncontradoException(
                                "Hueco no encontrado"));

        AgendaMedica agenda = buscarAgenda(actual.getAgendaId());
        comprobarAgendaActiva(agenda);

        if (actual.isBloqueado() == bloquear) {
            throw new DatosHuecoInvalidosException(
                    bloquear
                            ? "El hueco ya está bloqueado"
                            : "El hueco ya está disponible");
        }

        HuecoAgenda actualizado = copiar(actual);
        actualizado.setBloqueado(bloquear);

        return copiar(huecos.guardar(actualizado));
    }

    // Busca una agenda o lanza una excepción
    private AgendaMedica buscarAgenda(long agendaId)
            throws AgendaNoEncontradaException {

        return agendas.buscarPorId(agendaId)
                .orElseThrow(() ->
                        new AgendaNoEncontradaException(
                                "Agenda no encontrada"));
    }

    // No se pueden gestionar huecos de agendas inactivas.
    private void comprobarAgendaActiva(AgendaMedica agenda)
            throws DatosHuecoInvalidosException {

        if (!agenda.isActivo()) {
            throw new DatosHuecoInvalidosException(
                    "La agenda está inactiva");
        }
    }

    // Devuelve una copia para proteger el objeto almacenado
    private static HuecoAgenda copiar(HuecoAgenda original) {

        HuecoAgenda copia = new HuecoAgenda();
        copia.setId(original.getId());
        copia.setAgendaId(original.getAgendaId());
        copia.setHoraInicio(original.getHoraInicio());
        copia.setHoraFin(original.getHoraFin());
        copia.setBloqueado(original.isBloqueado());

        return copia;
    }

}

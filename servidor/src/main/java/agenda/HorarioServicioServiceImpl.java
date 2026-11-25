package agenda;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class HorarioServicioServiceImpl
        implements HorarioServicioService {

    private final HorarioServicioRepositorio repositorio;

    public HorarioServicioServiceImpl(
            HorarioServicioRepositorio repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "El repositorio es obligatorio");
        }
        this.repositorio = repositorio;
    }

    @Override
    public HorarioServicio consultar(Usuario solicitante)
            throws SinPermisoException {
        exigirAdministrador(solicitante);
        return repositorio.consultar();
    }

    @Override
    public synchronized HorarioServicio modificar(
            Usuario solicitante,
            HorarioServicio horario)
            throws SinPermisoException,
            DatosHorarioServicioInvalidosException {
        exigirAdministrador(solicitante);
        validar(horario);

        HorarioServicio actualizado = horario.copia();
        actualizado.setModificadoPor(solicitante.nombre());
        actualizado.setModificadoEn(LocalDateTime.now());
        return repositorio.guardar(actualizado);
    }

    private static void exigirAdministrador(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);
    }

    private static void validar(HorarioServicio horario)
            throws DatosHorarioServicioInvalidosException {
        if (horario == null || horario.getDias() == null) {
            throw new DatosHorarioServicioInvalidosException(
                    "Los datos del horario son obligatorios");
        }

        EnumSet<java.time.DayOfWeek> diasRecibidos =
                EnumSet.noneOf(java.time.DayOfWeek.class);
        for (HorarioDia dia : horario.getDias()) {
            if (dia == null || dia.getDia() == null
                    || !diasRecibidos.add(dia.getDia())) {
                throw new DatosHorarioServicioInvalidosException(
                        "Cada dia de la semana debe aparecer una sola vez");
            }
            if (dia.getIntervalos() == null) {
                throw new DatosHorarioServicioInvalidosException(
                        "Los intervalos de cada dia son obligatorios");
            }
            validarIntervalos(dia.getIntervalos());
        }
        if (!diasRecibidos.equals(EnumSet.allOf(
                java.time.DayOfWeek.class))) {
            throw new DatosHorarioServicioInvalidosException(
                    "El horario debe incluir todos los dias de la semana");
        }
    }

    private static void validarIntervalos(
            List<IntervaloHorario> intervalos)
            throws DatosHorarioServicioInvalidosException {
        for (int i = 0; i < intervalos.size(); i++) {
            IntervaloHorario intervalo = intervalos.get(i);
            if (intervalo == null
                    || intervalo.getHoraInicio() == null
                    || intervalo.getHoraFin() == null
                    || !intervalo.getHoraInicio()
                            .isBefore(intervalo.getHoraFin())) {
                throw new DatosHorarioServicioInvalidosException(
                        "Cada intervalo debe tener inicio anterior al fin");
            }
            for (int j = 0; j < i; j++) {
                IntervaloHorario anterior = intervalos.get(j);
                if (intervalo.getHoraInicio()
                        .isBefore(anterior.getHoraFin())
                        && anterior.getHoraInicio()
                                .isBefore(intervalo.getHoraFin())) {
                    throw new DatosHorarioServicioInvalidosException(
                            "Los intervalos de un dia no pueden solaparse");
                }
            }
        }
    }
}

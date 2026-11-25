package cita;

import java.time.LocalDateTime;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class PoliticaCitaServiceImpl
        implements PoliticaCitaService {

    private final PoliticaCitaRepositorio repositorio;

    public PoliticaCitaServiceImpl(
            PoliticaCitaRepositorio repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "El repositorio es obligatorio");
        }
        this.repositorio = repositorio;
    }

    @Override
    public PoliticaCita consultar(Usuario solicitante)
            throws SinPermisoException {
        exigirAdministrador(solicitante);
        return repositorio.consultar();
    }

    @Override
    public synchronized PoliticaCita modificar(
            Usuario solicitante,
            PoliticaCita politica)
            throws SinPermisoException,
            DatosPoliticaCitaInvalidosException {
        exigirAdministrador(solicitante);
        validar(politica);

        PoliticaCita actualizada = politica.copia();
        actualizada.setModificadoPor(solicitante.nombre());
        actualizada.setModificadoEn(LocalDateTime.now());
        return repositorio.guardar(actualizada);
    }

    private static void exigirAdministrador(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(solicitante, Rol.ADMINISTRADOR);
    }

    private static void validar(PoliticaCita politica)
            throws DatosPoliticaCitaInvalidosException {
        if (politica == null
                || politica.getAntelacionMinimaHoras() == null
                || politica.getAntelacionMaximaDias() == null
                || politica.getCancelacionMinimaHoras() == null
                || politica.getMaxCitasActivasPorPaciente() == null) {
            throw new DatosPoliticaCitaInvalidosException(
                    "Todos los valores de la politica son obligatorios");
        }

        if (politica.getAntelacionMinimaHoras() < 0
                || politica.getAntelacionMaximaDias() < 0
                || politica.getCancelacionMinimaHoras() < 0) {
            throw new DatosPoliticaCitaInvalidosException(
                    "Las antelaciones no pueden ser negativas");
        }
        if (politica.getMaxCitasActivasPorPaciente() <= 0) {
            throw new DatosPoliticaCitaInvalidosException(
                    "El maximo de citas activas debe ser positivo");
        }

        long ventanaMaximaHoras =
                (long) politica.getAntelacionMaximaDias() * 24;
        if (ventanaMaximaHoras
                <= politica.getAntelacionMinimaHoras()) {
            throw new DatosPoliticaCitaInvalidosException(
                    "La ventana maxima debe superar la antelacion minima");
        }
        if (politica.getCancelacionMinimaHoras()
                > ventanaMaximaHoras) {
            throw new DatosPoliticaCitaInvalidosException(
                    "La antelacion de cancelacion no puede superar "
                            + "la ventana maxima de reserva");
        }
    }
}

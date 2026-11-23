package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface CitasService {

    List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException;

    Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException;


   //confirmar una cita propia programada
   Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException;
}
package cita;

import agenda.AgendasRepositorioMemoria;
import agenda.HuecosAgendaRepositorioMemoria;
import paciente.PacientesRepositorioMemoria;
import solicitud.SolicitudesRepositorioMemoria;

/** Monta el servicio de citas del paciente con sus dependencias vacías. */
final class ServiciosCitasPrueba {

    private ServiciosCitasPrueba() { }

    static CitasServiceImpl citas(
            CitasRepositorioMemoria citas,
            IdentidadPacienteMemoria identidad) {

        return new CitasServiceImpl(
                citas,
                identidad,
                new CitasAdministracionServiceImpl(
                        citas,
                        new SolicitudesRepositorioMemoria(),
                        new PacientesRepositorioMemoria(),
                        new AgendasRepositorioMemoria(),
                        new HuecosAgendaRepositorioMemoria()),
                new PoliticaCitaRepositorioMemoria());
    }
}

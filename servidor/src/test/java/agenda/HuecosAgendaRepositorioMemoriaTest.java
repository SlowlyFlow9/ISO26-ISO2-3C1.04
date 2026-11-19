package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU29 - Pruebas del repositorio de huecos de agenda.
 *
 * Comprueba el almacenamiento, la consulta,
 * la actualización y los errores del repositorio.
 */
public class HuecosAgendaRepositorioMemoriaTest {

    private HuecosAgendaRepositorioMemoria repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new HuecosAgendaRepositorioMemoria();
    }

    //Prepara un hueco válido para utilizar en las pruebas
    private HuecoAgenda crearHueco(long agendaId) {
        HuecoAgenda hueco = new HuecoAgenda();
        hueco.setAgendaId(agendaId);
        hueco.setHoraInicio(LocalTime.of(9, 0));
        hueco.setHoraFin(LocalTime.of(9, 30));
        hueco.setBloqueado(false);
        return hueco;
    }

    @Test
    @DisplayName("Guardar un hueco permite recuperarlo")
    void guardarYRecuperarHueco() {

        // PREPARAR
        HuecoAgenda hueco = crearHueco(1);

        // EJECUTAR
        HuecoAgenda guardado = repositorio.guardar(hueco);
        HuecoAgenda encontrado = repositorio
                .buscarPorId(guardado.getId())
                .orElseThrow();

        // COMPROBAR
        assertEquals(1L, guardado.getId());
        assertEquals(1L, encontrado.getAgendaId());
        assertEquals(LocalTime.of(9, 0),
                encontrado.getHoraInicio());
        assertEquals(LocalTime.of(9, 30),
                encontrado.getHoraFin());
    }

    @Test
    @DisplayName("Los huecos nuevos reciben IDs consecutivos")
    void asignarIdentificadoresConsecutivos() {

        // EJECUTAR
        HuecoAgenda primero = repositorio.guardar(
                crearHueco(1));
        HuecoAgenda segundo = repositorio.guardar(
                crearHueco(1));

        //Comprobar
        assertEquals(1L, primero.getId());
        assertEquals(2L, segundo.getId());
        assertEquals(2, repositorio.listarPorAgenda(1).size());
    }

    @Test
    @DisplayName("Buscar un hueco inexistente devuelve vacío")
    void buscarHuecoInexistente() {

        assertTrue(repositorio.buscarPorId(999).isEmpty());
    }

    @Test
    @DisplayName("Listar solo los huecos de una agenda")
    void listarHuecosPorAgenda() {

        // PREPARAR
        repositorio.guardar(crearHueco(1));
        repositorio.guardar(crearHueco(1));
        repositorio.guardar(crearHueco(2));

        // EJECUTAR
        List<HuecoAgenda> resultado =
                repositorio.listarPorAgenda(1);

        // COMPROBAR
        assertEquals(2, resultado.size());

        assertTrue(resultado.stream()
                .allMatch(h -> h.getAgendaId() == 1));

        assertEquals(1,
                repositorio.listarPorAgenda(2).size());
    }

    @Test
    @DisplayName("No se puede guardar un hueco nulo")
    void rechazarHuecoNulo() {

        // EJECUTAR Y COMPROBAR
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> repositorio.guardar(null)
        );

        assertEquals("El hueco no puede ser nulo",
                error.getMessage());

        // Un error no debe consumir el siguiente ID.
        assertEquals(1L,
                repositorio.guardar(crearHueco(1)).getId());
    }

    @Test
    @DisplayName("No se aceptan identificadores no positivos")
    void rechazarIdentificadorInvalido() {

        // Comprobar los límites 0 y -1.
        for (long id : new long[]{0L, -1L}) {

            HuecoAgenda hueco = crearHueco(1);
            hueco.setId(id);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> repositorio.guardar(hueco)
            );
        }

        //Ningún hueco incorrecto debe almacenarse.
        assertTrue(repositorio.listarPorAgenda(1).isEmpty());

        assertEquals(1L,
                repositorio.guardar(crearHueco(1)).getId());
    }

    @Test
    @DisplayName("Se respeta un identificador ya asignado")
    void respetarIdentificadorExistente() {

        // PREPARAR
        HuecoAgenda hueco = crearHueco(1);
        hueco.setId(10L);

        //EJECUTAR
        repositorio.guardar(hueco);
        HuecoAgenda siguiente =
                repositorio.guardar(crearHueco(1));

        // COMPROBAR
        assertTrue(repositorio.buscarPorId(10).isPresent());
        assertEquals(11L, siguiente.getId());
    }

    @Test
    @DisplayName("Actualizar un hueco no crea otro registro")
    void actualizarHuecoExistente() {

        //PREPARAR
        HuecoAgenda original = repositorio.guardar(
                crearHueco(1));

        HuecoAgenda actualizado = crearHueco(1);
        actualizado.setId(original.getId());
        actualizado.setBloqueado(true);

        // EJECUTAR
        repositorio.guardar(actualizado);

        // COMPROBAR
        assertEquals(1,
                repositorio.listarPorAgenda(1).size());

        assertTrue(repositorio
                .buscarPorId(original.getId())
                .orElseThrow()
                .isBloqueado());
    }
}

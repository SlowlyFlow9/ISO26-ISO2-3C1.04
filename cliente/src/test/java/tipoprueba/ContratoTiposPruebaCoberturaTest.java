package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContratoTiposPruebaCoberturaTest {

    @Test
    @DisplayName(
            "Por defecto un servicio indica cero pruebas pendientes")
    void pendientesPorDefecto()
            throws Exception {

        TiposPruebaService servicio =
                new TiposPruebaService() {

                    @Override
                    public TipoPrueba crear(
                            DatosTipoPrueba datos) {
                        return null;
                    }

                    @Override
                    public TipoPrueba consultar(long id) {
                        return null;
                    }

                    @Override
                    public List<TipoPrueba> listar(
                            FiltroTipoPrueba filtro) {
                        return List.of();
                    }

                    @Override
                    public TipoPrueba modificar(
                            long id,
                            DatosTipoPrueba datos) {
                        return null;
                    }

                    @Override
                    public void desactivar(long id) {
                    }
                };

        assertEquals(
                0,
                servicio.contarPruebasPendientes(
                        100L));
    }

    @Test
    @DisplayName(
            "ServicioException conserva el mensaje")
    void servicioExceptionMensaje() {

        assertEquals(
                "Error",
                new ServicioException(
                        "Error")
                        .getMessage());
    }

    @Test
    @DisplayName(
            "NombreDuplicadoException conserva el mensaje")
    void nombreDuplicadoMensaje() {

        assertEquals(
                "Duplicado",
                new NombreDuplicadoException(
                        "Duplicado")
                        .getMessage());
    }

    @Test
    @DisplayName(
            "Se pueden recuperar todos los roles")
    void rolesDisponibles() {

        assertEquals(
                4,
                Rol.values().length);

        assertEquals(
                Rol.ADMINISTRADOR,
                Rol.valueOf(
                        "ADMINISTRADOR"));
    }
}
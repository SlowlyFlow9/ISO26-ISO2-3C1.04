package solicitud;

import java.util.ArrayList;
import java.util.List;

public class ValidadorReglaPrioridad {

    public Resultado validar(
            String nombre,
            String descripcion,
            String orden) {
        List<String> errores = new ArrayList<>();
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String descripcionLimpia =
                descripcion == null ? "" : descripcion.trim();
        String ordenLimpio = orden == null ? "" : orden.trim();

        if (nombreLimpio.isEmpty()) {
            errores.add("El nombre es obligatorio.");
        }

        int ordenNumero = 0;
        if (ordenLimpio.isEmpty()) {
            errores.add("El orden es obligatorio.");
        } else {
            try {
                ordenNumero = Integer.parseInt(ordenLimpio);
                if (ordenNumero <= 0) {
                    errores.add("El orden debe ser mayor que cero.");
                }
            } catch (NumberFormatException e) {
                errores.add("El orden debe ser un número entero.");
            }
        }

        DatosReglaPrioridad datos = errores.isEmpty()
                ? new DatosReglaPrioridad(
                        nombreLimpio, descripcionLimpia, ordenNumero)
                : null;
        return new Resultado(errores, datos);
    }

    public static class Resultado {

        private final List<String> errores;
        private final DatosReglaPrioridad datos;

        private Resultado(
                List<String> errores,
                DatosReglaPrioridad datos) {
            this.errores = List.copyOf(errores);
            this.datos = datos;
        }

        public boolean esValido() {
            return errores.isEmpty();
        }

        public List<String> getErrores() {
            return errores;
        }

        public DatosReglaPrioridad getDatos() {
            return datos;
        }
    }
}

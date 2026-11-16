package tipoprueba;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Valida solo el FORMATO del formulario (D-013). Las reglas de negocio las aplica el servidor. */
public class ValidadorTipoPrueba {

    public static class Resultado {
        private final List<String> errores;
        private final DatosTipoPrueba datos;

        Resultado(List<String> errores, DatosTipoPrueba datos) {
            this.errores = Collections.unmodifiableList(errores);
            this.datos = datos;
        }

        public boolean esValido() { return errores.isEmpty(); }
        public List<String> getErrores() { return errores; }
        public DatosTipoPrueba getDatos() { return datos; }
    }

    public Resultado validar(String nombre, String descripcion, String duracion,
                             boolean requierePreparacion, String instrucciones) {
        List<String> errores = new ArrayList<>();
        String n = nombre == null ? "" : nombre.trim();
        String d = duracion == null ? "" : duracion.trim();
        String i = instrucciones == null ? "" : instrucciones.trim();

        if (n.isEmpty()) {
            errores.add(Mensajes.NOMBRE_OBLIGATORIO);
        }

        int minutos = 0;
        if (d.isEmpty()) {
            errores.add(Mensajes.DURACION_OBLIGATORIA);
        } else {
            try {
                minutos = Integer.parseInt(d);
                if (minutos <= 0) {
                    errores.add(Mensajes.DURACION_NO_POSITIVA);
                }
            } catch (NumberFormatException e) {
                errores.add(Mensajes.DURACION_NO_NUMERICA);
            }
        }

        if (requierePreparacion && i.isEmpty()) {
            errores.add(Mensajes.INSTRUCCIONES_OBLIGATORIAS);
        }

        DatosTipoPrueba datos = errores.isEmpty()
                ? new DatosTipoPrueba(n, descripcion == null ? "" : descripcion.trim(),
                        minutos, requierePreparacion, requierePreparacion ? i : "")
                : null;
        return new Resultado(errores, datos);
    }
}

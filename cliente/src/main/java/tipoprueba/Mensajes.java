package tipoprueba;

/** Textos de la interfaz centralizados (D-015, pendiente de cerrar). */
public final class Mensajes {
    private Mensajes() { }

    public static final String NOMBRE_OBLIGATORIO = "El nombre es obligatorio.";
    public static final String DURACION_OBLIGATORIA = "La duración estimada es obligatoria.";
    public static final String DURACION_NO_NUMERICA = "La duración debe ser un número entero de minutos.";
    public static final String DURACION_NO_POSITIVA = "La duración debe ser mayor que 0.";
    public static final String INSTRUCCIONES_OBLIGATORIAS =
            "Si requiere preparación, indica las instrucciones al paciente.";

    public static final String EXITO_CREAR = "Tipo de prueba creado correctamente.";
    public static final String EXITO_MODIFICAR = "Tipo de prueba modificado correctamente.";
    public static final String EXITO_DESACTIVAR = "Tipo de prueba desactivado correctamente.";
    public static final String ERROR_GENERICO = "No se ha podido completar la operación: ";
    public static final String CONFIRMAR_DESACTIVAR = "¿Seguro que quieres desactivar el tipo de prueba \"%s\"?";
    public static final String AVISO_PENDIENTES =
            "Atención: hay %d prueba(s) pendiente(s) de este tipo.\n";
}

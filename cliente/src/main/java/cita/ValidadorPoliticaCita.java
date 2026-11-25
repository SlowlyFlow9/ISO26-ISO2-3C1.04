package cita;

public final class ValidadorPoliticaCita {

    private ValidadorPoliticaCita() {
    }

    public static PoliticaCita parsear(
            String antelacionMinimaHoras,
            String antelacionMaximaDias,
            String cancelacionMinimaHoras,
            String maxCitasActivasPorPaciente)
            throws FormatoPoliticaCitaException {
        PoliticaCita politica = new PoliticaCita();
        politica.setAntelacionMinimaHoras(
                entero("La antelacion minima", antelacionMinimaHoras));
        politica.setAntelacionMaximaDias(
                entero("La ventana maxima", antelacionMaximaDias));
        politica.setCancelacionMinimaHoras(
                entero("La antelacion de cancelacion",
                        cancelacionMinimaHoras));
        politica.setMaxCitasActivasPorPaciente(
                entero("El maximo de citas activas",
                        maxCitasActivasPorPaciente));
        return politica;
    }

    private static int entero(String nombre, String valor)
            throws FormatoPoliticaCitaException {
        if (valor == null || valor.isBlank()) {
            throw new FormatoPoliticaCitaException(
                    nombre + " es obligatoria");
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new FormatoPoliticaCitaException(
                    nombre + " debe ser un numero entero");
        }
    }
}

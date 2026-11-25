package agenda;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class ValidadorHorarioServicio {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withResolverStyle(
                            java.time.format.ResolverStyle.STRICT);

    private ValidadorHorarioServicio() {
    }

    public static List<IntervaloHorario> parsearIntervalos(
            String texto)
            throws FormatoHorarioServicioException {
        List<IntervaloHorario> intervalos = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return intervalos;
        }

        for (String parte : texto.split(",", -1)) {
            String[] horas = parte.trim().split("-", -1);
            if (horas.length != 2) {
                throw formatoInvalido();
            }
            try {
                intervalos.add(new IntervaloHorario(
                        LocalTime.parse(horas[0].trim(), FORMATO),
                        LocalTime.parse(horas[1].trim(), FORMATO)));
            } catch (DateTimeParseException e) {
                throw formatoInvalido();
            }
        }
        return intervalos;
    }

    private static FormatoHorarioServicioException formatoInvalido() {
        return new FormatoHorarioServicioException(
                "Usa el formato HH:mm-HH:mm y separa los intervalos con comas");
    }
}

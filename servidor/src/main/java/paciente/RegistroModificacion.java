package paciente;

import java.time.LocalDateTime;

/** Quién modificó a qué paciente y cuándo (D-017). En la It. 12 alimenta la auditoría. */
public record RegistroModificacion(long pacienteId, String usuario, LocalDateTime fecha) { }
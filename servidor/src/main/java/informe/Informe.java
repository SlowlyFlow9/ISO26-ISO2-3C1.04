package informe;

import java.time.LocalDate;

public class Informe {
    private String id;
    private String idPaciente;
    private String texto;
    private LocalDate fecha;

    public Informe() {
    }

    public Informe(String id, String idPaciente, String texto, LocalDate fecha) {
        this.id = id;
        this.idPaciente = idPaciente;
        this.texto = texto;
        this.fecha = fecha;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdPaciente() { return idPaciente; }
    public void setIdPaciente(String idPaciente) { this.idPaciente = idPaciente; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
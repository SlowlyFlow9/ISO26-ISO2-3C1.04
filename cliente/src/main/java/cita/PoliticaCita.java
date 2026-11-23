package cita;

import java.time.LocalDateTime;

public class PoliticaCita {

    private Integer antelacionMinimaHoras;
    private Integer antelacionMaximaDias;
    private Integer cancelacionMinimaHoras;
    private Integer maxCitasActivasPorPaciente;
    private String modificadoPor;
    private LocalDateTime modificadoEn;

    public static PoliticaCita inicial() {
        PoliticaCita politica = new PoliticaCita();
        politica.antelacionMinimaHoras = 0;
        politica.antelacionMaximaDias = 90;
        politica.cancelacionMinimaHoras = 24;
        politica.maxCitasActivasPorPaciente = 3;
        return politica;
    }

    public Integer getAntelacionMinimaHoras() {
        return antelacionMinimaHoras;
    }

    public void setAntelacionMinimaHoras(Integer valor) {
        antelacionMinimaHoras = valor;
    }

    public Integer getAntelacionMaximaDias() {
        return antelacionMaximaDias;
    }

    public void setAntelacionMaximaDias(Integer valor) {
        antelacionMaximaDias = valor;
    }

    public Integer getCancelacionMinimaHoras() {
        return cancelacionMinimaHoras;
    }

    public void setCancelacionMinimaHoras(Integer valor) {
        cancelacionMinimaHoras = valor;
    }

    public Integer getMaxCitasActivasPorPaciente() {
        return maxCitasActivasPorPaciente;
    }

    public void setMaxCitasActivasPorPaciente(Integer valor) {
        maxCitasActivasPorPaciente = valor;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public LocalDateTime getModificadoEn() {
        return modificadoEn;
    }

    public void setModificadoEn(LocalDateTime modificadoEn) {
        this.modificadoEn = modificadoEn;
    }
}

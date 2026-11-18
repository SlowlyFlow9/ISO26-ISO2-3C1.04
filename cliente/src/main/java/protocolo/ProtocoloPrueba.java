package protocolo;

public class ProtocoloPrueba {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long tipoPruebaId;
    private String instruccionesRealizacion;
    private boolean requiereContraste;
    private boolean requiereConsentimiento;
    private String requisitosSeguridad;
    private boolean activo = true;

    public ProtocoloPrueba() {
    }

    public ProtocoloPrueba copia() {
        ProtocoloPrueba copia = new ProtocoloPrueba();
        copia.id = id;
        copia.nombre = nombre;
        copia.descripcion = descripcion;
        copia.tipoPruebaId = tipoPruebaId;
        copia.instruccionesRealizacion = instruccionesRealizacion;
        copia.requiereContraste = requiereContraste;
        copia.requiereConsentimiento = requiereConsentimiento;
        copia.requisitosSeguridad = requisitosSeguridad;
        copia.activo = activo;
        return copia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getTipoPruebaId() {
        return tipoPruebaId;
    }

    public void setTipoPruebaId(Long tipoPruebaId) {
        this.tipoPruebaId = tipoPruebaId;
    }

    public String getInstruccionesRealizacion() {
        return instruccionesRealizacion;
    }

    public void setInstruccionesRealizacion(String instruccionesRealizacion) {
        this.instruccionesRealizacion = instruccionesRealizacion;
    }

    public boolean isRequiereContraste() {
        return requiereContraste;
    }

    public void setRequiereContraste(boolean requiereContraste) {
        this.requiereContraste = requiereContraste;
    }

    public boolean isRequiereConsentimiento() {
        return requiereConsentimiento;
    }

    public void setRequiereConsentimiento(boolean requiereConsentimiento) {
        this.requiereConsentimiento = requiereConsentimiento;
    }

    public String getRequisitosSeguridad() {
        return requisitosSeguridad;
    }

    public void setRequisitosSeguridad(String requisitosSeguridad) {
        this.requisitosSeguridad = requisitosSeguridad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
package paciente;

import java.time.LocalDate;

/** Datos de un paciente (D-005). id, fechaAlta y activo los asigna el servidor al dar el alta. */
public class Paciente {
    private Long id;
    private String nombre;
    private String apellidos;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;
    private String numeroTarjetaSanitaria;
    private LocalDate fechaAlta;
    private boolean activo = true;

    public Paciente() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }

    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDni() { return dni; }

    public void setDni(String dni) { this.dni = dni; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }

    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefono() { return telefono; }

    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }

    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getNumeroTarjetaSanitaria() { return numeroTarjetaSanitaria; }

    public void setNumeroTarjetaSanitaria(String numeroTarjetaSanitaria) { this.numeroTarjetaSanitaria = numeroTarjetaSanitaria; }

    public LocalDate getFechaAlta() { return fechaAlta; }

    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }

    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) { this.activo = activo; }
}

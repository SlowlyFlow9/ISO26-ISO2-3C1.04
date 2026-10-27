package paciente;

import java.io.IOException;

/** Arranque del servidor: Servidor [puerto], por defecto 8080. Los datos se guardan en memoria (D-024). */
public class Servidor {

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        PacientesServidorHttp.iniciar(new PacientesServiceImpl(new PacientesRepositorioMemoria()), puerto);
        System.out.println("Servidor escuchando en http://localhost:" + puerto + "/pacientes");
    }
}

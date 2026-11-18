package soporte;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Utilidad de pruebas para las pantallas Swing.
 * Responde automáticamente a los diálogos modales (JOptionPane y formularios) para que
 * los tests no se queden bloqueados, y guarda el texto de cada diálogo para comprobarlo.
 * Las respuestas se aplican en el orden en que se abren los diálogos; si no hay ninguna
 * preparada, el diálogo se acepta.
 */
public final class Dialogos implements AutoCloseable {

    private static final long ESPERA_MAXIMA_MS = 5000;

    private final ConcurrentLinkedDeque<Consumer<JDialog>> respuestas = new ConcurrentLinkedDeque<>();
    private final List<String> textos = Collections.synchronizedList(new ArrayList<>());
    private final AWTEventListener oyente = evento -> {
        if (evento.getID() == WindowEvent.WINDOW_OPENED && evento.getSource() instanceof JDialog dialogo) {
            SwingUtilities.invokeLater(() -> atender(dialogo));
        }
    };

    private Dialogos() { }

    /** Instala el respondedor. Si no hay pantalla (entorno headless) el test se omite. */
    public static Dialogos instalar() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Las pruebas de pantallas necesitan entorno gráfico");
        Dialogos dialogos = new Dialogos();
        Toolkit.getDefaultToolkit().addAWTEventListener(dialogos.oyente, AWTEvent.WINDOW_EVENT_MASK);
        return dialogos;
    }

    /** Prepara la respuesta al siguiente diálogo que se abra. */
    public Dialogos responder(Consumer<JDialog> respuesta) {
        respuestas.add(respuesta);
        return this;
    }

    /** Textos de todos los diálogos abiertos hasta ahora (título y mensaje). */
    public List<String> textos() {
        synchronized (textos) {
            return new ArrayList<>(textos);
        }
    }

    /** Último texto registrado, o cadena vacía si no se ha abierto ningún diálogo. */
    public String ultimoTexto() {
        List<String> copia = textos();
        return copia.isEmpty() ? "" : copia.get(copia.size() - 1);
    }

    /** Indica si algún diálogo contenía el texto. */
    public boolean algunoContiene(String fragmento) {
        return textos().stream().anyMatch(t -> t.contains(fragmento));
    }

    private void atender(JDialog dialogo) {
        if (!dialogo.isShowing()) return;
        textos.add(texto(dialogo));
        Consumer<JDialog> respuesta = respuestas.poll();
        (respuesta == null ? (Consumer<JDialog>) Dialogos::aceptar : respuesta).accept(dialogo);
    }

    private static String texto(JDialog dialogo) {
        StringBuilder texto = new StringBuilder(dialogo.getTitle() == null ? "" : dialogo.getTitle());
        JOptionPane panel = primero(dialogo, JOptionPane.class);
        if (panel != null && panel.getMessage() instanceof String mensaje) {
            texto.append(" | ").append(mensaje);
        }
        for (JLabel etiqueta : buscar(dialogo, JLabel.class)) {
            if (etiqueta.getText() != null && !etiqueta.getText().isBlank()) {
                texto.append(" | ").append(etiqueta.getText());
            }
        }
        return texto.toString();
    }

    @Override
    public void close() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(oyente);
        enEdt(() -> {
            for (Window ventana : Window.getWindows()) ventana.dispose();
        });
    }

    // ---- Respuestas habituales ----

    /** Pulsa Aceptar / Sí. */
    public static void aceptar(JDialog dialogo) {
        elegir(dialogo, JOptionPane.OK_OPTION);
    }

    /** Pulsa No (en diálogos Sí/No). */
    public static void no(JDialog dialogo) {
        elegir(dialogo, JOptionPane.NO_OPTION);
    }

    /** Cierra el diálogo sin aceptar (equivale a Cancelar). */
    public static void cancelar(JDialog dialogo) {
        elegir(dialogo, JOptionPane.CLOSED_OPTION);
    }

    private static void elegir(JDialog dialogo, int opcion) {
        JOptionPane panel = primero(dialogo, JOptionPane.class);
        if (panel != null) {
            panel.setValue(opcion);
        } else {
            dialogo.dispose();
        }
    }

    // ---- Utilidades sobre componentes ----

    /** Todos los componentes del tipo indicado, en el orden en que se añadieron. */
    public static <T> List<T> buscar(Container contenedor, Class<T> tipo) {
        List<T> encontrados = new ArrayList<>();
        recorrer(contenedor, tipo, encontrados);
        return encontrados;
    }

    private static <T> void recorrer(Container contenedor, Class<T> tipo, List<T> encontrados) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) encontrados.add(tipo.cast(hijo));
            if (hijo instanceof Container interior) recorrer(interior, tipo, encontrados);
        }
    }

    public static <T> T primero(Container contenedor, Class<T> tipo) {
        List<T> encontrados = buscar(contenedor, tipo);
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    /** Botón (o casilla) con el texto indicado. */
    public static AbstractButton boton(Container contenedor, String texto) {
        return buscar(contenedor, AbstractButton.class).stream()
                .filter(b -> texto.equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No hay ningún botón \"" + texto + "\""));
    }

    /** Lee un atributo privado de una pantalla (solo para pruebas). */
    public static <T> T campo(Object objeto, String nombre, Class<T> tipo) {
        Class<?> clase = objeto.getClass();
        while (clase != null) {
            try {
                Field campo = clase.getDeclaredField(nombre);
                campo.setAccessible(true);
                return tipo.cast(campo.get(objeto));
            } catch (NoSuchFieldException e) {
                clase = clase.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("No existe el atributo " + nombre);
    }

    // ---- Hilo de Swing ----

    /** Ejecuta en el hilo de Swing y espera a que termine. */
    public static void enEdt(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(accion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            if (e.getCause() instanceof Error error) throw error;
            throw new IllegalStateException(e.getCause());
        }
    }

    /** Ejecuta en el hilo de Swing y devuelve el resultado. */
    public static <T> T consultar(Supplier<T> consulta) {
        List<T> resultado = new ArrayList<>(1);
        enEdt(() -> resultado.add(consulta.get()));
        return resultado.get(0);
    }

    /** Espera hasta que se cumpla la condición (comprobada en el hilo de Swing). */
    public static void esperar(BooleanSupplier condicion) {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (System.currentTimeMillis() < limite) {
            if (consultar(condicion::getAsBoolean)) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("La condición no se ha cumplido en " + ESPERA_MAXIMA_MS + " ms");
    }
}

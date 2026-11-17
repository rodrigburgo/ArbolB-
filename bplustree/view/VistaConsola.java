package bplustree.view;

import java.util.*;

/**
 * VISTA (V de MVC): TODO lo que se imprime o se lee de la consola vive aqui.
 * No conoce el arbol ni las reglas del B+: solo recibe datos ya preparados
 * (listas, numeros, textos) y los muestra bonito.
 */
public class VistaConsola {

    private final Scanner in = new Scanner(System.in);

    // ---------- Entrada ----------

    public int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = in.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("  X Escribe un numero entero valido.");
            }
        }
    }

    public String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return in.nextLine().trim();
    }

    /** Pausa util al grabar el video: avanzar paso a paso con ENTER. */
    public void pausa() {
        System.out.print("   >  ENTER para continuar...");
        in.nextLine();
    }

    // ---------- Salida general ----------

    public void mensaje(String texto) { System.out.println(texto); }

    public void titulo(String texto) {
        String linea = "=".repeat(Math.max(texto.length() + 4, 40));
        System.out.println("\n" + linea + "\n  " + texto + "\n" + linea);
    }

    public void subtitulo(String texto) { System.out.println("\n> " + texto); }

    public void mostrarMenu(int orden, int tamanio) {
        System.out.println("\n+---------- ARBOL B+  (orden " + orden + ", " + tamanio + " registros) ----------+");
        System.out.println("|  1. Insertar estudiante                             |");
        System.out.println("|  2. Buscar por codigo                               |");
        System.out.println("|  3. Buscar por rango de codigos                     |");
        System.out.println("|  4. Eliminar por codigo                             |");
        System.out.println("|  5. Mostrar arbol (por niveles)                     |");
        System.out.println("|  6. Listar todo en orden (recorrido de hojas)       |");
        System.out.println("|  7. Cargar datos de ejemplo                         |");
        System.out.println("|  8. DEMO guiada: insercion paso a paso (m=3)        |");
        System.out.println("|  9. DEMO guiada: borrado (prestamo y fusion, m=4)   |");
        System.out.println("| 10. DEMO guiada: busqueda por rango (m=4)           |");
        System.out.println("| 11. Reiniciar con otro orden                        |");
        System.out.println("|  0. Salir                                           |");
        System.out.println("+-----------------------------------------------------+");
    }

    // ---------- Salida especifica del arbol B+ ----------

    /**
     * Dibuja el arbol por niveles. El ultimo nivel son las hojas y se muestran
     * unidas por flechas (->) para que se vea la lista enlazada.
     */
    public <K> void mostrarArbol(List<List<List<K>>> niveles, int orden, int tamanio) {
        System.out.println("\n  Arbol B+ | orden=" + orden + " | claves=" + tamanio
                + " | altura=" + (niveles.size() - 1));
        if (tamanio == 0) { System.out.println("  (vacio)"); return; }
        for (int n = 0; n < niveles.size(); n++) {
            boolean esUltimo = n == niveles.size() - 1;
            String etiqueta = esUltimo ? (n == 0 ? "raiz/hoja" : "hojas")
                                       : (n == 0 ? "raiz" : "nivel " + n);
            String sep = esUltimo ? " -> " : "  ";
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < niveles.get(n).size(); j++) {
                if (j > 0) sb.append(sep);
                sb.append(formatoNodo(niveles.get(n).get(j)));
            }
            System.out.printf("  %-10s %s%n", etiqueta + ":", sb);
        }
    }

    private <K> String formatoNodo(List<K> claves) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < claves.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(claves.get(i));
        }
        return sb.append("]").toString();
    }

    /** Muestra la bitacora (que hizo el arbol por dentro). */
    public void mostrarEventos(List<String> eventos) {
        for (String e : eventos) System.out.println("   * " + e);
    }

    public <K, V> void mostrarEntradas(String titulo, List<Map.Entry<K, V>> entradas) {
        System.out.println("\n  " + titulo + " (" + entradas.size() + "):");
        if (entradas.isEmpty()) System.out.println("   (ninguno)");
        for (Map.Entry<K, V> e : entradas) System.out.println("   " + e.getValue());
    }
}

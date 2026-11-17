package bplustree;

import bplustree.model.BPlusTree;

import java.util.*;

/**
 * Prueba automatica: compara el B+ contra TreeMap de Java (que ya sabemos
 * que funciona) con miles de operaciones aleatorias y varios ordenes.
 * Despues de CADA operacion se verifican los invariantes del arbol.
 */
public class PruebaAleatoria {
    public static void main(String[] args) {
        Random rnd = new Random(42);
        long operaciones = 0;
        for (int orden = 3; orden <= 9; orden++) {
            for (int ronda = 0; ronda < 30; ronda++) {
                BPlusTree<Integer, Integer> arbol = new BPlusTree<>(orden);
                TreeMap<Integer, Integer> ref = new TreeMap<>();
                for (int i = 0; i < 1500; i++) {
                    int k = rnd.nextInt(300);
                    int op = rnd.nextInt(10);
                    if (op < 5) {                                   // 50% insertar
                        arbol.insertar(k, k * 10);
                        ref.put(k, k * 10);
                    } else if (op < 8) {                            // 30% eliminar
                        boolean a = arbol.eliminar(k);
                        boolean b = ref.remove(k) != null;
                        check(a == b, "eliminar(" + k + ") difiere");
                    } else if (op == 8) {                           // 10% buscar
                        check(Objects.equals(arbol.buscar(k), ref.get(k)), "buscar(" + k + ") difiere");
                    } else {                                        // 10% rango
                        int hasta = k + rnd.nextInt(50);
                        List<Integer> esperado = new ArrayList<>(ref.subMap(k, true, hasta, true).keySet());
                        List<Integer> obtenido = new ArrayList<>();
                        arbol.buscarRango(k, hasta).forEach(e -> obtenido.add(e.getKey()));
                        check(esperado.equals(obtenido), "rango [" + k + "," + hasta + "] difiere");
                    }
                    arbol.verificarInvariantes();
                    check(arbol.tamanio() == ref.size(), "tamano difiere");
                    operaciones++;
                }
                check(arbol.claves().equals(new ArrayList<>(ref.keySet())), "claves finales difieren");
            }
        }
        System.out.println("OK: " + operaciones + " operaciones aleatorias coinciden con TreeMap "
                + "y el arbol fue valido en todo momento.");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
}

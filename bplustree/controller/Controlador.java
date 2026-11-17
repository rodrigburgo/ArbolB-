package bplustree.controller;

import bplustree.model.BPlusTree;
import bplustree.model.Estudiante;
import bplustree.view.VistaConsola;

import java.util.List;
import java.util.Map;

/**
 * CONTROLADOR (C de MVC): es el intermediario.
 *   1. Pide datos a la Vista (leerEntero, leerTexto...).
 *   2. Llama al Modelo (insertar, buscar, eliminar...).
 *   3. Le entrega el resultado a la Vista para que lo muestre.
 * Asi el Modelo no sabe nada de la consola y la Vista no sabe nada del B+.
 */
public class Controlador {

    private final VistaConsola vista;
    private BPlusTree<Integer, Estudiante> arbol;

    public Controlador(VistaConsola vista, int ordenInicial) {
        this.vista = vista;
        this.arbol = new BPlusTree<>(ordenInicial);
    }

    // ===================================================================
    //  BUCLE PRINCIPAL DEL MENU
    // ===================================================================

    public void iniciar() {
        vista.titulo("ARBOL B+ - Indice de estudiantes (MVC)");
        int opcion;
        do {
            vista.mostrarMenu(arbol.getOrden(), arbol.tamanio());
            opcion = vista.leerEntero("Opcion: ");
            switch (opcion) {
                case 1  -> insertar();
                case 2  -> buscar();
                case 3  -> buscarRango();
                case 4  -> eliminar();
                case 5  -> mostrarArbol();
                case 6  -> listarTodo();
                case 7  -> cargarEjemplo();
                case 8  -> demoInsercion();
                case 9  -> demoBorrado();
                case 10 -> demoRango();
                case 11 -> reiniciar();
                case 0  -> vista.mensaje("Hasta luego!");
                default -> vista.mensaje("  X Opcion no valida.");
            }
        } while (opcion != 0);
    }

    // ===================================================================
    //  OPCIONES DEL MENU (modo interactivo)
    // ===================================================================

    private void insertar() {
        int codigo = vista.leerEntero("Codigo (entero): ");
        String nombre = vista.leerTexto("Nombre: ");
        String carrera = vista.leerTexto("Carrera: ");
        arbol.insertar(codigo, new Estudiante(codigo, nombre, carrera));   // <- llamada al MODELO
        vista.mostrarEventos(arbol.getEventos());                          // <- la VISTA muestra
        mostrarArbol();
    }

    private void buscar() {
        int codigo = vista.leerEntero("Codigo a buscar: ");
        Estudiante e = arbol.buscar(codigo);
        vista.subtitulo("Camino recorrido:");
        vista.mostrarEventos(arbol.getEventos());
        vista.mensaje(e != null ? "  OK Resultado: " + e : "  X No existe el codigo " + codigo);
    }

    private void buscarRango() {
        int desde = vista.leerEntero("Desde codigo: ");
        int hasta = vista.leerEntero("Hasta codigo: ");
        List<Map.Entry<Integer, Estudiante>> r = arbol.buscarRango(desde, hasta);
        vista.mostrarEventos(arbol.getEventos());
        vista.mostrarEntradas("Estudiantes con codigo en [" + desde + ", " + hasta + "]", r);
    }

    private void eliminar() {
        int codigo = vista.leerEntero("Codigo a eliminar: ");
        arbol.eliminar(codigo);
        vista.mostrarEventos(arbol.getEventos());
        mostrarArbol();
    }

    private void mostrarArbol() {
        vista.mostrarArbol(arbol.niveles(), arbol.getOrden(), arbol.tamanio());
    }

    private void listarTodo() {
        vista.mostrarEntradas("Todos los registros ordenados", arbol.todos());
    }

    private void cargarEjemplo() {
        String[][] datos = {
            {"20230105", "Ana Torres", "Sistemas"},   {"20230112", "Luis Perez", "Civil"},
            {"20230120", "Maria Gomez", "Sistemas"},  {"20230131", "Carlos Ruiz", "Industrial"},
            {"20230207", "Sofia Diaz", "Medicina"},   {"20230215", "Jorge Vega", "Sistemas"},
            {"20230222", "Laura Mora", "Derecho"},    {"20230301", "Pedro Silva", "Civil"},
            {"20230309", "Valeria Rey", "Sistemas"},  {"20230318", "Andres Cruz", "Industrial"},
            {"20230326", "Camila Ortiz", "Medicina"}, {"20230404", "Diego Soto", "Derecho"},
        };
        for (String[] d : datos) {
            int cod = Integer.parseInt(d[0]);
            arbol.insertar(cod, new Estudiante(cod, d[1], d[2]));
        }
        vista.mensaje("  OK Se cargaron " + datos.length + " estudiantes.");
        mostrarArbol();
    }

    private void reiniciar() {
        int m = vista.leerEntero("Nuevo orden m (>= 3): ");
        try {
            arbol = new BPlusTree<>(m);
            vista.mensaje("  OK Arbol vacio con orden " + m);
        } catch (IllegalArgumentException ex) {
            vista.mensaje("  X " + ex.getMessage());
        }
    }

    // ===================================================================
    //  DEMOS GUIADAS (para explicar en el video paso a paso)
    // ===================================================================

    /** Inserta una lista de claves, mostrando que pasa por dentro en cada una. */
    private void insertarPasoAPaso(BPlusTree<Integer, Estudiante> t, int... claves) {
        for (int k : claves) {
            vista.subtitulo("insertar(" + k + ")");
            t.insertar(k, valorDemo(k));
            vista.mostrarEventos(t.getEventos());
            vista.mostrarArbol(t.niveles(), t.getOrden(), t.tamanio());
            t.verificarInvariantes();     // comprueba que sigue siendo un B+ valido
            vista.pausa();
        }
    }

    private void eliminarPasoAPaso(BPlusTree<Integer, Estudiante> t, int... claves) {
        for (int k : claves) {
            vista.subtitulo("eliminar(" + k + ")");
            t.eliminar(k);
            vista.mostrarEventos(t.getEventos());
            vista.mostrarArbol(t.niveles(), t.getOrden(), t.tamanio());
            t.verificarInvariantes();
            vista.pausa();
        }
    }

    private Estudiante valorDemo(int k) { return new Estudiante(k, "Est-" + k, "Demo"); }

    /** DEMO 1 - Seccion 05 del HTML: insertar 10,20,30,40,50 con m=3. */
    private void demoInsercion() {
        vista.titulo("DEMO 1: Insercion con division de hojas y de la raiz (m=3)");
        vista.mensaje("Orden 3 => maximo 2 claves por nodo. Un nodo con 3 claves se divide.");
        vista.mensaje("Fijate: las hojas conservan las claves reales; arriba solo suben COPIAS.");
        BPlusTree<Integer, Estudiante> t = new BPlusTree<>(3);
        insertarPasoAPaso(t, 10, 20, 30, 40, 50);
        vista.mensaje("\nOK Resultado: el arbol crecio a altura " + t.altura()
                + ". Todas las hojas estan enlazadas: " + t.claves());
    }

    /** DEMO 2 - Seccion 06 del HTML: prestamo y fusion al borrar (m=4). */
    private void demoBorrado() {
        vista.titulo("DEMO 2: Borrado con UNDERFLOW, prestamo y fusion (m=4)");
        vista.mensaje("Orden 4 => cada hoja debe tener entre 2 y 3 claves (la raiz es la excepcion).");
        BPlusTree<Integer, Estudiante> t = new BPlusTree<>(4);
        vista.subtitulo("Construyendo el arbol inicial...");
        for (int k : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}) t.insertar(k, valorDemo(k));
        vista.mostrarArbol(t.niveles(), t.getOrden(), t.tamanio());
        vista.pausa();
        eliminarPasoAPaso(t, 1, 5, 12, 9);
    }

    /** DEMO 3 - Seccion 07 del HTML: busqueda por rango con hojas enlazadas. */
    private void demoRango() {
        vista.titulo("DEMO 3: Busqueda puntual y por rango (m=4)");
        BPlusTree<Integer, Estudiante> t = new BPlusTree<>(4);
        for (int k : new int[]{10, 20, 5, 6, 12, 30, 7, 17, 3, 25, 18, 40, 50, 55, 45, 35, 38, 2, 4, 11})
            t.insertar(k, valorDemo(k));
        vista.mostrarArbol(t.niveles(), t.getOrden(), t.tamanio());
        vista.pausa();

        vista.subtitulo("buscar(17): baja por la raiz hasta una hoja");
        Estudiante e = t.buscar(17);
        vista.mostrarEventos(t.getEventos());
        vista.mensaje("  Resultado: " + e);
        vista.pausa();

        vista.subtitulo("buscar(99): no existe");
        e = t.buscar(99);
        vista.mostrarEventos(t.getEventos());
        vista.mensaje("  Resultado: " + e);
        vista.pausa();

        vista.subtitulo("buscarRango(10, 30): se baja UNA vez y luego se sigue la lista de hojas");
        List<Map.Entry<Integer, Estudiante>> r = t.buscarRango(10, 30);
        vista.mostrarEventos(t.getEventos());
        vista.mostrarEntradas("Rango [10, 30]", r);
        vista.mensaje("\n  Equivale a: SELECT * FROM estudiantes WHERE codigo BETWEEN 10 AND 30");
    }
}

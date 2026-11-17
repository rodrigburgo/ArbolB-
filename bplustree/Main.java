package bplustree;

import bplustree.controller.Controlador;
import bplustree.view.VistaConsola;

/**
 * Punto de entrada: arma las tres piezas del MVC y arranca.
 *
 *   Vista  <---->  Controlador  <---->  Modelo (BPlusTree dentro del controlador)
 */
public class Main {
    public static void main(String[] args) {
        VistaConsola vista = new VistaConsola();
        Controlador controlador = new Controlador(vista, 4);   // orden inicial = 4
        controlador.iniciar();
    }
}

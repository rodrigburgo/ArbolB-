package bplustree.model;

import java.util.*;

/**
 * MODELO (M de MVC): Arbol B+ generico.
 *
 * Esta clase NO imprime nada y NO lee nada del usuario: solo guarda datos y
 * aplica las reglas del arbol B+. Quien muestra los resultados es la Vista,
 * y quien conecta ambas cosas es el Controlador.
 *
 * Definicion (orden m = maximo de hijos por nodo interno):
 *  - Nodo interno: entre ceil(m/2) y m hijos (la raiz: entre 2 y m).
 *    Con k hijos tiene k-1 claves que solo sirven de "guia" (separadores).
 *  - Hoja: entre floor(m/2) y m-1 claves. Aqui viven los DATOS reales.
 *  - Todas las hojas estan al mismo nivel.
 *  - Las hojas estan enlazadas (siguiente / anterior) => busqueda por rango rapida.
 *
 * Regla de enrutamiento: en un nodo interno con claves k0 < k1 < ... :
 *      clave <  k0          -> hijo 0
 *      k(i-1) <= clave < ki -> hijo i
 *      clave >= k(last)     -> ultimo hijo
 *
 * Complejidad: O(log_m n) en buscar/insertar/eliminar y O(log_m n + k) en rango.
 */
public class BPlusTree<K extends Comparable<K>, V> {

    // ===================================================================
    //  NODOS (clases internas: nadie fuera del modelo las necesita ver)
    // ===================================================================

    /** Nodo base: tanto hojas como internos tienen una lista de claves. */
    private abstract static class Nodo<K, V> {
        final List<K> claves = new ArrayList<>();
        abstract boolean esHoja();
    }

    /** Nodo interno: solo claves-guia y punteros a hijos (NO guarda datos). */
    private static class NodoInterno<K, V> extends Nodo<K, V> {
        final List<Nodo<K, V>> hijos = new ArrayList<>();
        boolean esHoja() { return false; }
    }

    /** Hoja: claves reales + valores + enlaces a las hojas vecinas. */
    private static class NodoHoja<K, V> extends Nodo<K, V> {
        final List<V> valores = new ArrayList<>();
        NodoHoja<K, V> siguiente;
        NodoHoja<K, V> anterior;
        boolean esHoja() { return true; }
    }

    /** Resultado de dividir un nodo: la clave que sube al padre + el nodo nuevo (derecho). */
    private static class Division<K, V> {
        final K clave;
        final Nodo<K, V> derecho;
        Division(K clave, Nodo<K, V> derecho) { this.clave = clave; this.derecho = derecho; }
    }

    // ===================================================================
    //  CAMPOS
    // ===================================================================

    private final int orden;
    private Nodo<K, V> raiz;
    private int tamanio;

    /**
     * Bitacora de lo que hizo la ultima operacion (divisiones, prestamos,
     * fusiones...). La Vista la usa para mostrar la traza paso a paso.
     */
    private final List<String> eventos = new ArrayList<>();

    // ===================================================================
    //  CONSTRUCTOR Y CONSULTAS BASICAS
    // ===================================================================

    public BPlusTree(int orden) {
        if (orden < 3) throw new IllegalArgumentException("El orden debe ser >= 3");
        this.orden = orden;
        this.raiz = new NodoHoja<>();   // arbol vacio = una sola hoja vacia
        this.tamanio = 0;
    }

    public int getOrden()   { return orden; }
    public int tamanio()    { return tamanio; }
    public boolean vacio()  { return tamanio == 0; }

    /** Maximo de claves por nodo (hoja o interno) = m - 1. */
    public int maxClaves() { return orden - 1; }

    /** Minimo de claves en una hoja = floor(m/2). */
    private int minClavesHoja() { return orden / 2; }

    /** Minimo de claves en un nodo interno = ceil(m/2) - 1 = floor((m-1)/2). */
    private int minClavesInterno() { return (orden - 1) / 2; }

    private int minClaves(Nodo<K, V> n) { return n.esHoja() ? minClavesHoja() : minClavesInterno(); }

    /** Copia de la bitacora de la ultima operacion. */
    public List<String> getEventos() { return new ArrayList<>(eventos); }

    /** Altura = numero de saltos de la raiz a una hoja (arbol de 1 hoja => 0). */
    public int altura() {
        int h = 0;
        Nodo<K, V> n = raiz;
        while (!n.esHoja()) { h++; n = ((NodoInterno<K, V>) n).hijos.get(0); }
        return h;
    }

    // ===================================================================
    //  BUSQUEDA
    // ===================================================================

    /** Devuelve el valor asociado a la clave, o null si no existe. */
    public V buscar(K clave) {
        eventos.clear();
        NodoHoja<K, V> hoja = encontrarHoja(clave);
        int pos = Collections.binarySearch(hoja.claves, clave);
        if (pos >= 0) {
            eventos.add("Hoja " + hoja.claves + ": encontrada la clave " + clave + "!");
            return hoja.valores.get(pos);
        }
        eventos.add("Hoja " + hoja.claves + ": la clave " + clave + " NO esta.");
        return null;
    }

    /** Baja desde la raiz hasta la hoja donde deberia estar la clave. */
    private NodoHoja<K, V> encontrarHoja(K clave) {
        Nodo<K, V> actual = raiz;
        while (!actual.esHoja()) {
            NodoInterno<K, V> n = (NodoInterno<K, V>) actual;
            int i = indiceHijo(n, clave);
            eventos.add("Nodo interno " + n.claves + ": " + clave + " -> hijo #" + (i + 1));
            actual = n.hijos.get(i);
        }
        return (NodoHoja<K, V>) actual;
    }

    /** Indice del hijo por el que hay que bajar (ver regla de enrutamiento arriba). */
    private int indiceHijo(NodoInterno<K, V> n, K clave) {
        int i = 0;
        while (i < n.claves.size() && clave.compareTo(n.claves.get(i)) >= 0) i++;
        return i;
    }

    // ===================================================================
    //  BUSQUEDA POR RANGO  (la gran ventaja del B+: hojas enlazadas)
    // ===================================================================

    /** Todas las entradas con desde <= clave <= hasta, en orden. O(log n + k). */
    public List<Map.Entry<K, V>> buscarRango(K desde, K hasta) {
        eventos.clear();
        List<Map.Entry<K, V>> resultado = new ArrayList<>();
        NodoHoja<K, V> hoja = encontrarHoja(desde);   // 1) bajar UNA vez hasta la hoja inicial
        eventos.add("Hoja inicial: " + hoja.claves + " (desde aqui solo se siguen los enlaces ->)");
        while (hoja != null) {                         // 2) recorrer hojas con 'siguiente'
            for (int i = 0; i < hoja.claves.size(); i++) {
                K k = hoja.claves.get(i);
                if (k.compareTo(desde) < 0) continue;
                if (k.compareTo(hasta) > 0) return resultado;
                resultado.add(new AbstractMap.SimpleEntry<>(k, hoja.valores.get(i)));
            }
            hoja = hoja.siguiente;
        }
        return resultado;
    }

    // ===================================================================
    //  INSERCION
    // ===================================================================

    /**
     * Inserta (clave, valor). Si la clave ya existe, actualiza el valor.
     * Siempre se inserta en una hoja; si se desborda (m claves) se divide
     * y la division puede propagarse hacia arriba hasta la raiz.
     */
    public void insertar(K clave, V valor) {
        eventos.clear();
        Division<K, V> d = insertar(raiz, clave, valor);
        if (d != null) {
            // La raiz se dividio: el arbol crece UN nivel (unica forma en que crece).
            NodoInterno<K, V> nuevaRaiz = new NodoInterno<>();
            nuevaRaiz.claves.add(d.clave);
            nuevaRaiz.hijos.add(raiz);
            nuevaRaiz.hijos.add(d.derecho);
            raiz = nuevaRaiz;
            eventos.add("La raiz se dividio -> nueva raiz " + nuevaRaiz.claves + " (el arbol crece 1 nivel)");
        }
    }

    /** Insercion recursiva. Devuelve una Division si 'nodo' tuvo que partirse; si no, null. */
    private Division<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo.esHoja()) {
            NodoHoja<K, V> hoja = (NodoHoja<K, V>) nodo;
            int pos = Collections.binarySearch(hoja.claves, clave);
            if (pos >= 0) {                                   // clave repetida: actualizar
                hoja.valores.set(pos, valor);
                eventos.add("La clave " + clave + " ya existia: se actualizo su valor.");
                return null;
            }
            int ins = -pos - 1;                               // posicion ordenada
            hoja.claves.add(ins, clave);
            hoja.valores.add(ins, valor);
            tamanio++;
            eventos.add("Insertada " + clave + " en la hoja -> " + hoja.claves);
            if (hoja.claves.size() < orden) return null;      // cabe: terminamos
            return dividirHoja(hoja);                         // overflow: dividir
        }

        NodoInterno<K, V> interno = (NodoInterno<K, V>) nodo;
        int i = indiceHijo(interno, clave);
        Division<K, V> d = insertar(interno.hijos.get(i), clave, valor);
        if (d == null) return null;                           // el hijo no se dividio

        // El hijo se dividio: registrar la clave que sube y el nuevo hijo derecho.
        interno.claves.add(i, d.clave);
        interno.hijos.add(i + 1, d.derecho);
        if (interno.claves.size() < orden) return null;
        return dividirInterno(interno);
    }

    /**
     * Divide una hoja desbordada. La clave que sube es una COPIA de la primera
     * clave de la hoja derecha (en el B+ la clave real se queda en la hoja).
     */
    private Division<K, V> dividirHoja(NodoHoja<K, V> hoja) {
        String antes = hoja.claves.toString();
        int mid = hoja.claves.size() / 2;
        NodoHoja<K, V> derecha = new NodoHoja<>();
        derecha.claves.addAll(hoja.claves.subList(mid, hoja.claves.size()));
        derecha.valores.addAll(hoja.valores.subList(mid, hoja.valores.size()));
        hoja.claves.subList(mid, hoja.claves.size()).clear();
        hoja.valores.subList(mid, hoja.valores.size()).clear();

        // Enlazar la hoja nueva en la lista: hoja <-> derecha <-> (antigua siguiente)
        derecha.siguiente = hoja.siguiente;
        if (derecha.siguiente != null) derecha.siguiente.anterior = derecha;
        hoja.siguiente = derecha;
        derecha.anterior = hoja;

        K sube = derecha.claves.get(0);
        eventos.add("DIVISION de hoja " + antes + " -> " + hoja.claves + " | " + derecha.claves
                + "   (sube COPIA de " + sube + " al padre)");
        return new Division<>(sube, derecha);
    }

    /**
     * Divide un nodo interno desbordado. La clave del medio SUBE (se mueve,
     * no se copia: los internos no guardan datos, solo guias).
     */
    private Division<K, V> dividirInterno(NodoInterno<K, V> nodo) {
        String antes = nodo.claves.toString();
        int mid = nodo.claves.size() / 2;
        K sube = nodo.claves.get(mid);

        NodoInterno<K, V> derecha = new NodoInterno<>();
        derecha.claves.addAll(nodo.claves.subList(mid + 1, nodo.claves.size()));
        derecha.hijos.addAll(nodo.hijos.subList(mid + 1, nodo.hijos.size()));
        nodo.claves.subList(mid, nodo.claves.size()).clear();
        nodo.hijos.subList(mid + 1, nodo.hijos.size()).clear();

        eventos.add("DIVISION de nodo interno " + antes + " -> " + nodo.claves + " | " + derecha.claves
                + "   (sube " + sube + " al padre)");
        return new Division<>(sube, derecha);
    }

    // ===================================================================
    //  BORRADO
    // ===================================================================

    /** Elimina la clave. Devuelve true si existia. */
    public boolean eliminar(K clave) {
        eventos.clear();
        boolean ok = eliminar(raiz, clave);
        // Si la raiz es interna y se quedo sin claves, su unico hijo pasa a ser la raiz.
        if (!raiz.esHoja() && raiz.claves.isEmpty()) {
            raiz = ((NodoInterno<K, V>) raiz).hijos.get(0);
            eventos.add("La raiz quedo sin claves -> su unico hijo es la nueva raiz (el arbol baja 1 nivel)");
        }
        if (!ok) eventos.add("La clave " + clave + " no existe: no se hizo nada.");
        return ok;
    }

    private boolean eliminar(Nodo<K, V> nodo, K clave) {
        if (nodo.esHoja()) {
            NodoHoja<K, V> hoja = (NodoHoja<K, V>) nodo;
            int pos = Collections.binarySearch(hoja.claves, clave);
            if (pos < 0) return false;
            hoja.claves.remove(pos);
            hoja.valores.remove(pos);
            tamanio--;
            eventos.add("Eliminada " + clave + " de la hoja -> " + hoja.claves);
            return true;
        }
        NodoInterno<K, V> padre = (NodoInterno<K, V>) nodo;
        int i = indiceHijo(padre, clave);
        Nodo<K, V> hijo = padre.hijos.get(i);
        boolean ok = eliminar(hijo, clave);
        // Al volver de la recursion, el hijo quedo por debajo del minimo (underflow)?
        if (ok && hijo.claves.size() < minClaves(hijo)) {
            eventos.add("UNDERFLOW en " + hijo.claves + " (minimo " + minClaves(hijo) + " claves)");
            arreglarUnderflow(padre, i);
        }
        return ok;
    }

    /** Intenta: 1) prestar del hermano izquierdo, 2) del derecho, 3) fusionar. */
    private void arreglarUnderflow(NodoInterno<K, V> padre, int i) {
        Nodo<K, V> izq = i > 0 ? padre.hijos.get(i - 1) : null;
        Nodo<K, V> der = i < padre.hijos.size() - 1 ? padre.hijos.get(i + 1) : null;

        if (izq != null && izq.claves.size() > minClaves(izq)) {
            prestarDeIzquierda(padre, i);
        } else if (der != null && der.claves.size() > minClaves(der)) {
            prestarDeDerecha(padre, i);
        } else if (izq != null) {
            fusionar(padre, i - 1);       // fusiona hijo(i-1) con hijo(i)
        } else {
            fusionar(padre, i);           // fusiona hijo(i) con hijo(i+1)
        }
    }

    private void prestarDeIzquierda(NodoInterno<K, V> padre, int i) {
        Nodo<K, V> nodo = padre.hijos.get(i);
        Nodo<K, V> izq = padre.hijos.get(i - 1);
        if (nodo.esHoja()) {
            NodoHoja<K, V> n = (NodoHoja<K, V>) nodo, l = (NodoHoja<K, V>) izq;
            int ult = l.claves.size() - 1;
            n.claves.add(0, l.claves.remove(ult));
            n.valores.add(0, l.valores.remove(ult));
            padre.claves.set(i - 1, n.claves.get(0));   // separador = nuevo minimo de la derecha
        } else {
            NodoInterno<K, V> n = (NodoInterno<K, V>) nodo, l = (NodoInterno<K, V>) izq;
            n.claves.add(0, padre.claves.get(i - 1));    // baja el separador del padre
            n.hijos.add(0, l.hijos.remove(l.hijos.size() - 1));
            padre.claves.set(i - 1, l.claves.remove(l.claves.size() - 1)); // sube la ultima de la izquierda
        }
        eventos.add("PRESTAMO del hermano izquierdo (queda " + izq.claves + ") -> el nodo queda " + nodo.claves
                + ", separador del padre ahora " + padre.claves);
    }

    private void prestarDeDerecha(NodoInterno<K, V> padre, int i) {
        Nodo<K, V> nodo = padre.hijos.get(i);
        Nodo<K, V> der = padre.hijos.get(i + 1);
        if (nodo.esHoja()) {
            NodoHoja<K, V> n = (NodoHoja<K, V>) nodo, r = (NodoHoja<K, V>) der;
            n.claves.add(r.claves.remove(0));
            n.valores.add(r.valores.remove(0));
            padre.claves.set(i, r.claves.get(0));        // separador = nuevo minimo de la derecha
        } else {
            NodoInterno<K, V> n = (NodoInterno<K, V>) nodo, r = (NodoInterno<K, V>) der;
            n.claves.add(padre.claves.get(i));            // baja el separador del padre
            n.hijos.add(r.hijos.remove(0));
            padre.claves.set(i, r.claves.remove(0));      // sube la primera de la derecha
        }
        eventos.add("PRESTAMO del hermano derecho (queda " + der.claves + ") -> el nodo queda " + nodo.claves
                + ", separador del padre ahora " + padre.claves);
    }

    /** Fusiona hijo(pos) con hijo(pos+1) y elimina el separador pos del padre. */
    private void fusionar(NodoInterno<K, V> padre, int pos) {
        Nodo<K, V> izq = padre.hijos.get(pos);
        Nodo<K, V> der = padre.hijos.get(pos + 1);
        String antes = izq.claves + " + " + der.claves;
        if (izq.esHoja()) {
            NodoHoja<K, V> l = (NodoHoja<K, V>) izq, r = (NodoHoja<K, V>) der;
            l.claves.addAll(r.claves);
            l.valores.addAll(r.valores);
            l.siguiente = r.siguiente;                    // saltar la hoja que desaparece
            if (r.siguiente != null) r.siguiente.anterior = l;
        } else {
            NodoInterno<K, V> l = (NodoInterno<K, V>) izq, r = (NodoInterno<K, V>) der;
            l.claves.add(padre.claves.get(pos));          // el separador BAJA al nodo fusionado
            l.claves.addAll(r.claves);
            l.hijos.addAll(r.hijos);
        }
        padre.claves.remove(pos);
        padre.hijos.remove(pos + 1);
        eventos.add("FUSION " + antes + " -> " + izq.claves + "   (padre queda " + padre.claves + ")");
    }

    // ===================================================================
    //  RECORRIDOS Y DATOS PARA LA VISTA
    // ===================================================================

    /** Todas las entradas en orden: solo se recorre la lista de hojas, O(n). */
    public List<Map.Entry<K, V>> todos() {
        List<Map.Entry<K, V>> out = new ArrayList<>();
        for (NodoHoja<K, V> h = hojaMasIzquierda(); h != null; h = h.siguiente)
            for (int i = 0; i < h.claves.size(); i++)
                out.add(new AbstractMap.SimpleEntry<>(h.claves.get(i), h.valores.get(i)));
        return out;
    }

    public List<K> claves() {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, V> e : todos()) out.add(e.getKey());
        return out;
    }

    private NodoHoja<K, V> hojaMasIzquierda() {
        Nodo<K, V> n = raiz;
        while (!n.esHoja()) n = ((NodoInterno<K, V>) n).hijos.get(0);
        return (NodoHoja<K, V>) n;
    }

    /**
     * Estructura del arbol por niveles, solo con las claves de cada nodo:
     * niveles().get(0) = [raiz], ..., ultimo nivel = hojas (en orden de enlace).
     * La Vista decide como dibujarlo.
     */
    public List<List<List<K>>> niveles() {
        List<List<List<K>>> resultado = new ArrayList<>();
        List<Nodo<K, V>> actual = new ArrayList<>();
        actual.add(raiz);
        while (!actual.isEmpty()) {
            List<List<K>> nivel = new ArrayList<>();
            List<Nodo<K, V>> siguiente = new ArrayList<>();
            for (Nodo<K, V> n : actual) {
                nivel.add(new ArrayList<>(n.claves));
                if (!n.esHoja()) siguiente.addAll(((NodoInterno<K, V>) n).hijos);
            }
            resultado.add(nivel);
            actual = siguiente;
        }
        return resultado;
    }

    // ===================================================================
    //  VERIFICACION DE INVARIANTES (sirve para probar que el arbol es valido)
    // ===================================================================

    /** Lanza IllegalStateException si se viola alguna propiedad del B+. */
    public void verificarInvariantes() {
        verificar(raiz, null, null, true);
        // Cadena de hojas: ordenada, sin saltos, con 'anterior' consistente y n elementos.
        int contados = 0;
        K previa = null;
        NodoHoja<K, V> prevHoja = null;
        for (NodoHoja<K, V> h = hojaMasIzquierda(); h != null; h = h.siguiente) {
            if (h.anterior != prevHoja) throw new IllegalStateException("Enlace 'anterior' incorrecto");
            for (K k : h.claves) {
                if (previa != null && previa.compareTo(k) >= 0)
                    throw new IllegalStateException("Hojas desordenadas en " + k);
                previa = k;
                contados++;
            }
            prevHoja = h;
        }
        if (contados != tamanio)
            throw new IllegalStateException("tamanio=" + tamanio + " pero hay " + contados + " claves");
    }

    /** Devuelve la profundidad de las hojas bajo 'n' (todas deben coincidir). */
    private int verificar(Nodo<K, V> n, K min, K max, boolean esRaiz) {
        int size = n.claves.size();
        if (size > orden - 1) throw new IllegalStateException("Nodo con demasiadas claves: " + n.claves);
        if (!esRaiz && size < minClaves(n)) throw new IllegalStateException("Nodo con pocas claves: " + n.claves);
        for (int i = 0; i < size; i++) {
            K k = n.claves.get(i);
            if (i > 0 && n.claves.get(i - 1).compareTo(k) >= 0)
                throw new IllegalStateException("Claves desordenadas: " + n.claves);
            if (min != null && k.compareTo(min) < 0) throw new IllegalStateException(k + " < cota " + min);
            if (max != null && k.compareTo(max) >= 0) throw new IllegalStateException(k + " >= cota " + max);
        }
        if (n.esHoja()) return 0;
        NodoInterno<K, V> in = (NodoInterno<K, V>) n;
        if (in.hijos.size() != size + 1) throw new IllegalStateException("hijos != claves+1 en " + n.claves);
        if (esRaiz && size < 1) throw new IllegalStateException("Raiz interna sin claves");
        int prof = -1;
        for (int i = 0; i <= size; i++) {
            K lo = i == 0 ? min : in.claves.get(i - 1);
            K hi = i == size ? max : in.claves.get(i);
            int p = verificar(in.hijos.get(i), lo, hi, false);
            if (prof == -1) prof = p;
            else if (prof != p) throw new IllegalStateException("Hojas a distinto nivel");
        }
        return prof + 1;
    }
}

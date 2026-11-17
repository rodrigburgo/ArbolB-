# Árbol B+ en Java con Arquitectura MVC

Implementación completa de un **Árbol B+ genérico (`BPlusTree<K, V>`)** en Java bajo el patrón **Modelo-Vista-Controlador (MVC)**. El proyecto abarca la definición formal de la estructura, operaciones fundamentales (búsqueda puntual, inserción, eliminación y búsqueda por rango) y validación automática de invariantes estructurales.

---

## Arquitectura del Proyecto (MVC)

El diseño separa estrictamente la lógica de negocio y algoritmos de la interacción por consola:

```text
src/bplustree/
├── Main.java                      # Punto de entrada: instancia Vista, Controlador y arranca la app
├── PruebaAleatoria.java           # Stress test comparativo contra java.util.TreeMap (315 000 ops)
├── model/                         # MODELO: Estructura de datos y reglas (cero dependencias de I/O)
│   ├── BPlusTree.java             # Implementación del Árbol B+ (insertar, buscar, eliminar, rango)
│   └── Estudiante.java            # Entidad de dominio de ejemplo (clave = código)
├── view/                          # VISTA: Entrada/salida por consola y renderizado visual
│   └── VistaConsola.java          # Menús, captura de datos y dibujo del árbol por niveles
└── controller/                    # CONTROLADOR: Orquestación de eventos y demostraciones
    └── Controlador.java           # Gestión del ciclo de menú, flujo MVC y demos guiadas
```

### Flujo de Ejecución (Ejemplo: Inserción)

```text
VistaConsola.leerEntero() ──▶ Controlador.insertar() ──▶ BPlusTree.insertar() ──▶ VistaConsola.mostrarArbol()
```

---

## Opciones del Menú y Demostraciones

La aplicación incluye tanto gestión interactiva como demostraciones paso a paso vinculadas a la documentación teórica:

| Opción | Ejemplo / Funcionalidad | Sección del HTML |
| :---: | :--- | :---: |
| **8** | Insertar `10, 20, 30, 40, 50` con `m = 3`: división de hojas, de nodo interno y de la raíz | `05 Inserción` |
| **9** | Borrado con *underflow*: fusión y préstamo (`m = 4`) | `06 Borrado` |
| **10** | Buscar `17`, buscar `99` (no existe) y rango `[10, 30]` con 20 claves (`m = 4`) | `04 y 07` |
| **1 - 7** | Índice de estudiantes por código: CRUD interactivo, rango, listado ordenado | `03, 08` |

> **Nota:** Cada paso de las demos muestra la **bitácora** de lo que hizo el árbol por dentro (división, préstamo, fusión), el **árbol dibujado por niveles** con las hojas enlazadas (`→`), y verifica que el árbol siga siendo válido.

---

## Correspondencia con la Teoría

Mapeo directo entre los conceptos formales del Árbol B+ y su ubicación exacta en el código fuente:

| Concepto | Dónde está en el código |
| :--- | :--- |
| Orden $m$, máximo $m - 1$ claves | `BPlusTree(int orden)`, `maxClaves()` |
| Datos solo en hojas | `NodoHoja.valores`; `NodoInterno` solo tiene `claves` e `hijos` |
| Hojas enlazadas | `NodoHoja.siguiente / anterior`, usadas en `buscarRango()` y `todos()` |
| Regla de enrutamiento | `indiceHijo()` |
| División de hoja | `dividirHoja()` |
| División de interno | `dividirInterno()` |
| El árbol crece solo por la raíz | Final de `insertar(K, V)` |
| Underflow, préstamo, fusión | `arreglarUnderflow()`, `prestarDe...()`, `fusionar()` |
| El árbol baja cuando la raíz queda vacía | Final de `eliminar(K)` |
| Invariantes | `verificarInvariantes()` |

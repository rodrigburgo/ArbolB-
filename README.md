Árbol B+ en Java con MVC
Proyecto Árbol B+ conlleva su definición formal, búsqueda, inserción, borrado y búsqueda por rango.

Estructura MVC

src/bplustree/
├── Main.java                      <- arma Vista + Controlador y arranca
├── PruebaAleatoria.java           <- prueba contra TreeMap (315 000 operaciones)
├── model/                         <- MODELO: datos y reglas, sin System.out
│   ├── BPlusTree.java             <- el árbol B+ (insertar, buscar, eliminar, rango)
│   └── Estudiante.java            <- registro de ejemplo (clave = código)
├── view/
│   └── VistaConsola.java          <- VISTA: todo print/scan, dibuja el árbol por niveles
└── controller/
    └── Controlador.java           <- CONTROLADOR: menú, conecta vista y modelo, demos
```Flujo de una operación (ejemplo: insertar): `Vista.leerEntero()` -> `Controlador.insertar()` -> `Modelo.insertar()` -> `Vista.mostrarArbol()`



## Qué ejemplos incluye (opciones del menú)

|Opción|Ejemplo|Sección del HTML| |-|-|-|
|8|Insertar 10,20,30,40,50 con m=3: división de hojas, de nodo interno y de la raíz|05 Inserción|
|9|Borrado con underflow: fusión y préstamo (m=4)|06 Borrado|
|10|Buscar 17, buscar 99 (no existe) y rango \[10, 30] con 20 claves (m=4)|04 y 07|
|1-7|Índice de estudiantes por código: CRUD interactivo, rango, listado ordenado|03, 08|

Cada paso de las demos muestra: la bitácora de lo que hizo el árbol por dentro (división, préstamo, fusión), el árbol dibujado por niveles con las hojas enlazadas (→), y verifica que el árbol siga siendo válido.

## Correspondencia con la teoría

|Concepto|Dónde está en el código|
|-|-|
|Orden m, máximo m-1 claves|`BPlusTree(int orden)`, `maxClaves()`|
|Datos solo en hojas|`NodoHoja.valores`; `NodoInterno` solo tiene `claves` e `hijos`|
|Hojas enlazadas|`NodoHoja.siguiente / anterior`, usadas en `buscarRango()` y `todos()`|
|Regla de enrutamiento |`indiceHijo()`|
|División de hoja |`dividirHoja()`|
|División de interno |`dividirInterno()`|
|El árbol crece solo por la raíz|final de `insertar(K, V)`|
|Underflow, préstamo, fusión|`arreglarUnderflow()`, `prestarDe...()`, `fusionar()`|
|El árbol baja cuando la raíz queda vacía|final de `eliminar(K)`|
|Invariantes|`verificarInvariantes()`|



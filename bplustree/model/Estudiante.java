package bplustree.model;

/**
 * MODELO: un registro de ejemplo. En una base de datos real, el arbol B+ seria
 * un INDICE: la clave (codigo) apunta al registro completo (el valor).
 * 'record' (Java 16+) genera constructor, getters, equals y toString solos.
 */
public record Estudiante(int codigo, String nombre, String carrera) {
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + carrera + ")";
    }
}

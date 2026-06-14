package org.example.dominio.catalogo;

public class Subcategoria {
    private String id;
    private String nombre;
    private TipoAtributo tipo; // El atributo es de la clase enum TipoAtributo

    // Constructor
    public Subcategoria(String id, String nombre, TipoAtributo tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    // Getters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoAtributo getTipo() { return tipo; }
}



//public class Subcategoria {
//    private String id;
//    private String nombre;
//    private TipoAtributo tipo; // Agregamos el tipo
//
//    public Subcategoria(String nombre) {
//        this.id = id;
//        this.nombre = nombre;
//        this.tipo = tipo;
//    }
//
//    public TipoAtributo getTipo() { return tipo; }
//    public String getNombre() { return nombre; }
//}

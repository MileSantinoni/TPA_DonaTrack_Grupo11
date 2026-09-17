package org.example.dominio.catalogo;

import java.util.ArrayList;
import java.util.List;

public class Categoria {
    private String nombre;
    private List<Subcategoria> subcategorias;

    public Categoria(String nombre) {
        this.nombre = nombre;
        this.subcategorias = new ArrayList<>();
    }

    public void agregarSubcategoria(Subcategoria subcategoria) {
        this.subcategorias.add(subcategoria);
    }


    public String getNombre() { return nombre; }
    public List<Subcategoria> getSubcategorias() { return subcategorias; }
}

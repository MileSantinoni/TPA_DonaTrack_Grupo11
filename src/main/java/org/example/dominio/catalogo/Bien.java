package org.example.dominio.catalogo;

import java.time.LocalDate;

public abstract class Bien {
    private String descripcion;
    private String foto; // Ruta, URL
    private int cantidad;
    private String unidadMedida;
    private Subcategoria subcategoria;

    public Bien(String descripcion, int cantidad, String unidadMedida, Subcategoria subcategoria) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.subcategoria = subcategoria;
        this.foto = null; //es opcional
    }


    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; } //para agregar foto

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad;}

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) {this.unidadMedida = unidadMedida;
    }
    public Subcategoria getSubcategoria() { return subcategoria; }
    public void setSubcategoria(Subcategoria subcategoria) { this.subcategoria = subcategoria;}
    
    public LocalDate getFechaVencimiento() { return null;}

    public Estado getEstado() { return null;}

}
   


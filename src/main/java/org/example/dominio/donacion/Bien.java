package org.example.dominio.donacion;

public abstract class Bien {
    public String descripcion;
    public String fotoUrl; // Opcional
    public int cantidad;
    public String unidadMedida; // Ej: kilogramos, unidades
    public Subcategoria subcategoria;


    public String getDescripcion() {
        return this.descripcion;
    }

    public String getFotoUrl() {
        return this.fotoUrl;
    }

    public int getCantidad() {
        return this.cantidad;
    }

    public String getUnidadMedida() {
        return this.unidadMedida;
    }

   
       /* return this.subcategoria;*/
    }
   
}

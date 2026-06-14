package org.example.dominio.catalogo;

import java.time.LocalDate;

public class Bien {
    private String id;
    private String descripcion;
    private String foto; // Ruta, URL
    private int cantidad;
    private String unidadMedida;
    private Subcategoria subcategoria;
    private LocalDate fechaVencimiento;
    private Estado Estado;

    public Bien(String id, String descripcion, int cantidad, String unidadMedida,
                Subcategoria subcategoria, LocalDate fechaVencimiento, Estado estado) {

        this.id = id;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.subcategoria = subcategoria;
//        this.Estado = estado;

        // ==========================================
        // LÓGICA DE VALIDACIÓN SEGÚN EL TIPO
        // ==========================================
        TipoAtributo tipoExigido = subcategoria.getTipo();

        if (tipoExigido == TipoAtributo.PERECEDERO && fechaVencimiento == null) {
            throw new IllegalArgumentException("Los bienes perecederos deben tener una fecha de vencimiento.");
        }

        if (tipoExigido == TipoAtributo.CON_ESTADO && estado == null) {
            throw new IllegalArgumentException("Se debe indicar el estado (NUEVO/USADO) para esta subcategoría.");
        }

        this.fechaVencimiento = fechaVencimiento;
        this.Estado = estado;
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
    
    public LocalDate getFechaVencimiento() { return fechaVencimiento;}

    public Estado getEstado() { return Estado;}

}
   


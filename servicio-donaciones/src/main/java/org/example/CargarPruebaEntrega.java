package org.example;

import java.time.LocalDate;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioCatalogo;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Categoria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.donante.Genero;

public class CargarPruebaEntrega {

  public static void main(String[] args) {
    String password = System.getenv("DONACIONES_DB_PASSWORD");
    if (password == null || password.isBlank()) {
      throw new IllegalStateException("Falta configurar DONACIONES_DB_PASSWORD");
    }

    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of("javax.persistence.jdbc.password", password)
        );

    try {
      EntityManager em = factory.createEntityManager();

      try {
        // 1) Asegurar subcategoría de prueba
        RepositorioCatalogo catalogo = new RepositorioCatalogo(em);
        Subcategoria subcategoria = null;
        if (catalogo.buscarSubcategoria("ALIMENTOS-SECOS").isEmpty()) {
          Categoria categoria = new Categoria("Alimentos de prueba");
          categoria.agregarSubcategoria(new Subcategoria(
              "ALIMENTOS-SECOS",
              "Alimentos secos",
              TipoAtributo.NO_PERECEDERO
          ));
          catalogo.agregarCategoria(categoria);
        }
        subcategoria = catalogo.buscarSubcategoria("ALIMENTOS-SECOS").get();

        // 2) Donante
        PersonaHumana donante = new PersonaHumana(
            "prueba@donaTrack.com",
            "270000000",
            TipoDocumento.DNI,
            "Juan",
            "Perez",
            30,
            Genero.MASCULINO,
            "Calle Falsa 123"
        );

        // 3) Entidad beneficiaria (ubicación en Buenos Aires)
        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
            "Comedor San Juan",
            "Av. Italia 1000",
            "11223344"
        );
        entidad.confirmarUbicacion(-34.6037, -58.3816);

        // 4) Donación
        Donacion donacion = new Donacion(
            "Donación de prueba",
            1,
            "UNIDAD",
            subcategoria,
            LocalDate.now(),
            Estado.NUEVO,
            donante
        );

        // 5) Asignación
        AsignacionDonacion asignacion = new AsignacionDonacion(
            donacion,
            entidad,
            null,
            LocalDate.now()
        );

        em.getTransaction().begin();
        RepositorioDonaciones repositorioDonaciones = new RepositorioDonaciones(em);
        repositorioDonaciones.agregar(donacion);
        em.getTransaction().commit();

        em.getTransaction().begin();
        RepositorioEntidadesBeneficiarias repositorioEntidades =
            new RepositorioEntidadesBeneficiarias(em);
        repositorioEntidades.agregar(entidad);
        em.getTransaction().commit();

        em.getTransaction().begin();
        RepositorioAsignacionesDonacion repositorioAsignaciones =
            new RepositorioAsignacionesDonacion(em);
        repositorioAsignaciones.agregar(asignacion);
        em.getTransaction().commit();

        // 6) Cambiar estado a EN_TRASLADO y luego a ENTREGADA
        em.getTransaction().begin();
        donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, "Iniciando traslado de prueba");
        em.getTransaction().commit();

        em.getTransaction().begin();
        donacion.cambiarEstado(EstadoDonacion.ENTREGADA, "Recepción confirmada en prueba");
        em.getTransaction().commit();

        System.out.println("Prueba de entrega cargada exitosamente.");
        System.out.println("Donante: " + donante.getMail());
        System.out.println("Donación: " + donacion.getIdAsString());
        System.out.println("Entidad: " + entidad.getRazonSocial());
        System.out.println("Asignación: " + asignacion.getIdAsString());
      } finally {
        em.close();
      }
    } finally {
      factory.close();
    }
  }
}

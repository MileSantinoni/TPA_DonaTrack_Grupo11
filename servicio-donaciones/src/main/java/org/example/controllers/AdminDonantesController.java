package org.example.controllers;

import io.javalin.http.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.Repositorios.RepositorioDonantes;
import org.example.dominio.donante.*;

public class AdminDonantesController {

  private static final String BASE = "/administracion/donantes";

  private final Supplier<EntityManager> proveedor;

  public AdminDonantesController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void index(Context ctx) {
    EntityManager em = proveedor.get();

    String q = texto(ctx.queryParam("q"));
    String tipo = texto(ctx.queryParam("tipo"));

    if (!List.of("", "HUMANA", "JURIDICA").contains(tipo)) {
      throw new BadRequestResponse("Tipo de persona inválido.");
    }

    int pagina = 1;

    try {
      if (ctx.queryParam("pagina") != null) {
        pagina = Integer.parseInt(ctx.queryParam("pagina"));
      }

      if (pagina < 1 || pagina > 100000) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Página inválida.");
    }

    String condiciones = " FROM Donante d WHERE 1 = 1";

    if (!tipo.isEmpty()) {
      condiciones += " AND TYPE(d) = "
          + (tipo.equals("HUMANA") ? "PersonaHumana" : "PersonaJuridica");
    }

    if (!q.isEmpty()) {
      condiciones += """
           AND (
             LOWER(d.mail) LIKE :q ESCAPE '!'
             OR LOWER(d.numeroDocumento) LIKE :q ESCAPE '!'
             OR d.id IN (
               SELECT h.id FROM PersonaHumana h
               WHERE LOWER(
                 CONCAT(CONCAT(h.nombre, ' '), COALESCE(h.apellido, ''))
               ) LIKE :q ESCAPE '!'
             )
             OR d.id IN (
               SELECT j.id FROM PersonaJuridica j
               WHERE LOWER(j.razonSocial) LIKE :q ESCAPE '!'
             )
           )
          """;
    }

    var cuenta = em.createQuery(
        "SELECT COUNT(d)" + condiciones,
        Long.class
    );

    var consulta = em.createQuery(
        "SELECT d" + condiciones + " ORDER BY d.id DESC",
        Donante.class
    );

    if (!q.isEmpty()) {
      String patron = "%" + q.toLowerCase(Locale.ROOT)
          .replace("!", "!!")
          .replace("%", "!%")
          .replace("_", "!_") + "%";

      cuenta.setParameter("q", patron);
      consulta.setParameter("q", patron);
    }

    long total = cuenta.getSingleResult();
    long paginas = Math.max(1, (total + 9) / 10);

    pagina = (int) Math.min(pagina, paginas);

    Map<String, Object> modelo = new HashMap<>();

    modelo.put(
        "donantes",
        consulta.setFirstResult((pagina - 1) * 10)
            .setMaxResults(10)
            .getResultList()
            .stream()
            .map(this::datos)
            .toList()
    );

    modelo.put("q", q);
    modelo.put("humana", tipo.equals("HUMANA"));
    modelo.put("juridica", tipo.equals("JURIDICA"));
    modelo.put("total", total);
    modelo.put("pagina", pagina);
    modelo.put("paginas", paginas);
    modelo.put("hayPaginacion", paginas > 1);

    String enlace = BASE
        + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
        + "&tipo=" + tipo
        + "&pagina=";

    if (pagina > 1) {
      modelo.put("anterior", enlace + (pagina - 1));
    }

    if (pagina < paginas) {
      modelo.put("siguiente", enlace + (pagina + 1));
    }

    ctx.render("adminDonantes.hbs.hbs", modelo);
  }

  public void detalle(Context ctx) {
    ctx.json(datos(buscar(ctx)));
  }

  public void guardar(Context ctx) {
    EntityManager em = proveedor.get();

    String id = ctx.pathParamMap().get("id");
    Donante actual = id == null ? null : buscar(ctx);

    String tipo = campo(ctx, "type");

    if (!List.of("HUMANA", "JURIDICA").contains(tipo)) {
      throw new BadRequestResponse("Tipo de persona inválido.");
    }

    boolean humana = tipo.equals("HUMANA");

    if (actual != null && humana != (actual instanceof PersonaHumana)) {
      throw new BadRequestResponse(
          "No se puede cambiar el tipo de una persona registrada."
      );
    }

    String mail = obligatorio(campo(ctx, "email"), "Email")
        .toLowerCase(Locale.ROOT);

    if (!mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new BadRequestResponse("Ingresá un email válido.");
    }

    String documento = obligatorio(campo(ctx, "doc"), "Documento");

    TipoDocumento tipoDoc = enumerado(
        TipoDocumento.class,
        campo(ctx, "tipoDoc")
    );

    if (!humana && tipoDoc != TipoDocumento.CUIT) {
      throw new BadRequestResponse("La persona jurídica requiere CUIT.");
    }

    long duplicados = em.createQuery("""
            SELECT COUNT(d) FROM Donante d
            WHERE d.id <> :id
              AND (
                LOWER(d.mail) = :mail
                OR (
                  d.tipoDeDocumento = :tipo
                  AND d.numeroDocumento = :doc
                )
              )
            """, Long.class)
        .setParameter("id", actual == null ? -1L : actual.getId())
        .setParameter("mail", mail)
        .setParameter("tipo", tipoDoc)
        .setParameter("doc", documento)
        .getSingleResult();

    if (duplicados > 0) {
      throw new ConflictResponse(
          "Ya existe un donante con ese email o documento."
      );
    }

    // Validamos antes de modificar las entidades.
    String nombre = humana
        ? obligatorio(campo(ctx, "first"), "Nombre")
        : "";

    String apellido = campo(ctx, "last");

    String razon = humana
        ? ""
        : obligatorio(campo(ctx, "name"), "Razón social");

    int edad = 0;
    Genero genero = Genero.OTRO;
    TipoOrganizacion organizacion = TipoOrganizacion.EMPRESA;

    if (humana) {
      try {
        edad = Integer.parseInt(campo(ctx, "age"));

        if (edad < 0) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException e) {
        throw new BadRequestResponse(
            "La edad debe ser un número entero mayor o igual a cero."
        );
      }

      genero = enumerado(Genero.class, campo(ctx, "gender"));
    } else {
      organizacion = enumerado(
          TipoOrganizacion.class,
          campo(ctx, "org")
      );
    }

    String rep = campo(ctx, "rep");
    String repLast = campo(ctx, "repLast");
    String repMail = campo(ctx, "repEmail");

    boolean tieneRepresentante =
        !humana && !(rep + repLast + repMail).isBlank();

    if (tieneRepresentante
        && (rep.isBlank()
        || repLast.isBlank()
        || !repMail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
      throw new BadRequestResponse(
          "Completá nombre, apellido y email válido del representante."
      );
    }

    var tx = em.getTransaction();
    tx.begin();

    try {
      Donante d;

      if (humana) {
        PersonaHumana p = actual == null
            ? new PersonaHumana(
            mail, documento, tipoDoc,
            nombre, apellido, edad, genero,
            campo(ctx, "address")
        )
            : (PersonaHumana) actual;

        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setEdad(edad);
        p.setGenero(genero);
        p.setDireccion(campo(ctx, "address"));

        d = p;
      } else {
        PersonaJuridica p = actual == null
            ? new PersonaJuridica(
            mail, documento, tipoDoc,
            razon, organizacion, campo(ctx, "rubro")
        )
            : (PersonaJuridica) actual;

        p.setRazonSocial(razon);
        p.setTipo(organizacion);
        p.setRubro(campo(ctx, "rubro"));

        Representante r = p.getRepresentantes().stream()
            .min(Comparator.comparing(Representante::getId))
            .orElse(null);

        if (tieneRepresentante) {
          if (r == null) {
            p.agregarRepresentante(
                new Representante(rep, repLast, repMail)
            );
          } else {
            r.setNombre(rep);
            r.setApellido(repLast);
            r.setEmail(repMail);
          }
        } else if (r != null) {
          p.getRepresentantes().remove(r);
        }

        d = p;
      }

      d.actualizarDatos(mail, documento, tipoDoc);

      String telefono = campo(ctx, "phone");

      MedioContacto medio = d.getMediosDeContacto().stream()
          .filter(m -> m.getTipo() == TipoMedioContacto.TELEFONO)
          .min(Comparator.comparing(MedioContacto::getId))
          .orElse(null);

      if (!telefono.isBlank()) {
        if (medio == null) {
          d.agregarMedioContacto(
              new MedioContacto(TipoMedioContacto.TELEFONO, telefono)
          );
        } else {
          medio.setNumero(telefono);
        }
      } else if (medio != null) {
        if (d.getContactoPredeterminado()
            == TipoContactoPredeterminado.TELEFONO) {
          throw new BadRequestResponse(
              "El teléfono es el contacto predeterminado; no puede quedar vacío."
          );
        }

        d.getMediosDeContacto().remove(medio);
      }

      if (actual == null) {
        em.persist(d);
      }

      tx.commit();

      ctx.status(actual == null ? 201 : 200)
          .json(Map.of(
              "mensaje",
              "Donante guardado correctamente."
          ));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  public void eliminar(Context ctx) {
    EntityManager em = proveedor.get();
    Donante d = buscar(ctx);

    var tx = em.getTransaction();
    tx.begin();

    try {
      long donaciones = em.createQuery(
              "SELECT COUNT(x) FROM Donacion x WHERE x.donante.id = :id",
              Long.class
          )
          .setParameter("id", d.getId())
          .getSingleResult();

      long registros = em.createQuery(
              "SELECT COUNT(x) FROM RegistroDonacion x WHERE x.donante.id = :id",
              Long.class
          )
          .setParameter("id", d.getId())
          .getSingleResult();

      if (donaciones + registros > 0) {
        throw new ConflictResponse(
            "No se puede eliminar: tiene donaciones o cargas registradas."
        );
      }

      em.remove(d);
      tx.commit();

      ctx.json(Map.of("mensaje", "Donante eliminado."));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  public void importar(Context ctx) throws IOException {
    var archivo = ctx.uploadedFile("archivo");

    if (archivo == null) {
      throw new BadRequestResponse("Seleccioná un archivo CSV.");
    }

    if (archivo.size() > 10 * 1024 * 1024) {
      throw new BadRequestResponse("El máximo es 10 MB.");
    }

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    tx.begin();

    try (Reader reader = new InputStreamReader(
        archivo.content(),
        StandardCharsets.UTF_8.newDecoder()
    )) {
      var importador = new ImportadorCSV(
          new RepositorioDonantes(em)
      );

      importador.importar(reader);

      tx.commit();

      ctx.json(Map.of(
          "mensaje",
          "Importación completa: "
              + importador.getDonantesCreados()
              + " creados y "
              + importador.getDonantesActualizados()
              + " actualizados."
      ));
    } catch (IllegalArgumentException | IOException | UncheckedIOException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw new BadRequestResponse(
          "No se importó ninguna fila. " + e.getMessage()
      );
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private Donante buscar(Context ctx) {
    long id;

    try {
      id = Long.parseLong(ctx.pathParam("id"));
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("ID inválido.");
    }

    Donante d = proveedor.get().find(Donante.class, id);

    if (d == null) {
      throw new NotFoundResponse("El donante ya no existe.");
    }

    return d;
  }

  private Map<String, Object> datos(Donante d) {
    Map<String, Object> m = new HashMap<>();

    m.put("id", d.getId().toString());
    m.put("email", texto(d.getMail()));
    m.put("doc", texto(d.getNumeroDocumento()));
    m.put("tipoDoc", Objects.toString(d.getTipoDeDocumento(), "DNI"));

    m.put(
        "phone",
        d.getMediosDeContacto().stream()
            .filter(x -> x.getTipo() == TipoMedioContacto.TELEFONO)
            .min(Comparator.comparing(MedioContacto::getId))
            .map(MedioContacto::getNumero)
            .orElse("")
    );

    if (d instanceof PersonaHumana p) {
      m.put("type", "HUMANA");
      m.put("tipoNombre", "Persona humana");
      m.put(
          "name",
          (texto(p.getNombre()) + " " + texto(p.getApellido())).trim()
      );
      m.put("first", texto(p.getNombre()));
      m.put("last", texto(p.getApellido()));
      m.put("age", p.getEdad());
      m.put("gender", Objects.toString(p.getGenero(), "OTRO"));
      m.put("address", texto(p.getDireccion()));
    } else if (d instanceof PersonaJuridica p) {
      m.put("type", "JURIDICA");
      m.put("tipoNombre", "Persona jurídica");
      m.put("name", texto(p.getRazonSocial()));
      m.put("org", Objects.toString(p.getTipo(), "EMPRESA"));
      m.put("rubro", texto(p.getRubro()));

      p.getRepresentantes().stream()
          .min(Comparator.comparing(Representante::getId))
          .ifPresent(r -> {
            m.put("rep", texto(r.getNombre()));
            m.put("repLast", texto(r.getApellido()));
            m.put("repEmail", texto(r.getEmail()));
          });
    }

    m.put(
        "iniciales",
        Arrays.stream(m.get("name").toString().split("\\s+"))
            .filter(s -> !s.isEmpty())
            .limit(2)
            .map(s -> s.substring(0, 1))
            .reduce("", String::concat)
            .toUpperCase(Locale.ROOT)
    );

    return m;
  }

  private static String texto(String s) {
    return s == null ? "" : s.trim();
  }

  private static String campo(Context c, String nombre) {
    return texto(c.formParam(nombre));
  }

  private static String obligatorio(String s, String nombre) {
    if (s.isBlank()) {
      throw new BadRequestResponse(nombre + " es obligatorio.");
    }

    return s;
  }

  private static <E extends Enum<E>> E enumerado(
      Class<E> clase,
      String valor
  ) {
    try {
      return Enum.valueOf(clase, valor);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("Valor inválido: " + valor);
    }
  }
}
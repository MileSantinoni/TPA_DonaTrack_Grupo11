package org.example.api.donaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.EventoLogistico;
import org.example.dominio.donacion.SeguimientoLogistico;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;

public final class DonacionesGraphQL {

  private static final String CABECERA = "X-Planificacion-Token";

  private static final ObjectMapper JSON = new ObjectMapper()
      .findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final RepositorioAsignacionesDonacion asignaciones;
  private final RepositorioDonaciones donaciones;
  private final SeguimientoLogistico seguimiento;
  private final GraphQL graphql;

  public record Peticion(
      String query,
      Map<String, Object> variables,
      String operationName
  ) {
  }

  public DonacionesGraphQL(Supplier<EntityManager> proveedor) {
    asignaciones = new RepositorioAsignacionesDonacion(proveedor);
    donaciones = new RepositorioDonaciones(proveedor);

    Notificador notificador =
        new Notificador(new Email(), new SMS(), new WhatsApp());

    seguimiento = new SeguimientoLogistico(
        asignaciones,
        notificador
    );

    RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
        .type("Query", tipo -> tipo
            .dataFetcher(
                "asignaciones",
                proteger(entorno ->
                    asignaciones.buscarTodas().stream()
                        .map(this::convertir)
                        .toList()
                )
            )
            .dataFetcher(
                "asignacion",
                proteger(entorno -> {
                  String id = entorno.getArgument("id");

                  return asignaciones.buscarPorId(id)
                      .map(this::convertir)
                      .orElse(null);
                })
            )
            .dataFetcher(
                "existeDonacion",
                proteger(entorno -> {
                  String id = entorno.getArgument("id");
                  return donaciones.buscarPorId(id).isPresent();
                })
            )
        )
        .type("Mutation", tipo -> tipo
            .dataFetcher(
                "registrarEvento",
                proteger(entorno -> {
                  Map<String, Object> input =
                      entorno.getArgument("input");

                  EventoLogistico evento = JSON.convertValue(
                      input,
                      EventoLogistico.class
                  );

                  seguimiento.registrar(evento);

                  return true;
                })
            )
        )
        .build();

    var esquema = new SchemaGenerator().makeExecutableSchema(
        new SchemaParser().parse(leerEsquema()),
        wiring
    );

    graphql = GraphQL.newGraphQL(esquema).build();
  }

  public void registrar(Javalin app) {
    app.post("/graphql", ctx -> {
      String esperado = System.getenv("PLANIFICACION_TOKEN");

      if (esperado == null || esperado.isBlank()) {
        ctx.status(503).json(errorHttp(
            "Falta configurar PLANIFICACION_TOKEN en Donaciones"
        ));
        return;
      }

      String recibido = ctx.header(CABECERA);

      if (recibido == null || !MessageDigest.isEqual(
          esperado.getBytes(StandardCharsets.UTF_8),
          recibido.getBytes(StandardCharsets.UTF_8)
      )) {
        ctx.status(403).json(errorHttp(
            "Credencial interna inválida"
        ));
        return;
      }

      Peticion peticion;

      try {
        peticion = ctx.bodyAsClass(Peticion.class);
      } catch (RuntimeException e) {
        ctx.status(400).json(errorHttp(
            "El cuerpo debe ser una petición GraphQL válida"
        ));
        return;
      }

      if (peticion == null
          || peticion.query() == null
          || peticion.query().isBlank()) {

        ctx.status(400).json(errorHttp(
            "Falta el documento GraphQL en query"
        ));
        return;
      }

      var entrada = ExecutionInput.newExecutionInput()
          .query(peticion.query())
          .variables(
              peticion.variables() == null
                  ? Map.of()
                  : peticion.variables()
          );

      if (peticion.operationName() != null
          && !peticion.operationName().isBlank()) {
        entrada.operationName(peticion.operationName());
      }

      var resultado = graphql.execute(entrada.build());

      ctx.header("Cache-Control", "no-store");
      ctx.json(resultado.toSpecification());
    });
  }

  private Map<String, Object> convertir(
      AsignacionDonacion asignacion
  ) {
    var entidad = asignacion.getEntidad();
    var donacion = asignacion.getDonacion();

    // HashMap admite coordenadas y datos opcionales nulos.
    Map<String, Object> datos = new HashMap<>();

    datos.put("idDonacion", donacion.getIdAsString());
    datos.put("idEntidad", entidad.getIdAsString());
    datos.put("razonSocial", entidad.getRazonSocial());
    datos.put("direccion", entidad.getDireccion());
    datos.put("telefono", entidad.getTelefono());
    datos.put("estadoDonacion", donacion.getEstadoActual().name());
    datos.put("latitud", entidad.getLatitud());
    datos.put("longitud", entidad.getLongitud());

    return datos;
  }

  private static DataFetcher<Object> proteger(
      DataFetcher<?> operacion
  ) {
    return entorno -> {
      try {
        return operacion.get(entorno);
      } catch (IllegalArgumentException e) {
        return DataFetcherResult.newResult()
            .error(
                GraphqlErrorBuilder.newError(entorno)
                    .message(mensaje(e))
                    .extensions(Map.of("status", 400))
                    .build()
            )
            .build();
      } catch (IllegalStateException e) {
        return DataFetcherResult.newResult()
            .error(
                GraphqlErrorBuilder.newError(entorno)
                    .message(mensaje(e))
                    .extensions(Map.of("status", 409))
                    .build()
            )
            .build();
      }
    };
  }

  private static String mensaje(RuntimeException e) {
    return e.getMessage() == null
        ? "Operación rechazada"
        : e.getMessage();
  }

  private static Map<String, Object> errorHttp(String mensaje) {
    return Map.of(
        "errors",
        List.of(Map.of("message", mensaje))
    );
  }

  private static String leerEsquema() {
    try (var archivo = DonacionesGraphQL.class
        .getResourceAsStream("/graphql/donaciones.graphqls")) {

      if (archivo == null) {
        throw new IllegalStateException(
            "No se encontró graphql/donaciones.graphqls"
        );
      }

      return new String(
          archivo.readAllBytes(),
          StandardCharsets.UTF_8
      );
    } catch (IOException e) {
      throw new IllegalStateException(
          "No se pudo leer el esquema GraphQL de Donaciones",
          e
      );
    }
  }
}
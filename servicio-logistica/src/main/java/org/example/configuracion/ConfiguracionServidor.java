package org.example.configuracion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ConfiguracionServidor {

  private ConfiguracionServidor() {
  }

  public static Map<String, Object> persistencia(String servicio) {
    if (!Set.of("DONACIONES", "LOGISTICA").contains(servicio)) {
      throw new IllegalArgumentException("Servicio desconocido");
    }

    String password = System.getenv(servicio + "_DB_PASSWORD");

    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "Falta " + servicio + "_DB_PASSWORD"
      );
    }

    String ddl = valor(servicio + "_DB_DDL", "update");

    if (!Set.of("validate", "update", "none").contains(ddl)) {
      throw new IllegalStateException(
          servicio + "_DB_DDL debe ser validate, update o none; "
              + "no se permite recrear la base."
      );
    }

    String base = servicio.equals("DONACIONES")
        ? "donatrack_donaciones"
        : "donatrack_logistica";

    String url = valor(
        servicio + "_DB_URL",
        "jdbc:postgresql://localhost:5432/" + base
    );

    if (!url.startsWith("jdbc:postgresql://")) {
      throw new IllegalStateException(
          servicio + "_DB_URL debe ser una URL JDBC de PostgreSQL."
      );
    }

    Map<String, Object> propiedades = new HashMap<>();

    propiedades.put("javax.persistence.jdbc.url", url);

    propiedades.put(
        "javax.persistence.jdbc.user",
        valor(servicio + "_DB_USER", "postgres")
    );

    propiedades.put("javax.persistence.jdbc.password", password);
    propiedades.put("hibernate.hbm2ddl.auto", ddl);
    propiedades.put("hibernate.show_sql", "false");
    propiedades.put("hibernate.format_sql", "false");

    return propiedades;
  }

  public static int puerto(int predeterminado) {
    try {
      int puerto = Integer.parseInt(
          valor("PORT", Integer.toString(predeterminado))
      );

      if (puerto < 1 || puerto > 65535) {
        throw new NumberFormatException();
      }

      return puerto;
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "PORT debe ser un número entre 1 y 65535."
      );
    }
  }

  private static String valor(
      String nombre,
      String predeterminado
  ) {
    String valor = System.getenv(nombre);

    return valor == null || valor.isBlank()
        ? predeterminado
        : valor.trim();
  }
}
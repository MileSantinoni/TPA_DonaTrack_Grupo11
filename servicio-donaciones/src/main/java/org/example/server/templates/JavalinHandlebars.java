package org.example.server.templates;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.FileRenderer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;


public class JavalinHandlebars implements FileRenderer {

  Handlebars handlebars = new Handlebars();
  @NotNull
  @Override
  public String render(@NotNull String path, @NotNull Map<String, ?> model, @NotNull Context context) {
    Template template = null;
    try {
      template = handlebars.compile("templates/" + path.replace(".hbs",""));
      Map<String, Object> vista = new java.util.HashMap<>(model);
      org.example.seguridad.SesionWeb.modelo(context, vista);
      return template.apply(vista);
    } catch (IOException e) {
      e.printStackTrace();
      context.status(HttpStatus.NOT_FOUND);
      return "No se encuentra la página indicada...";
    }
  }
}

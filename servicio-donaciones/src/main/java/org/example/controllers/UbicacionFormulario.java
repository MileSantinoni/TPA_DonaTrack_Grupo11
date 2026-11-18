package org.example.controllers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public record UbicacionFormulario(
    String direccion,
    double latitud,
    double longitud
) {

  public static UbicacionFormulario leer(Context ctx) {
    String direccion = ctx.formParam("direccion");

    if (direccion == null
        || direccion.isBlank()
        || direccion.trim().length() > 180) {
      throw new BadRequestResponse(
          "Seleccioná una dirección válida de hasta 180 caracteres."
      );
    }

    if (!"true".equals(ctx.formParam("ubicacionConfirmada"))) {
      throw new BadRequestResponse(
          "Confirmá la ubicación de entrega en el mapa."
      );
    }

    try {
      double latitud = Double.parseDouble(ctx.formParam("latitud"));
      double longitud = Double.parseDouble(ctx.formParam("longitud"));

      if (!Double.isFinite(latitud)
          || !Double.isFinite(longitud)
          || Math.abs(latitud) > 90
          || Math.abs(longitud) > 180) {
        throw new IllegalArgumentException();
      }

      return new UbicacionFormulario(
          direccion.trim(),
          latitud,
          longitud
      );

    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BadRequestResponse(
          "La ubicación no es válida. Seleccioná nuevamente la dirección."
      );
    }
  }
}
package org.example.controllers;

import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;

public class LandingController {

  public void index(Context ctx) {
    Map<String, Object> model = new HashMap<>();
    model.put("titulo", "Bienvenido");

    ctx.render("landingPage.hbs", model);
  }
}
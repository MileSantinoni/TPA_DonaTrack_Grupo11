package org.example.server;

import io.javalin.Javalin;

public class App {

  public static void main(String[] args) {
    Javalin app = Server.crearApp();

    Runtime.getRuntime().addShutdownHook(
        new Thread(app::stop)
    );

    app.start(8083);
  }
}
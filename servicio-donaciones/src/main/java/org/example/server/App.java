package org.example.server;

public class App {

  public static void main(String[] args) {
//    Bootstrap.init();
    new Server().start(8083);
  }
}

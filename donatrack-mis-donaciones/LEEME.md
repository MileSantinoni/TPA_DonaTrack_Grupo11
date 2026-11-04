# Mis donaciones — maqueta Handlebars

Copiá la carpeta `servicio-donaciones` de este ZIP en la raíz del proyecto.
Se agregan archivos nuevos: no reemplaza la landing ni main.css.
Requiere el main.css compartido que ya extrajimos de landing.css (fuentes, colores, botones y badges).

## Dar de alta la vista

En `servicio-donaciones/src/main/java/org/example/server/Router.java`, dentro de
`configure(Javalin app)`, junto a las rutas de vistas, agregá:

```java
app.get("/mis-donaciones", ctx ->
    ctx.render("misDonaciones.hbs", java.util.Map.of()));
```

Levantá `org.example.server.App` y abrí http://localhost:8083/mis-donaciones.
Si hay estilos en caché, Ctrl+F5.

## Alcance

- Handlebars y CSS, sin React, Tailwind ni dependencias nuevas.
- Usa los estilos globales de main.css y agrega estilos acotados a esta página.
- Cabecera autenticada visual con Ana como persona de ejemplo; no implementa sesión.
- Tres donaciones fijas. Los filtros se aplican en memoria al tocar Buscar; Limpiar restablece la lista.
- Cards desplegables con HTML details/summary. Primera card abierta inicialmente.
- Mapa estático original de Buenos Aires y recorrido ilustrativo; no consulta logística.
- Cuatro hitos, tal como Figma agrupa Asignada / Lista para entregar.
- La pestaña Entidades y la campana muestran un aviso de maqueta; sus pantallas quedan pendientes.
- No se agregan footer ni hero ilustrado porque no aparecen en este frame de Figma.
- No cambia ni consulta datos de la base.

Fuente: Figma DonaTrack, página Donante · Pantallas, frame 74:4.
Los assets fueron descargados: no dependen de URLs temporales de Figma.
El mapa conserva la atribución a OpenStreetMap.

# Estado actual de DonaTrack

Última actualización: 2026-09-17.

## Objetivo y alcance acordados

El usuario trajo cambios de `main` a su branch y está reestructurando el proyecto raíz según una corrección del docente: eliminar la capa de services que se limita a coordinar llamadas. Los controllers reciben solicitudes HTTP, invocan comportamiento del dominio y construyen respuestas; los objetos del dominio orquestan los casos de uso.

Trabajar sobre `src/main/java/org/example/`, sus tests en `src/test/`, el `pom.xml` raíz y la documentación. **No modificar `servicio-donaciones` ni `servicio-logistica` sin una nueva indicación explícita.** Se hicieron cambios allí al principio por una interpretación incorrecta del alcance, pero fueron revertidos y se verificó que ambos directorios quedaron sin modificaciones.

Los enunciados completos de las entregas 1, 2 y 3 están en `AGENTS.md`. Describen requisitos, no funcionalidades ya implementadas. No se creó una copia en `docs/enunciado.md` para evitar mantener dos versiones del mismo texto.

## Decisiones de estructura

- No agregar services de aplicación como intermediarios entre controllers y dominio. Cambiar únicamente el nombre de un service no satisface la corrección.
- La infraestructura de integración HTTP puede existir como adaptador; no debe confundirse con la orquestación del negocio.
- El package solicitado por el usuario se llama `org.example.Routes`, con mayúscula, y contiene `RoutesLogistica` y `RoutesDonaciones`.
- Las clases de rutas solo registran endpoints mediante `registrar(...)`, recibiendo Javalin y los controllers ya construidos.
- `LogisticaApplication.crearApp(...)` configura JSON/Javalin, instancia los componentes y llama a ambos registradores. `main()` inicia la aplicación en el puerto 8080.

Flujo acordado: solicitud HTTP → endpoint registrado en Routes → controller → dominio → respuesta HTTP del controller.

## Cambios realizados en el proyecto raíz

### Eliminación de services

- Se eliminaron `service/RutaService.java` y `service/NotificacionesService.java`.
- El registro de rutas pasó a `MonitorCamiones.registrarRuta(PlanDeRuta, ...)`. `RutaRequest.aPlan()` convierte el DTO HTTP en el objeto de dominio `PlanDeRuta`.
- `RutaController` llama al dominio para registrar e iniciar rutas.
- `Donacion.asignarA(entidad, notificador)` realiza la asignación y solicita las notificaciones. `AsignacionController` invoca ese método y guarda la asignación en el repositorio.
- Los mensajes de asignación pasaron al `Notificador` existente del dominio.
- `Donante.notificarAusencia(...)` evalúa su historial; `VerificadorAusenciaDonantes` programa la ejecución y proporciona los registros y el notificador.
- `ClienteGeneradorRutas` se movió de `service` a `integracion`, retirando la anotación `@Service`. Su diseño interno todavía requiere revisión: conserva acceso a repositorios y devuelve DTOs de la API.
- Se adaptaron referencias y tests. `RutaServiceTest` fue reemplazado por `dominio/logistica/RegistroRutaTest`. Algunos otros tests aún están físicamente bajo la carpeta `service` aunque ya no prueban esa capa.
- `Ruta` y `Entrega` ya contenían comportamiento de inicio, recepción, fallo y retorno antes de esta sesión; no atribuir toda esa implementación al refactor actual.

### Separación del registro de endpoints

- `src/main/java/org/example/Routes/RoutesLogistica.java`: endpoints de camiones y rutas.
- `src/main/java/org/example/Routes/RoutesDonaciones.java`: endpoints de asignaciones, donantes, donaciones, entidades y necesidades.
- `src/main/java/org/example/LogisticaApplication.java`: configuración, construcción de componentes, registro de rutas y arranque.
- Se conservaron los métodos HTTP, las URLs y los métodos de controllers previamente registrados.

## Verificaciones realizadas

- Se detectó y corrigió un error introducido al construir `UbicacionCamion` en `RutaRequest.aPlan()`: el constructor requiere latitud, longitud, velocidad y fecha/hora. Se utiliza velocidad 0 y la fecha/hora actual para el depósito.
- `mvn test -q` terminó con código 0 tras la corrección.
- Se ejecutó también `mvn clean test -q`: los reportes Surefire indicaron 80 tests, 0 fallos, 0 errores y 3 omitidos (77 aprobados). La ejecución redirigida mediante PowerShell reportó código 1 y `NativeCommandError` por salida de stderr; no confundir ese resultado del shell con una ejecución completamente limpia.
- Después de separar Routes, `mvn "-Dtest=LogisticaHttpTest,LogisticaApiJavalinTest" test -q` terminó con código 0: 7 tests aprobados, sin errores ni omisiones.
- En los tests aparecen excepciones de correo por `Email(null)`. `Email.enviar()` captura la excepción y marca la notificación fallida; por eso los tests pueden aprobar sin que se envíen correos.
- `git diff --check` no detectó errores de whitespace; mostró una advertencia de conversión LF/CRLF.
- No se verificó el cumplimiento integral de las tres entregas ni el envío real de notificaciones. Los tests existentes no garantizan un flujo completo correcto.

## Pendientes postergados expresamente

El usuario pidió dejar estos problemas para después y continuar con la reestructura. No abordarlos automáticamente al retomar:

1. Registrar una ruta no cambia sus donaciones a `LISTA_PARA_ENTREGAR`, aunque `Entrega.validarInicioTraslado()` exige ese estado.
2. La configuración de notificaciones utiliza `Email(null)`, lo que impide enviar correo real.
3. `Donacion.asignarA(...)` selecciona la primera necesidad de la entidad sin comprobar que corresponda a la subcategoría de la donación.
4. El cliente del planificador sigue siendo síncrono. Quedan por resolver callback, lotes de hasta 100 donaciones y replanificación de las no asignadas según el enunciado.
5. Revisar si `MonitorCamiones` debe responsabilizarse también del registro de rutas; actualmente mezcla esa responsabilidad con el seguimiento.

La política de ausencia conserva el comportamiento previo: notifica exactamente a los 21 días desde el último registro de donación. No se auditó su equivalencia con todas las interacciones de plataforma mencionadas en el TP.

## Cómo retomar

1. Leer las instrucciones de `AGENTS.md` y este archivo.
2. Revisar `git status` y los archivos actuales antes de asumir que el código sigue igual. Hay cambios staged y unstaged; esta sesión no creó commits ni hizo push.
3. Continuar con la siguiente indicación del usuario dentro del alcance raíz. La última tarea de código completada fue separar el registro de endpoints en Routes.
4. Actualizar este documento al terminar, registrando qué se cambió, qué se verificó y qué quedó pendiente.

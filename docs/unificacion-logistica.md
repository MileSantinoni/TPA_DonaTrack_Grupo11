# Unificación de Logística

Base: `308e20a`, con `unificar-donaciones-v2.patch` ya aplicado.

`servicio-logistica` recupera las reglas del `src` raíz y mantiene sus entidades
independientes de Donaciones. Los controllers Javalin delegan en el dominio;
`integracion` contiene los clientes HTTP Java 17. Se eliminan `RutaService` y
`TrazabilidadEntregaService`; `ClienteGeneradorRutas` pasa a `integracion`, sin Spring.

## Responsabilidades

- `MonitorCamiones.registrarRuta`: resuelve las asignaciones por HTTP antes de
  registrar la ruta y reservar el camión. Conserva el ID y los datos reales de la
  entidad destinataria; utiliza las coordenadas del depósito recibidas.
- `Ruta.iniciar`: valida las entregas y envía un único evento para todo el lote.
  Cambia los estados locales después de la confirmación de Donaciones.
- `Entrega`: valida recepción, fotos, entrega fallida y retorno. Conserva fecha
  de recepción y camión responsable. Informa las transiciones por HTTP.
- `SeguimientoLogistico`, dentro del dominio de Donaciones: valida el lote
  completo, aplica las transiciones mediante el patrón State existente y pide
  las notificaciones al `Notificador`. No importa entidades de Logística.
- `ClienteGeneradorRutas`: mantiene el contrato JSON y la modalidad síncrona
  existente; obtiene las asignaciones mediante `ClienteDonaciones`.

## HTTP

Se conservan los endpoints previos de ambos servicios. Se agregan:

| Servicio | Método y ruta | JSON |
| --- | --- | --- |
| Logística | `POST /rutas/{patente}/entregas/{idDonacion}/recepcion` | `{"fotos":["foto.jpg"]}`; puede enviarse `{}` sin fotos |
| Logística | `POST /rutas/{patente}/entregas/{idDonacion}/no-recibida` | `{"motivo":"Entidad cerrada"}` |
| Logística | `POST /rutas/{patente}/entregas/{idDonacion}/retorno` | `{"motivo":"Regreso al depósito"}` |
| Donaciones, interno | `POST /interno/logistica/eventos` | Evento con ID de operación, tipo, referencias de donación/entidad, patente, motivo y fecha |

Los eventos son `INICIO_TRASLADO`, `RECEPCION`, `NO_RECIBIDA` y `RETORNO_DEPOSITO`.
Donaciones responde 204 al aceptar, 400 si el evento es inválido y 409 si entra
en conflicto con el dominio. Logística devuelve 502 ante fallos de comunicación.
Para iniciar, las donaciones deben estar `LISTA_PARA_ENTREGAR`, como en el raíz.
El registro de una ruta no cambia automáticamente el estado de una donación.

Logística sigue en el puerto 8082; Donaciones en 8080.
`DONACIONES_URL` configura su conexión (por defecto `http://localhost:8080`).
`GENERADOR_RUTAS_URL` configura el cliente del generador (por defecto
`http://localhost:8081/rutas/generar`).

## Verificación

Desde la raíz del repositorio:

```powershell
mvn -f pom-servicios.xml clean test
```

Este comando prueba los dos módulos. `mvn test` con el `pom.xml` raíz todavía
prueba la aplicación anterior.

Se migran/adaptan pruebas de camiones, GPS, registro de rutas, trazabilidad y
contrato del generador. Se agregan pruebas HTTP, rechazo de transiciones,
validación de un lote completo, notificaciones y reintentos sin duplicación.
Las pruebas de proveedores reales de notificación existentes siguen omitidas.

## Alcance y límites

- El `src` raíz queda como referencia; este parche no lo elimina ni lo modifica.
- Los repositorios y los IDs de eventos procesados siguen en memoria. Un reintento
  conserva el evento pendiente mientras el proceso de Logística siga vivo;
  Donaciones reconoce eventos repetidos mientras su proceso siga vivo. La entrega 3
  deberá persistir esos datos y hacer transaccional la aplicación del lote.
- La validación previa del lote evita cambios parciales por errores de estado
  normales. No es una transacción distribuida ni garantiza recuperación tras caídas.
- Las notificaciones usan los proveedores existentes de Donaciones. Las pruebas
  verifican la invocación y el contenido; no acreditan envíos SMTP/Twilio reales.
  Donaciones conserva sus dependencias previas de Spring Mail y su configuración
  pendiente de proveedores. Logística no utiliza Spring.
- No se incorpora generación asíncrona de rutas, autenticación ni persistencia
  PostgreSQL en esta unificación. Se conserva el cliente síncrono existente.

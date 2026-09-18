# Pruebas anteriores que todavia no se pueden ejecutar en Logistica

Los archivos `*.java.original` son copias de pruebas del proyecto anterior. Se conservan aqui para que no se pierdan al separar los proyectos. No estan dentro de `src/test/java` porque prueban funcionalidades que el nuevo proyecto de Logistica aun no implementa y sus referencias a clases de Donaciones impiden compilarlas.

- `ClienteGeneradorRutasTest.java.original` prueba el cliente anterior del generador de rutas, que construye el pedido con `AsignacionDonacion` y usa Spring `RestTemplate`. El adaptador HTTP propio del nuevo Logistica aun no esta conectado.
- `NotificadorLogisticaTest.java.original` prueba avisos de inicio y resultado de entregas mediante el `Notificador` y la `Donacion` del proyecto anterior. Esa comunicacion entre los dos proyectos nuevos aun no esta implementada.

Las otras seis pruebas anteriores de Logistica se copiaron a `src/test/java` y se adaptaron a las clases y al contrato HTTP actuales. Las pruebas originales siguen en `src/test/java` de la raiz, para que el proyecto anterior conserve su conjunto de pruebas.

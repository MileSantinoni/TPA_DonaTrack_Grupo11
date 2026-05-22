# Decisiones de Diseño – DonaTrack

## Modelo de dominio

Decidimos construir un modelo de dominio que represente los conceptos principales de DonaTrack: personas donantes, donaciones, bienes, entidades beneficiarias, necesidades, notificaciones, categorías, subcategorías, entre otros.

---

## Separación entre `RegistroDonacion` y `Donacion`

Se decidió separar `RegistroDonacion` de `Donacion`.

- `RegistroDonacion` representa la carga completa inicial que realiza la persona administradora cuando un donante lleva bienes al depósito.
- `Donacion` representa cada unidad resultante luego de segmentar esos bienes.

Esta separación permite modelar correctamente tanto el ingreso original como las unidades individuales que luego serán gestionadas por el sistema.

---

## Método `segmentar()` en `RegistroDonacion`

Decidimos ubicar el método `segmentar()` dentro de `RegistroDonacion` porque es la clase que conoce la `listaBienes`.

Esto permite que la responsabilidad de segmentar quede en el objeto que tiene la información necesaria para hacerlo. De esta forma evitamos que otra clase tenga que consultar demasiados datos del registro o de los bienes.

---

## Uso de herencia para `Necesidad`

Decidimos modelar `Necesidad` como una clase abstracta y utilizar herencia/generalización para crear:

- `NecesidadExtraordinaria`
- `NecesidadRecurrente`

Esta decisión permite representar correctamente distintos comportamientos y características según el tipo de necesidad.

---

## Uso de herencia en `Bien`

Decidimos modelar `Bien` como una clase abstracta y crear las clases:

- `BienEstandar`
- `BienPerecedero`
- `BienConEstado`

todas heredando de `Bien`.

Esto evita que todos los bienes tengan atributos que no corresponden y que existan valores `NULL` innecesarios.

Por ejemplo:

- una silla no necesita fecha de vencimiento;
- un paquete de fideos no necesita estado nuevo/usado.

---

## División entre `Categoria` y `Subcategoria`

Decidimos modelar `Categoria` y `Subcategoria` como clases separadas, ya que el enunciado indica que los bienes pertenecen a categorías y que cada categoría posee múltiples subcategorías.

Tomamos esta decisión priorizando la extensibilidad del sistema, ya que si en un futuro aparecen nuevas subcategorías, como aceite o legumbres, podrán agregarse sin modificar la lógica principal.

---

## Uso de ENUMs

Decidimos utilizar `ENUMs` para evitar trabajar con textos libres como:

- `"nuevo"`
- `"usado"`
- `"entregada"`

Esto mejora la robustez del sistema, reduciendo errores e inconsistencias en los valores posibles.

---

## Trazabilidad mediante `RegistroCambioEstado`

Tomamos la decisión de que la donación no tenga solamente un estado actual, sino también un historial de estados.

Cada vez que cambia el estado de una donación se crea un `RegistroCambioEstado` con:

- estado anterior;
- estado nuevo;
- fecha y hora;
- justificativo.

También decidimos incluir un justificativo en cada cambio de estado para cumplir con requisitos de trazabilidad y auditoría.

---

## Separación entre `Notificacion` y `Notificador`

- `Notificacion` representa el registro del envío.
- `Notificador` representa el servicio encargado de ejecutarlo.

Esto permite reemplazar la simulación actual por una integración real en futuras iteraciones sin modificar la clase `Notificacion`.

Si ambas responsabilidades estuvieran unificadas en una sola clase, cualquier cambio en la forma de envío implicaría modificar también la representación de la notificación.

---

## Resolución del contacto predeterminado

`Notificador` resuelve automáticamente el destinatario y el medio según el `contactoPredeterminado` del donante, utilizando fallback al mail cuando el medio configurado no se encuentra registrado.

---

## Modelado del donante

Modelamos `Donante` con los atributos compartidos entre los diferentes tipos de donantes:

- personas humanas;
- personas jurídicas.

Ambas son clases que heredan de `Donante` y cuentan con atributos y métodos específicos en cada caso.

### Decisiones tomadas

- El mail fue modelado como atributo dentro de `Donante` ya que es obligatorio para ambos tipos.
- Se decidió separarlo del resto de medios de contacto debido a que los demás son opcionales y cualquiera puede ser elegido como predeterminado.
- Utilizamos un `Enum` para representar el estado del registro del donante y detectar si se trata de su primer acceso, permitiendo así el envío del mail de bienvenida.
- Las personas jurídicas poseen una colección de representantes, ya que las organizaciones operan mediante personas habilitadas en su nombre.

---

## Modelado de la importación CSV

La importación masiva de donantes se modeló mediante una clase `ImportadorCSV`.

Esta clase se encarga de:

1. leer el archivo CSV mediante su path;
2. separar los parámetros según cómo llegan;
3. convertir los datos a sus tipos correspondientes;
4. verificar si el donante ya existe.

### Comportamiento

- Si el donante ya existe, se actualizan sus datos.
- Si no existe, se determina si corresponde crear:
    - una persona humana; o
    - una persona jurídica.

Luego se instancia el tipo de donante correspondiente.
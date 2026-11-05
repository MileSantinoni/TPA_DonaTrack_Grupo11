(() => {
    const $ = (id) => document.getElementById(id);

    const registro = $("registro");
    const asignacion = $("asignacion");
    const carga = $("registrar-carga");
    const confirmar = $("confirmar-asignacion");

    let idAsignacion = null;
    let ocupada = false;
    let buscarSecuencia = 0;
    let propuestaSecuencia = 0;

    const guardado = sessionStorage.getItem("admin-donaciones-aviso");

    if (guardado) {
        $("aviso").textContent = guardado;
        sessionStorage.removeItem("admin-donaciones-aviso");
    }

    async function solicitar(url, method = "GET", body) {
        const respuesta = await fetch(url, {
            method,
            headers: body === undefined
                ? { Accept: "application/json" }
                : {
                    Accept: "application/json",
                    "Content-Type": "application/json"
                },
            body: body === undefined ? undefined : JSON.stringify(body)
        });

        const texto = await respuesta.text();
        let datos;

        try {
            datos = texto ? JSON.parse(texto) : null;
        } catch {
            datos = null;
        }

        if (!respuesta.ok) {
            throw new Error(
                datos?.mensaje || datos?.message
                || texto || `Error ${respuesta.status}`
            );
        }

        return datos;
    }

    function terminar(mensaje, nuevaCarga = false) {
        sessionStorage.setItem("admin-donaciones-aviso", mensaje);

        if (nuevaCarga) {
            location.assign("/administracion/donaciones");
        } else {
            location.reload();
        }
    }

    async function guardar(form, accion) {
        if (ocupada) return;

        ocupada = true;
        const error = form.querySelector(".ac-error");
        const boton = form.querySelector('[type="submit"]');

        error.textContent = "";
        boton.disabled = true;

        try {
            await accion();
        } catch (e) {
            error.textContent = e.message;
        } finally {
            boton.disabled = false;
            ocupada = false;
        }
    }

    document.querySelectorAll("dialog").forEach((dialog) => {
        dialog.addEventListener("cancel", (event) => {
            if (ocupada) event.preventDefault();
        });
    });

    document.addEventListener("click", (event) => {
        const cerrar = event.target.closest("[data-cerrar]");
        if (cerrar && !ocupada) cerrar.closest("dialog").close();
    });

    function mostrarPaso(paso) {
        ["paso-donante", "paso-alta-donante", "paso-bienes"]
            .forEach((id) => {
                $(id).hidden = id !== paso;
            });

        registro.scrollTop = 0;

        requestAnimationFrame(() => {
            $(paso).querySelector(
                "input:not(:disabled), select:not(:disabled), button"
            )?.focus();
        });
    }

    function pasarABienes() {
        const selector = $("donante-seleccionado");

        if (!selector.value) {
            $("resultado-busqueda").textContent =
                "Buscá y seleccioná un donante para continuar.";
            selector.focus();
            return;
        }

        $("resumen-donante").textContent =
            selector.selectedOptions[0].textContent;

        mostrarPaso("paso-bienes");
    }

    $("abrir-registro").addEventListener("click", () => {
        registro.showModal();
        mostrarPaso("paso-donante");
    });

    $("continuar-bienes").addEventListener("click", pasarABienes);

    $("volver-donante").addEventListener("click", () => {
        if (!ocupada) mostrarPaso("paso-donante");
    });

    $("abrir-alta-donante").addEventListener("click", () => {
        // Invalida cualquier búsqueda anterior que siga respondiendo.
        buscarSecuencia++;

        $("alta-donante").querySelector(".ac-error").textContent = "";
        mostrarPaso("paso-alta-donante");
    });

    $("volver-busqueda").addEventListener("click", () => {
        if (!ocupada) mostrarPaso("paso-donante");
    });

    $("tipo-alta-donante").addEventListener("change", (event) => {
        const juridica = event.target.value === "juridica";

        $("alta-humana").hidden = juridica;
        $("alta-humana").disabled = juridica;

        $("alta-juridica").hidden = !juridica;
        $("alta-juridica").disabled = !juridica;

        $("etiqueta-documento").textContent = juridica ? "CUIT" : "DNI";
    });

    $("alta-donante").addEventListener("submit", (event) => {
        event.preventDefault();

        const formulario = event.currentTarget;

        guardar(formulario, async () => {
            const datos = new FormData(formulario);
            const juridica = datos.get("clase") === "juridica";

            const texto = (campo) => String(datos.get(campo) || "").trim();

            const cuerpo = {
                mail: texto("mail"),
                numeroDocumento: texto("numeroDocumento"),
                tipoDeDocumento: juridica ? "CUIT" : "DNI"
            };

            if (!cuerpo.mail || !cuerpo.numeroDocumento) {
                throw new Error("Completá el email y el documento.");
            }

            if (juridica) {
                Object.assign(cuerpo, {
                    razonSocial: texto("razonSocial"),
                    tipo: texto("tipo"),
                    rubro: texto("rubro")
                });

                if (!cuerpo.razonSocial || !cuerpo.rubro) {
                    throw new Error("Completá la razón social y el rubro.");
                }
            } else {
                const edad = Number(datos.get("edad"));

                if (!Number.isInteger(edad) || edad < 0 || edad > 150) {
                    throw new Error("Ingresá una edad entera entre 0 y 150.");
                }

                Object.assign(cuerpo, {
                    nombre: texto("nombre"),
                    apellido: texto("apellido"),
                    edad,
                    genero: texto("genero"),
                    direccion: texto("direccion")
                });

                if (!cuerpo.nombre || !cuerpo.apellido || !cuerpo.direccion) {
                    throw new Error("Completá nombre, apellido y dirección.");
                }
            }

            const nuevo = await solicitar(
                juridica ? "/donantes/juridicas" : "/donantes/humanas",
                "POST",
                cuerpo
            );

            if (!nuevo?.id) {
                throw new Error(
                    "El alta respondió sin ID. Buscá el donante por email "
                    + "antes de intentar registrarlo nuevamente."
                );
            }

            buscarSecuencia++;

            const opcion = new Option(
                `${nuevo.nombre} · ${nuevo.documento} · ${nuevo.mail}`,
                String(nuevo.id),
                true,
                true
            );

            $("donante-seleccionado").replaceChildren(opcion);
            $("resultado-busqueda").textContent = "Donante registrado.";
            formulario.reset();

            $("alta-humana").hidden = false;
            $("alta-humana").disabled = false;
            $("alta-juridica").hidden = true;
            $("alta-juridica").disabled = true;
            $("etiqueta-documento").textContent = "DNI";

            pasarABienes();
        });
    });

    function agregarBien() {
        const fragmento = $("plantilla-bien").content.cloneNode(true);
        const fila = fragmento.querySelector("fieldset");
        const select = fila.querySelector('[name="idSubcategoria"]');

        function actualizarRequisitos() {
            const tipo = select.selectedOptions[0]?.dataset.tipo;
            fila.querySelector('[name="fechaVencimiento"]').required =
                tipo === "PERECEDERO";

            fila.querySelector('[name="estadoBien"]').required =
                tipo === "CON_ESTADO";

            fila.querySelector(".ac-requisito").textContent =
                tipo === "PERECEDERO"
                    ? "Esta subcategoría requiere fecha de vencimiento."
                    : tipo === "CON_ESTADO"
                        ? "Esta subcategoría requiere indicar Nuevo o Usado."
                        : "";
        }

        select.addEventListener("change", actualizarRequisitos);

        fila.querySelector("[data-quitar]").addEventListener("click", () => {
            if ($("bienes").children.length > 1 && !ocupada) fila.remove();
        });

        $("bienes").appendChild(fragmento);
    }

    $("agregar-bien").addEventListener("click", () => {
        if (!ocupada) agregarBien();
    });

    agregarBien();

    $("buscar-donante").addEventListener("submit", async (event) => {
        event.preventDefault();

        const secuencia = ++buscarSecuencia;
        const q = new FormData(event.currentTarget).get("q").trim();

        $("resultado-busqueda").textContent = "Buscando…";
        $("donante-seleccionado").replaceChildren(
            new Option("Seleccionar donante", "")
        );

        try {
            const personas = await solicitar(
                "/administracion/donaciones/buscar-donantes?q="
                + encodeURIComponent(q)
            );

            if (secuencia !== buscarSecuencia) return;

            personas.forEach((persona) => {
                $("donante-seleccionado").appendChild(new Option(
                    `${persona.nombre} · ${persona.documento} · ${persona.mail}`,
                    persona.id
                ));
            });

            $("resultado-busqueda").textContent = personas.length
                ? "Seleccioná el donante. Se muestran hasta 20 coincidencias."
                : "No encontramos donantes. Revisá el email o documento.";

        } catch (error) {
            if (secuencia === buscarSecuencia) {
                $("resultado-busqueda").textContent = error.message;
            }
        }
    });

    carga.addEventListener("submit", (event) => {
        event.preventDefault();

        guardar(carga, async () => {
            const datos = new FormData(carga);

            const bienes = [...$("bienes").querySelectorAll("fieldset")]
                .map((fila) => {
                    const valor = (nombre) =>
                        fila.querySelector(`[name="${nombre}"]`).value.trim();

                    const cantidad = Number(valor("cantidad"));

                    if (!Number.isInteger(cantidad)
                        || cantidad <= 0
                        || cantidad > 2147483647) {
                        throw new Error("La cantidad debe ser un entero positivo.");
                    }

                    return {
                        descripcion: valor("descripcion"),
                        cantidad,
                        unidadMedida: valor("unidadMedida"),
                        idSubcategoria: valor("idSubcategoria"),
                        fechaVencimiento: valor("fechaVencimiento") || null,
                        estadoBien: valor("estadoBien") || null,
                        foto: null
                    };
                });

            const resultado = await solicitar("/registros-donacion", "POST", {
                idDonante: datos.get("idDonante"),
                descripcionGeneral: datos.get("descripcionGeneral").trim(),
                bienes
            });

            terminar(
                `Carga registrada: ${resultado.cantidadBienes} bienes, `
                + `${resultado.cantidadDonaciones} donaciones generadas.`,
                true
            );
        });
    });

    document.querySelectorAll("[data-asignar]").forEach((boton) => {
        boton.addEventListener("click", async () => {
            const secuencia = ++propuestaSecuencia;

            idAsignacion = boton.dataset.asignar;
            $("propuestas").textContent = "Consultando propuestas…";
            confirmar.querySelector(".ac-error").textContent = "";

            const submit = confirmar.querySelector('[type="submit"]');
            submit.disabled = true;
            asignacion.showModal();

            try {
                const propuestas = await solicitar(
                    `/administracion/donaciones/${idAsignacion}/propuestas`
                );

                if (secuencia !== propuestaSecuencia) return;

                $("propuestas").replaceChildren();

                propuestas.forEach((entidad, indice) => {
                    const label = document.createElement("label");
                    label.className = "choice";

                    const radio = document.createElement("input");
                    radio.type = "radio";
                    radio.name = "idEntidad";
                    radio.value = entidad.id;
                    radio.required = true;

                    const texto = document.createElement("span");
                    texto.textContent =
                        `${indice + 1}. ${entidad.nombre} · ${entidad.direccion}`;

                    label.append(radio, texto);
                    $("propuestas").appendChild(label);
                });

                if (!propuestas.length) {
                    $("propuestas").textContent =
                        "No hay entidades propuestas para esta donación.";
                }

                submit.disabled = propuestas.length === 0;
            } catch (error) {
                $("propuestas").textContent = "";
                confirmar.querySelector(".ac-error").textContent = error.message;
            }
        });
    });

    confirmar.addEventListener("submit", (event) => {
        event.preventDefault();

        guardar(confirmar, async () => {
            await solicitar("/asignaciones/confirmar", "POST", {
                idDonacion: idAsignacion,
                idEntidad: new FormData(confirmar).get("idEntidad")
            });

            terminar("Asignación registrada. La planificación de ruta queda pendiente.");
        });
    });

    document.querySelectorAll("[data-vencer]").forEach((boton) => {
        boton.addEventListener("click", async () => {
            if (ocupada || !window.confirm("¿Marcar esta donación como vencida?")) {
                return;
            }

            ocupada = true;
            boton.disabled = true;

            try {
                await solicitar(
                    `/donaciones/${boton.dataset.vencer}/estado`,
                    "PATCH",
                    {
                        nuevoEstado: "VENCIDA",
                        justificativo: "Fecha de vencimiento superada"
                    }
                );

                terminar("Donación marcada como vencida.");
            } catch (error) {
                $("aviso").textContent = error.message;
            } finally {
                ocupada = false;
                boton.disabled = false;
            }
        });
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Tab") document.body.classList.add("teclado");
    });

    document.addEventListener("pointerdown", () => {
        document.body.classList.remove("teclado");
    });
})();
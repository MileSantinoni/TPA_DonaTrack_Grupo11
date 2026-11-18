(() => {
    "use strict";

    const $ = id => document.getElementById(id);
    const modal = $("plan-modal");
    const form = $("plan-form");
    const inicio = $("inicio-ruta-modal");

    if (!modal || !form || !inicio) return;

    let ocupado = false;
    let refrescar = false;
    let foco;
    let ruta;
    let filas = [];

    // Limpiar el comportamiento anterior de apertura automática.
    try {
        sessionStorage.removeItem("planificar-donacion");
    } catch {}

    async function pedir(url, datos) {
        const respuesta = await fetch(url, {
            method: datos === undefined ? "GET" : "POST",
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json"
            },
            body: datos === undefined
                ? undefined
                : JSON.stringify(datos)
        });

        const contenidoTexto = await respuesta.text();
        let contenido;

        try {
            contenido = contenidoTexto
                ? JSON.parse(contenidoTexto)
                : {};
        } catch {
            throw new Error(
                "El servidor no devolvió una respuesta válida. "
                + "Recargá la página y verificá tu sesión."
            );
        }

        if (!respuesta.ok || respuesta.redirected) {
            throw new Error(
                contenido.mensaje
                || contenido.title
                || contenido.message
                || "No se pudo completar la operación."
            );
        }

        return contenido;
    }

    function texto(padre, mensaje) {
        const p = document.createElement("p");
        p.textContent = mensaje;
        padre.append(p);
    }

    function actualizarSeleccion() {
        const cantidad = filas.filter(f => f.check.checked).length;

        $("plan-submit").disabled =
            ocupado || cantidad === 0 || cantidad > 48;

        $("plan-submit").textContent =
            `Planificar seleccionadas (${cantidad})`;
    }

    function crearFila(d) {
        const tarjeta = document.createElement("article");
        tarjeta.className = "plan-fila";

        const etiqueta = document.createElement("label");
        etiqueta.className = "plan-eleccion";

        const check = document.createElement("input");
        check.type = "checkbox";
        check.disabled = !d.ubicada;
        check.checked = d.ubicada
            && filas.filter(f => f.check.checked).length < 48;

        const titulo = document.createElement("span");
        titulo.textContent =
            `${d.descripcion} · ${d.cantidad} ${d.unidad}`;

        etiqueta.append(check, titulo);
        tarjeta.append(etiqueta);

        texto(tarjeta, `Entidad: ${d.entidad}`);
        texto(
            tarjeta,
            `Dirección: ${d.direccion || "Sin dirección"}`
        );

        const medidas = document.createElement("div");
        medidas.className = "ac-grid";
        const campos = {};

        for (const [clave, nombre] of [
            ["pesoKg", "Peso total (kg)"],
            ["volumenM3", "Volumen total (m³)"]
        ]) {
            const label = document.createElement("label");
            label.textContent = nombre;

            const input = document.createElement("input");
            input.type = "number";
            input.min = "0.001";
            input.step = "any";
            input.required = true;
            input.disabled = !check.checked;

            if (clave === "pesoKg"
                && /^(kg|kilogramos?)$/i.test(d.unidad)) {
                input.value = d.cantidad;
            }

            campos[clave] = input;
            label.append(input);
            medidas.append(label);
        }

        tarjeta.append(medidas);

        if (!d.ubicada) {
            texto(
                tarjeta,
                "Pendiente: la entidad debe configurar su ubicación."
            );
        }

        check.addEventListener("change", () => {
            Object.values(campos).forEach(input => {
                input.disabled = !check.checked;
            });

            const cantidad = filas.filter(f => f.check.checked).length;

            $("plan-error").textContent = cantidad > 48
                ? "Podés seleccionar hasta 48 donaciones por ejecución."
                : "";

            actualizarSeleccion();
        });

        filas.push({ donacion: d, check, campos });
        return tarjeta;
    }

    async function abrir() {
        if (ocupado || modal.open || inicio.open) return;

        foco = document.activeElement;
        filas = [];
        refrescar = false;
        ocupado = true;

        form.hidden = false;
        $("plan-resultado").hidden = true;
        $("plan-error").textContent = "";
        $("plan-deposito").textContent = "";
        $("plan-lista").textContent = "Consultando donaciones pendientes…";
        $("plan-submit").disabled = true;

        modal.showModal();

        try {
            const datos = await pedir(
                "/administracion/planificaciones/pendientes"
            );

            if (!Array.isArray(datos.donaciones)) {
                throw new Error(
                    "La respuesta no contiene el listado de pendientes."
                );
            }

            $("plan-deposito").textContent =
                `Salida: ${datos.deposito} · Entregas: ${datos.fecha}`;

            $("plan-lista").replaceChildren();

            datos.donaciones.forEach(d => {
                $("plan-lista").append(crearFila(d));
            });

            if (!filas.length) {
                texto(
                    $("plan-lista"),
                    "No hay donaciones asignadas pendientes de planificación."
                );
            }
        } catch (error) {
            $("plan-lista").replaceChildren();
            $("plan-error").textContent = error.message;
        } finally {
            ocupado = false;
            actualizarSeleccion();
        }
    }

    document.querySelectorAll("[data-planificar-lote]")
        .forEach(boton => boton.addEventListener("click", abrir));

    modal.querySelectorAll("[data-plan-cerrar]")
        .forEach(boton => {
            boton.addEventListener("click", () => {
                if (!ocupado) modal.close();
            });
        });

    modal.addEventListener("cancel", event => {
        if (ocupado) event.preventDefault();
    });

    modal.addEventListener("close", () => {
        if (refrescar) {
            location.reload();
        } else {
            foco?.focus();
        }
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (ocupado) return;

        const seleccionadas = filas.filter(f => f.check.checked);

        if (!seleccionadas.length || seleccionadas.length > 48) {
            $("plan-error").textContent =
                "Seleccioná entre 1 y 48 donaciones.";
            return;
        }

        const cargas = {};

        for (const fila of seleccionadas) {
            const carga = {
                pesoKg: Number(fila.campos.pesoKg.value),
                volumenM3: Number(fila.campos.volumenM3.value)
            };

            if (!Object.values(carga).every(
                n => Number.isFinite(n) && n > 0
            )) {
                $("plan-error").textContent =
                    "Completá peso y volumen positivos en cada selección.";
                return;
            }

            cargas[fila.donacion.id] = carga;
        }

        ocupado = true;
        refrescar = true;
        $("plan-error").textContent = "";

        const controles = [...form.querySelectorAll("button,input")]
            .map(control => [control, control.disabled]);

        controles.forEach(([control]) => control.disabled = true);
        $("plan-submit").textContent = "Planificando…";

        try {
            const resultado = await pedir(
                "/administracion/planificaciones",
                { cargas }
            );

            const resumen = $("plan-resumen");
            resumen.replaceChildren();

            const idsSeleccionados = new Set(Object.keys(cargas));
            const planificadas = new Set();

            const rutas = (resultado.lotes || [])
                .flatMap(lote => lote.rutas || [])
                .filter(r => (r.entregas || []).some(
                    id => idsSeleccionados.has(id)
                ));

            texto(resumen, `Planificación para ${resultado.fecha}.`);

            rutas.forEach(r => {
                const ids = (r.entregas || []).filter(
                    id => idsSeleccionados.has(id)
                );

                ids.forEach(id => planificadas.add(id));

                texto(
                    resumen,
                    `Camión ${r.patente}: ${ids.length} entrega(s).`
                );

                ids.forEach(id => {
                    const fila = seleccionadas.find(
                        f => f.donacion.id === id
                    );

                    texto(
                        resumen,
                        `${fila.donacion.descripcion} → ${fila.donacion.entidad}`
                    );
                });
            });

            texto(
                resumen,
                `${planificadas.size} de ${seleccionadas.length} `
                + "donaciones obtuvieron una ruta en esta respuesta."
            );

            const sinAsignar = new Set(
                (resultado.lotes || []).flatMap(l => l.sinAsignar || [])
            );

            seleccionadas
                .filter(f => !planificadas.has(f.donacion.id))
                .forEach(f => {
                    const id = f.donacion.id;
                    let motivo =
                        "No se confirmó una ruta. Revisá su estado antes de reintentar.";

                    if ((resultado.sinUbicacion || []).includes(id)) {
                        motivo = "Falta confirmar la ubicación.";
                    } else if ((resultado.sinDatosDeCarga || []).includes(id)) {
                        motivo = "Faltan datos de carga.";
                    } else if ((resultado.sinCamionDisponible || []).includes(id)) {
                        motivo = "No hay camiones libres para otra ruta.";
                    } else if (sinAsignar.has(id)) {
                        motivo = "El optimizador no pudo asignarla a la flota.";
                    }

                    texto(resumen, `${f.donacion.descripcion}: ${motivo}`);
                });

            const enlace = $("plan-ver-camion");
            enlace.href = "/administracion/camiones";
            enlace.textContent = "Ver camiones y rutas";

            form.hidden = true;
            $("plan-resultado").hidden = false;
        } catch (error) {
            $("plan-error").textContent =
                error.message
                + " Antes de repetir, cerrá y revisá las rutas: "
                + "la operación puede haberse procesado.";
        } finally {
            ocupado = false;

            controles.forEach(([control, disabled]) => {
                control.disabled = disabled;
            });

            actualizarSeleccion();
        }
    });

    // Inicio de traslado: se conserva separado de la planificación.
    document.querySelectorAll("[data-iniciar-ruta]")
        .forEach(boton => {
            boton.addEventListener("click", () => {
                if (ocupado || modal.open || inicio.open) return;

                foco = boton;
                ruta = boton.dataset.iniciarRuta;

                const patente = boton.closest("[data-patente]")
                    ?.dataset.patente || "";

                $("inicio-ruta-texto").textContent =
                    `Camión ${patente} · Fecha planificada: ${boton.dataset.fecha}`;

                $("inicio-ruta-error").textContent = "";
                inicio.showModal();
            });
        });

    inicio.querySelectorAll("[data-inicio-cerrar]")
        .forEach(boton => {
            boton.addEventListener("click", () => {
                if (!ocupado) inicio.close();
            });
        });

    inicio.addEventListener("cancel", event => {
        if (ocupado) event.preventDefault();
    });

    inicio.addEventListener("close", () => foco?.focus());

    $("inicio-ruta-confirmar").addEventListener("click", async () => {
        if (ocupado || !ruta) return;

        ocupado = true;
        const botones = [...inicio.querySelectorAll("button")];
        botones.forEach(b => b.disabled = true);
        $("inicio-ruta-error").textContent = "";

        try {
            const resultado = await pedir(
                "/administracion/rutas/"
                + encodeURIComponent(ruta)
                + "/iniciar",
                {}
            );

            try {
                sessionStorage.setItem(
                    "camiones-aviso",
                    resultado.mensaje || "Traslado iniciado."
                );
            } catch {}

            location.reload();
        } catch (error) {
            $("inicio-ruta-error").textContent = error.message;
        } finally {
            ocupado = false;
            botones.forEach(b => b.disabled = false);
        }
    });
})();
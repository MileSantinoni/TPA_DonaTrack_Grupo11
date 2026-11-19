(() => {
    const root = document.querySelector(".dt-avisos");
    if (!root || root.dataset.conectado === "true") return;

    root.dataset.conectado = "true";

    const campana = root.querySelector(".dt-notificaciones");
    const panel = root.querySelector("#dt-bandeja");
    const contador = root.querySelector("[data-cuenta]");
    const todas = panel.querySelector("[data-todas]");
    const tabs = [...panel.querySelectorAll("[role=tab]")];
    const zona = panel.querySelector("[role=tabpanel]");
    const lista = panel.querySelector("ul");
    const vacio = panel.querySelector("[data-vacio]");
    const error = panel.querySelector("[data-error]");
    const resumen = panel.querySelector("[data-resumen]");
    const paginacion = panel.querySelector(".av-paginacion");
    const anterior = panel.querySelector("[data-anterior]");
    const siguiente = panel.querySelector("[data-siguiente]");
    const numeroPagina = panel.querySelector("[data-pagina]");

    let actual = 0;
    let pagina = 0;
    let consulta = null;
    let guardando = false;
    let sesionVencida = false;
    let datos = null;

    async function comprobarRespuesta(response) {
        if (response.status === 401) {
            sesionVencida = true;
            throw new Error("Tu sesión venció. Volvé a iniciar sesión.");
        }

        if (response.status === 403) {
            throw new Error("No se pudo realizar la acción. Recargá la página.");
        }

        if (!response.ok) {
            throw new Error("No pudimos actualizar las notificaciones.");
        }

        return response;
    }

    function mostrarError(mensaje) {
        error.textContent = mensaje;
        error.hidden = false;
    }

    function actualizarTabs() {
        tabs.forEach((tab, indice) => {
            tab.setAttribute("aria-selected", String(indice === actual));
            tab.tabIndex = indice === actual ? 0 : -1;
        });

        zona.setAttribute("aria-labelledby", tabs[actual].id);
    }

    async function cargar() {
        if (sesionVencida || guardando) return;

        consulta?.abort();
        const peticion = new AbortController();
        consulta = peticion;

        zona.setAttribute("aria-busy", "true");

        try {
            const params = new URLSearchParams({
                leidas: String(actual === 1),
                pagina: String(pagina)
            });

            const response = await fetch(
                `/cuenta/notificaciones?${params}`,
                {
                    credentials: "same-origin",
                    cache: "no-store",
                    signal: peticion.signal
                }
            );

            await comprobarRespuesta(response);
            const resultado = await response.json();

            if (consulta !== peticion) return;

            // Si la última fila desapareció, volver a la página anterior.
            if (resultado.items.length === 0 && pagina > 0) {
                pagina--;
                void cargar();
                return;
            }

            datos = resultado;
            error.hidden = true;
            pintar();

        } catch (e) {
            if (e.name !== "AbortError" && consulta === peticion) {
                mostrarError(e.message);

                if (!datos) {
                    vacio.textContent = "No se pudo cargar la bandeja.";
                }
            }
        } finally {
            if (consulta === peticion) {
                zona.setAttribute("aria-busy", "false");
            }
        }
    }

    function pintar() {
        contador.textContent = datos.sinLeer > 99
            ? "99+"
            : String(datos.sinLeer);

        contador.hidden = datos.sinLeer === 0;

        campana.setAttribute(
            "aria-label",
            `Notificaciones: ${datos.sinLeer} sin leer`
        );

        tabs[0].textContent = `Sin leer (${datos.sinLeer})`;
        tabs[1].textContent = `Leídas (${datos.leidas})`;

        todas.hidden = actual === 1 || datos.sinLeer === 0;

        resumen.textContent = datos.sinLeer
            ? `${datos.sinLeer} sin leer`
            : "Estás al día.";

        lista.replaceChildren();

        vacio.hidden = datos.items.length > 0;
        vacio.textContent = actual === 1
            ? "No hay notificaciones leídas."
            : "No tenés notificaciones sin leer.";

        for (const aviso of datos.items) {
            const li = document.createElement("li");
            const titulo = document.createElement("b");
            const mensaje = document.createElement("span");
            const fecha = document.createElement("small");
            const accion = document.createElement("button");

            titulo.textContent = aviso.titulo;
            mensaje.textContent = aviso.mensaje;
            fecha.textContent = aviso.fecha;

            accion.type = "button";
            accion.dataset.id = aviso.id;

            if (aviso.leida) {
                accion.dataset.accion = "ocultar";
                accion.textContent = "Quitar de la bandeja";
            } else {
                li.className = "av-nueva";
                accion.dataset.accion = "leer";
                accion.textContent = "Marcar como leída";
            }

            li.append(titulo, mensaje, fecha, accion);
            lista.append(li);
        }

        paginacion.hidden = pagina === 0 && !datos.hayMas;
        anterior.disabled = pagina === 0;
        siguiente.disabled = !datos.hayMas;
        numeroPagina.textContent = `Página ${pagina + 1}`;
    }

    function cambiarTab(indice) {
        if (guardando) return;

        actual = indice;
        pagina = 0;
        actualizarTabs();

        lista.replaceChildren();
        vacio.hidden = false;
        vacio.textContent = "Cargando notificaciones…";
        paginacion.hidden = true;
        todas.hidden = true;

        void cargar();
    }

    async function modificar(url) {
        if (guardando || sesionVencida) return;

        guardando = true;
        consulta?.abort();
        consulta = null;

        panel.querySelectorAll("button").forEach(b => {
            b.disabled = true;
        });

        try {
            const response = await fetch(url, {
                method: "POST",
                credentials: "same-origin",
                body: new URLSearchParams({
                    csrf: panel.dataset.csrf
                })
            });

            await comprobarRespuesta(response);
            error.hidden = true;

        } catch (e) {
            mostrarError(e.message);
            return;

        } finally {
            guardando = false;

            panel.querySelectorAll("button").forEach(b => {
                b.disabled = false;
            });

            if (datos) {
                anterior.disabled = pagina === 0;
                siguiente.disabled = !datos.hayMas;
            }
        }

        await cargar();

        if (!panel.hidden) {
            tabs[actual].focus();
        }
    }

    function abrir(visible) {
        panel.hidden = !visible;
        campana.setAttribute("aria-expanded", String(visible));

        if (visible) {
            void cargar();
        }
    }

    campana.addEventListener("click", () => abrir(panel.hidden));

    tabs.forEach((tab, indice) => {
        tab.addEventListener("click", () => cambiarTab(indice));

        tab.addEventListener("keydown", event => {
            if (guardando) return;

            const destino = {
                ArrowRight: 1 - indice,
                ArrowLeft: 1 - indice,
                Home: 0,
                End: 1
            }[event.key];

            if (destino !== undefined) {
                event.preventDefault();
                cambiarTab(destino);
                tabs[destino].focus();
            }
        });
    });

    todas.addEventListener("click", () => {
        void modificar("/cuenta/notificaciones/leer-todas");
    });

    lista.addEventListener("click", event => {
        const boton = event.target.closest("[data-accion]");
        if (!boton) return;

        const id = encodeURIComponent(boton.dataset.id);
        const accion = boton.dataset.accion;

        void modificar(`/cuenta/notificaciones/${id}/${accion}`);
    });

    anterior.addEventListener("click", () => {
        if (pagina > 0 && !guardando) {
            pagina--;
            void cargar();
        }
    });

    siguiente.addEventListener("click", () => {
        if (datos?.hayMas && !guardando) {
            pagina++;
            void cargar();
        }
    });

    document.addEventListener("pointerdown", event => {
        if (!root.contains(event.target)) abrir(false);
    });

    document.addEventListener("focusin", event => {
        if (!root.contains(event.target)) abrir(false);
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.hidden) {
            abrir(false);
            campana.focus();
        }
    });

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) void cargar();
    });

    window.addEventListener("pageshow", () => void cargar());

    setInterval(() => {
        if (!document.hidden) void cargar();
    }, 30000);

    actualizarTabs();
    void cargar();
})();
(() => {
    "use strict";

    const $ = id => document.getElementById(id);
    const aviso = $("mon-aviso");
    const lista = $("mon-lista");
    const key = document.querySelector(
        'meta[name="geoapify-key"]'
    )?.content.trim();

    if (!window.L || !key) {
        aviso.textContent =
            "No se pudo iniciar el mapa. Revisá Leaflet y GEOAPIFY_API_KEY.";
        return;
    }

    const mapa = L.map("mon-mapa", {
        scrollWheelZoom: false
    }).setView([-34.6037, -58.3816], 12);

    L.tileLayer(
        "https://maps.geoapify.com/v1/tile/osm-carto/{z}/{x}/{y}.png"
        + "?apiKey=" + encodeURIComponent(key),
        {
            maxZoom: 19,
            attribution:
                '&copy; <a href="https://www.geoapify.com/">Geoapify</a>'
                + ' | &copy; <a href="https://www.openstreetmap.org/copyright">'
                + "OpenStreetMap</a> contributors"
        }
    ).addTo(mapa);

    new ResizeObserver(() => {
        mapa.invalidateSize({ animate: false });
    }).observe($("mon-mapa"));

    const capas = L.featureGroup().addTo(mapa);
    const cards = new Map();

    let camiones = [];
    let seleccion = null;
    let temporizador;
    let consultando = false;
    let necesitaEncuadre = true;
    let paginaActiva = true;

    function posicion(p) {
        return p && Number.isFinite(p.lat) && Number.isFinite(p.lng)
        && Math.abs(p.lat) <= 90 && Math.abs(p.lng) <= 180
            ? [p.lat, p.lng] : null;
    }

    function elemento(tag, texto, clase) {
        const nodo = document.createElement(tag);
        nodo.textContent = texto;
        if (clase) nodo.className = clase;
        return nodo;
    }

    function fecha(valor) {
        const dato = new Date(valor);
        return Number.isNaN(dato.getTime())
            ? "Sin fecha"
            : dato.toLocaleString("es-AR");
    }

    function decodificar(cadena) {
        const puntos = [];
        let i = 0, lat = 0, lng = 0;

        function diferencia() {
            let resultado = 0, desplazamiento = 0;

            while (true) {
                if (i >= cadena.length || desplazamiento > 30) {
                    throw new Error("Recorrido inválido.");
                }

                const valor = cadena.charCodeAt(i++) - 63;
                if (valor < 0 || valor > 63) {
                    throw new Error("Recorrido inválido.");
                }

                resultado += (valor & 31) * (2 ** desplazamiento);
                if (valor < 32) break;
                desplazamiento += 5;
            }

            return resultado % 2
                ? -(Math.floor(resultado / 2) + 1)
                : resultado / 2;
        }

        while (i < cadena.length) {
            lat += diferencia();
            lng += diferencia();

            const p = posicion({ lat: lat / 1e5, lng: lng / 1e5 });
            if (!p) throw new Error("Coordenadas inválidas.");
            puntos.push(p);
        }

        if (puntos.length < 2) throw new Error("Recorrido incompleto.");
        return puntos;
    }

    function elegir(patente) {
        seleccion = patente;
        necesitaEncuadre = true;
        renderizar();
    }

    function actualizarCards() {
        const existentes = new Set(camiones.map(c => c.patente));

        for (const [patente, card] of cards) {
            if (!existentes.has(patente)) {
                card.boton.remove();
                cards.delete(patente);
            }
        }

        for (const camion of camiones) {
            let card = cards.get(camion.patente);

            if (!card) {
                const boton = elemento("button", "", "mon-camion");
                boton.type = "button";

                const nombre = elemento("strong", camion.patente);
                const estado = elemento("span", "", "mon-estado");
                const avance = elemento("small", "");
                const barra = document.createElement("progress");
                const reporte = elemento("small", "");

                boton.append(nombre, estado, avance, barra, reporte);
                boton.addEventListener("click", () => elegir(camion.patente));

                card = { boton, estado, avance, barra, reporte };
                cards.set(camion.patente, card);
                lista.append(boton);
            }

            card.boton.setAttribute(
                "aria-pressed", String(seleccion === camion.patente)
            );

            card.estado.textContent = camion.estado;
            card.avance.textContent = camion.idRuta
                ? `${camion.completadas} de ${camion.total} entregadas`
                + (camion.noRecibidas
                    ? ` · ${camion.noRecibidas} no recibidas` : "")
                : camion.rutasPendientes
                    ? `${camion.rutasPendientes} rutas pendientes`
                    : "Sin ruta asignada";

            card.barra.max = Math.max(1, camion.total);
            card.barra.value = camion.completadas;
            card.barra.hidden = !camion.total;
            card.barra.setAttribute(
                "aria-label", `Entregas completadas de ${camion.patente}`
            );

            card.reporte.textContent = camion.ubicacion
                ? `${camion.reciente
                    ? "Reporte reciente" : "Sin reportes recientes"} · `
                + fecha(camion.ubicacion.fecha)
                : "Sin ubicación reportada";
        }

        $("mon-total").textContent = `(${camiones.length})`;
    }

    function punto(coordenadas, color, texto, patente) {
        if (!coordenadas) return;

        const marcador = L.circleMarker(coordenadas, {
            radius: 9,
            color: "#fff",
            weight: 3,
            fillColor: color,
            fillOpacity: 1
        }).addTo(capas);

        marcador.bindTooltip(elemento("span", texto));

        if (patente) {
            marcador.on("click", () => elegir(patente));
        }
    }

    function renderizar() {
        if (seleccion && !camiones.some(c => c.patente === seleccion)) {
            seleccion = null;
            necesitaEncuadre = true;
        }

        actualizarCards();
        capas.clearLayers();

        const elegido = camiones.find(c => c.patente === seleccion);
        const visibles = elegido ? [elegido] : camiones;

        $("mon-detalle").hidden = !elegido;
        $("mon-mapa-aviso").textContent = "";

        if (elegido) {
            $("mon-titulo").textContent = `Camión ${elegido.patente}`;
            $("mon-ruta").textContent = elegido.idRuta
                ? `Ruta: ${elegido.idRuta} · Fecha: ${elegido.fecha}`
                : "Este camión no tiene un recorrido único para mostrar.";

            $("mon-paradas").replaceChildren(
                ...elegido.entregas.map(e => elemento(
                    "li",
                    `${e.entidad} · ${e.direccion} · `
                    + e.estado.replaceAll("_", " ")
                ))
            );

            if (elegido.geometria) {
                try {
                    const recorrido = decodificar(elegido.geometria);

                    L.polyline(recorrido, {
                        color: "#ad4d32",
                        weight: 5,
                        opacity: 0.8
                    }).addTo(capas);

                } catch (error) {
                    $("mon-mapa-aviso").textContent =
                        "No se pudo dibujar el recorrido guardado.";
                }
            }

            punto(posicion(elegido.deposito), "#d58b40", "Depósito");

            if (!elegido.ubicacion) {
                $("mon-mapa-aviso").textContent +=
                    " Este camión todavía no tiene ubicación reportada.";
            }
        }

        for (const camion of visibles) {
            punto(
                posicion(camion.ubicacion),
                camion.reciente ? "#4285cf" : "#8d9299",
                camion.patente + (camion.reciente
                    ? " · Reporte reciente" : " · Última ubicación conocida"),
                camion.patente
            );
        }

        const limites = capas.getBounds();

        if (necesitaEncuadre && limites.isValid()) {
            mapa.fitBounds(limites, {
                padding: [35, 35],
                maxZoom: 15,
                animate: false
            });
            necesitaEncuadre = false;
        }

        if (!elegido && !limites.isValid()) {
            $("mon-mapa-aviso").textContent =
                "No hay ubicaciones reportadas para mostrar.";
        }
    }

    function programar() {
        clearTimeout(temporizador);

        if (paginaActiva && !document.hidden) {
            temporizador = setTimeout(actualizar, 5000);
        }
    }

    async function actualizar() {
        clearTimeout(temporizador);
        if (consultando || document.hidden || !paginaActiva) return;

        consultando = true;
        const abortador = new AbortController();
        const timeout = setTimeout(() => abortador.abort(), 12000);

        try {
            const respuesta = await fetch("/administracion/monitoreo/datos", {
                headers: { Accept: "application/json" },
                cache: "no-store",
                signal: abortador.signal
            });

            if (!respuesta.ok) {
                throw new Error("No se pudo consultar la flota.");
            }

            if (!(respuesta.headers.get("content-type") || "")
                .includes("application/json")) {
                throw new Error("Verificá tu sesión de administrador.");
            }

            const datos = await respuesta.json();

            if (!Array.isArray(datos.camiones)) {
                throw new Error("La respuesta de Logística es inválida.");
            }

            if (!paginaActiva || document.hidden) return;

            camiones = datos.camiones;
            renderizar();

            aviso.textContent = camiones.length
                ? "Actualización automática · Última consulta: "
                + new Date().toLocaleTimeString("es-AR")
                : "Todavía no hay camiones. Registralos desde la pestaña Camiones.";

        } catch (error) {
            if (paginaActiva && !document.hidden) {
                // No presentamos puntos conservados como información en vivo.
                camiones = camiones.map(c => ({ ...c, reciente: false }));
                renderizar();

                aviso.textContent =
                    "No se pudo actualizar. Se conserva la última información "
                    + "y se reintentará automáticamente.";
            }
        } finally {
            clearTimeout(timeout);
            consultando = false;
            programar();
        }
    }

    $("mon-todos").addEventListener("click", () => elegir(null));

    document.addEventListener("visibilitychange", () => {
        clearTimeout(temporizador);
        if (!document.hidden) actualizar();
    });

    window.addEventListener("pagehide", () => {
        paginaActiva = false;
        clearTimeout(temporizador);
    });

    window.addEventListener("pageshow", () => {
        paginaActiva = true;
        actualizar();
    });

    actualizar();
})();
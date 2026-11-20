(() => {
    "use strict";

    const key = document
        .querySelector('meta[name="geoapify-key"]')
        ?.content.trim();

    function coordenadas(punto) {
        if (!punto
            || !Number.isFinite(punto.lat)
            || !Number.isFinite(punto.lng)
            || Math.abs(punto.lat) > 90
            || Math.abs(punto.lng) > 180) {
            return null;
        }

        return [punto.lat, punto.lng];
    }

    document.querySelectorAll("[data-seguimiento-id]")
        .forEach(iniciar);

    const entregaSolicitada = new URLSearchParams(
        location.search
    ).get("entrega");

    if (entregaSolicitada) {
        const panel = [
            ...document.querySelectorAll("[data-seguimiento-id]")
        ].find(
            elemento =>
                elemento.dataset.seguimientoId === entregaSolicitada
        );

        const detalle = panel?.closest("details");

        if (detalle) {
            detalle.open = true;

            requestAnimationFrame(() => {
                detalle.scrollIntoView({
                    block: "start"
                });
            });
        }
    }


    function decodificarRecorrido(cadena) {
        if (typeof cadena !== "string" || !cadena.length) {
            return [];
        }

        const puntos = [];
        let indice = 0;
        let latitud = 0;
        let longitud = 0;

        function leerDiferencia() {
            let resultado = 0;
            let desplazamiento = 0;

            while (true) {
                if (indice >= cadena.length || desplazamiento > 30) {
                    throw new Error("Geometría de ruta inválida.");
                }

                const valor = cadena.charCodeAt(indice++) - 63;

                if (valor < 0 || valor > 63) {
                    throw new Error("Geometría de ruta inválida.");
                }

                resultado += (valor & 31) * (2 ** desplazamiento);

                if (valor < 32) break;

                desplazamiento += 5;
            }

            return resultado % 2 === 1
                ? -(Math.floor(resultado / 2) + 1)
                : resultado / 2;
        }

        while (indice < cadena.length) {
            latitud += leerDiferencia();
            longitud += leerDiferencia();

            const lat = latitud / 100000;
            const lng = longitud / 100000;

            if (!Number.isFinite(lat)
                || !Number.isFinite(lng)
                || Math.abs(lat) > 90
                || Math.abs(lng) > 180) {
                throw new Error("El recorrido contiene coordenadas inválidas.");
            }

            puntos.push([lat, lng]);
        }

        if (puntos.length < 2) {
            throw new Error("El recorrido no tiene suficientes puntos.");
        }

        return puntos;
    }

    function iniciar(panel) {
        const detalle = panel.closest("details");
        const mensaje = panel.querySelector(".dt-seguimiento-mensaje");
        const fecha = panel.querySelector(".dt-seguimiento-fecha");
        const contenedor = panel.querySelector(".dt-google-mapa");
        const leyenda = panel.querySelector(".dt-mapa-leyenda");


        let mapa;
        let marcadores;
        let recorrido;
        let consultando = false;
        let temporizador;
        let encuadreInicial = false;
        let seguimientoFinalizado = false;

        function estaVisible() {
            return panel.isConnected
                && !document.hidden
                && (!detalle || detalle.open);
        }

        function programarActualizacion() {
            clearTimeout(temporizador);

            if (estaVisible() && !seguimientoFinalizado) {
                temporizador = setTimeout(actualizar, 5000);
            }
        }

        function crearMapa() {
            if (mapa) return;

            mapa = L.map(contenedor, {
                scrollWheelZoom: false
            }).setView([-34.6037, -58.3816], 13);

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
            ).addTo(mapa).on("tileerror", () => {
                mensaje.textContent =
                    "No se pudo cargar parte del mapa. "
                    + "Revisá la conexión o las restricciones de la key.";
            });

            marcadores = L.featureGroup().addTo(mapa);

            new ResizeObserver(() => {
                if (!contenedor.hidden) {
                    mapa.invalidateSize();
                }
            }).observe(contenedor);
        }

        function agregarPunto(punto) {
            const texto = document.createElement("div");
            texto.textContent = punto.nombre;

            L.circleMarker(punto.posicion, {
                radius: 10,
                color: "#ffffff",
                weight: 3,
                fillColor: punto.color,
                fillOpacity: 1
            })
                .bindPopup(texto)
                .addTo(marcadores);

            const elemento = document.createElement("span");
            elemento.style.display = "inline-flex";
            elemento.style.alignItems = "center";
            elemento.style.gap = "7px";

            const indicador = document.createElement("span");
            indicador.setAttribute("aria-hidden", "true");

            Object.assign(indicador.style, {
                width: "10px",
                height: "10px",
                borderRadius: "50%",
                backgroundColor: punto.color,
                flexShrink: "0"
            });

            elemento.append(
                indicador,
                document.createTextNode(punto.etiqueta)
            );

            leyenda.append(elemento);
        }

        async function actualizar() {
            clearTimeout(temporizador);

            if (consultando || !estaVisible()) return;

            consultando = true;

            if (!mapa) {
                mensaje.textContent = "Consultando seguimiento…";
            }


            const abortador = new AbortController();
            const timeout = setTimeout(() => abortador.abort(), 15000);

            try {
                if (!key || !window.L) {
                    throw new Error(
                        "No se pudo iniciar el mapa. "
                        + "Revisá la carga de Leaflet y GEOAPIFY_API_KEY."
                    );
                }

                const id = encodeURIComponent(panel.dataset.seguimientoId);
                const base = panel.dataset.seguimientoBase
                    || "/mis-donaciones";

                const respuesta = await fetch(
                    `${base}/${id}/seguimiento`,
                    {
                        headers: { Accept: "application/json" },
                        cache: "no-store",
                        signal: abortador.signal
                    }
                );

                const tipo = respuesta.headers.get("content-type") || "";

                if (!tipo.includes("application/json")) {
                    throw new Error(
                        "No se pudo consultar el seguimiento. "
                        + "Recargá la página y verificá tu sesión."
                    );
                }

                const datos = await respuesta.json();

                if (!respuesta.ok) {
                    throw new Error(
                        datos.mensaje
                        || datos.message
                        || "No se pudo consultar el seguimiento."
                    );
                }

                seguimientoFinalizado = datos.seguimientoFinalizado === true;

                // El usuario pudo cerrar los detalles durante la consulta.
                if (!estaVisible()) return;

                const puntos = [
                    {
                        posicion: coordenadas(datos.deposito),
                        nombre: "Depósito de salida",
                        etiqueta: "Depósito",
                        color: "#d58b40"
                    },
                    {
                        posicion: coordenadas(datos.destino),
                        nombre: "Entrega: " + (datos.entidad || "Entidad"),
                        etiqueta: "Destino",
                        color: "#42966b"
                    },
                    {
                        posicion: coordenadas(datos.camion),
                        nombre: "Último reporte del camión "
                            + (datos.patente || ""),
                        etiqueta: "Último reporte del camión",
                        color: "#4285cf"
                    }
                ].filter(p => p.posicion);

                const avisos = [];

                if (datos.patente) {
                    avisos.push(`Camión: ${datos.patente}.`);
                }

                if (datos.fechaPlanificada) {
                    avisos.push(
                        `Entrega planificada: ${datos.fechaPlanificada}.`
                    );
                }

                if (!coordenadas(datos.deposito)) {
                    avisos.push("El depósito no tiene coordenadas registradas.");
                }

                if (!coordenadas(datos.destino)) {
                    avisos.push("El destino no tiene coordenadas registradas.");
                }

                if (!seguimientoFinalizado
                    && datos.estadoEntrega === "EN_TRASLADO"
                    && !coordenadas(datos.camion)) {
                    avisos.push(
                        "Todavía no hay un reporte de ubicación del camión "
                        + "disponible para esta entrega."
                    );
                }

                if (!puntos.length) {
                    mensaje.textContent = avisos.join(" ")
                        + " No hay ubicaciones para mostrar.";
                    return;
                }

                contenedor.hidden = false;

                // Esperamos a que el navegador le asigne tamaño al mapa.
                await new Promise(resolve => requestAnimationFrame(resolve));

                if (!estaVisible()) return;

                crearMapa();
                mapa.invalidateSize({ animate: false });

                marcadores.clearLayers();

                if (recorrido) {
                    recorrido.remove();
                    recorrido = null;
                }

                leyenda.replaceChildren();
                fecha.textContent = "";

                puntos.forEach(agregarPunto);

                const limites = marcadores.getBounds();

                if (datos.geometriaPlanificada) {
                    try {
                        const coordenadasRuta = decodificarRecorrido(
                            datos.geometriaPlanificada
                        );

                        recorrido = L.polyline(coordenadasRuta, {
                            color: "#ad4d32",
                            weight: 5,
                            opacity: 0.85,
                            lineCap: "round",
                            lineJoin: "round"
                        }).addTo(mapa);

                        recorrido.bindTooltip("Recorrido planificado");
                        recorrido.bringToBack();

                        limites.extend(recorrido.getBounds());

                        const elemento = document.createElement("span");
                        elemento.style.display = "inline-flex";
                        elemento.style.alignItems = "center";
                        elemento.style.gap = "7px";

                        const muestra = document.createElement("span");
                        muestra.setAttribute("aria-hidden", "true");

                        Object.assign(muestra.style, {
                            display: "inline-block",
                            width: "22px",
                            height: "4px",
                            borderRadius: "2px",
                            backgroundColor: "#ad4d32"
                        });

                        elemento.append(
                            muestra,
                            document.createTextNode("Recorrido planificado")
                        );

                        leyenda.append(elemento);

                    } catch (error) {
                        console.error("Error al dibujar el recorrido:", error);

                        if (recorrido) {
                            recorrido.remove();
                            recorrido = null;
                        }

                        avisos.push(
                            "No se pudo dibujar el recorrido guardado."
                        );
                    }
                } else {
                    avisos.push(
                        "Esta ruta no tiene un recorrido guardado."
                    );
                }

                leyenda.hidden = false;

                requestAnimationFrame(() => {
                    if (!estaVisible()) return;

                    mapa.invalidateSize({ animate: false });

                    if (!encuadreInicial && limites.isValid()) {
                        mapa.fitBounds(limites, {
                            padding: [40, 40],
                            maxZoom: 16,
                            animate: false
                        });

                        encuadreInicial = true;
                    }
                });

                if (seguimientoFinalizado) {
                    avisos.push(
                        datos.llegoADestino
                            ? "El camión llegó al destino. El seguimiento de ubicación finalizó."
                            : "El seguimiento de esta entrega finalizó."
                    );
                }

                mensaje.textContent = avisos.join(" ");

                if (coordenadas(datos.camion) && datos.camion.fecha) {
                    fecha.textContent =
                        "Última ubicación registrada: "
                        + datos.camion.fecha.replace("T", " ");
                }

            } catch (error) {
                if (estaVisible()) {
                    mensaje.textContent =
                        (error.name === "AbortError"
                            ? "La consulta tardó demasiado."
                            : error.message)
                        + (mapa
                            ? " Se conserva la última información mostrada."
                            : "")
                        + " Reintentaremos automáticamente.";
                }
            } finally {
                clearTimeout(timeout);
                consultando = false;
                programarActualizacion();
            }
        }

        function cambiarVisibilidad() {
            clearTimeout(temporizador);

            if (estaVisible()) {
                actualizar();
            }
        }

        detalle?.addEventListener("toggle", cambiarVisibilidad);
        document.addEventListener("visibilitychange", cambiarVisibilidad);

        window.addEventListener("pagehide", () => {
            clearTimeout(temporizador);
        });

        window.addEventListener("pageshow", cambiarVisibilidad);

        if (estaVisible()) actualizar();
    }
})();
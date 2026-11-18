(() => {
    "use strict";

    const key = document
        .querySelector('meta[name="geoapify-key"]')
        ?.content.trim();

    document.querySelectorAll("[data-direccion-entidad]")
        .forEach(iniciar);

    function iniciar(root) {
        const buscar = root.querySelector("[data-direccion-busqueda]");
        const resultados = root.querySelector("[data-direccion-resultados]");
        const estado = root.querySelector("[data-direccion-estado]");
        const contenedorMapa = root.querySelector("[data-direccion-mapa]");
        const formulario = root.closest("form");

        const direccion = root.querySelector('[name="direccion"]');
        const latitud = root.querySelector('[name="latitud"]');
        const longitud = root.querySelector('[name="longitud"]');
        const confirmada = root.querySelector('[name="ubicacionConfirmada"]');

        let temporizador;
        let solicitud;
        let version = 0;
        let mapa;
        let marcador;
        let seleccion = null;

        function mensaje(texto = "") {
            estado.textContent = texto;
            estado.hidden = !texto;
        }

        function limpiarResultados() {
            resultados.replaceChildren();
            resultados.hidden = true;
        }

        formulario.addEventListener("submit", event => {
            if (!seleccion || confirmada.value !== "true") {
                event.preventDefault();
                mensaje("Seleccioná una dirección de las sugerencias.");
                buscar.focus();
            }
        });

        if (!key || !window.L) {
            buscar.disabled = true;
            mensaje(
                !key
                    ? "Falta configurar el buscador de direcciones."
                    : "No se pudo cargar el mapa. Recargá la página."
            );
            return;
        }

        buscar.addEventListener("input", () => {
            const actual = ++version;

            clearTimeout(temporizador);
            solicitud?.abort();

            seleccion = null;
            direccion.value = "";
            latitud.value = "";
            longitud.value = "";
            confirmada.value = "false";
            contenedorMapa.hidden = true;

            limpiarResultados();
            mensaje();

            const texto = buscar.value.trim();

            if (texto.length < 5) return;

            temporizador = setTimeout(
                () => consultar(texto, actual),
                450
            );
        });

        async function consultar(texto, actual) {
            const controller = new AbortController();
            solicitud = controller;

            mensaje("Buscando…");

            const parametros = new URLSearchParams({
                text: texto,
                filter: "countrycode:ar",
                lang: "es",
                limit: "5",
                format: "json",
                apiKey: key
            });

            try {
                const respuesta = await fetch(
                    "https://api.geoapify.com/v1/geocode/autocomplete?"
                    + parametros,
                    { signal: controller.signal }
                );

                if (!respuesta.ok) {
                    throw new Error(
                        respuesta.status === 429
                            ? "Se alcanzó el límite de consultas. Intentá más tarde."
                            : "No se pudieron consultar las direcciones."
                    );
                }

                const datos = await respuesta.json();

                if (actual !== version) return;

                limpiarResultados();

                const opciones = (datos.results || []).filter(p =>
                    typeof p.formatted === "string"
                    && Number.isFinite(p.lat)
                    && Number.isFinite(p.lon)
                );

                if (!opciones.length) {
                    mensaje(
                        "No encontramos esa dirección. Probá agregando la localidad."
                    );
                    return;
                }

                mensaje();

                opciones.forEach(p => {
                    const boton = document.createElement("button");

                    boton.type = "button";
                    boton.className = "btn btn-secondary";
                    boton.style.textAlign = "left";
                    boton.style.whiteSpace = "normal";
                    boton.textContent = p.formatted;

                    boton.addEventListener("click", () => elegir(p));
                    resultados.append(boton);
                });

                resultados.hidden = false;

            } catch (error) {
                if (error.name === "AbortError" || actual !== version) {
                    return;
                }

                limpiarResultados();
                mensaje(error.message || "No se pudo buscar la dirección.");
            }
        }

        function elegir(p) {
            if (!p.street || !p.housenumber) {
                mensaje(
                    "Elegí una dirección que incluya calle y altura."
                );
                return;
            }

            if (p.formatted.length > 180) {
                mensaje(
                    "La dirección supera los 180 caracteres permitidos."
                );
                return;
            }

            ++version;
            solicitud?.abort();
            clearTimeout(temporizador);

            seleccion = p;
            buscar.value = p.formatted;
            direccion.value = p.formatted;
            latitud.value = String(p.lat);
            longitud.value = String(p.lon);

            // La selección queda lista para confirmar con Guardar dirección.
            confirmada.value = "true";

            limpiarResultados();
            mensaje();
            contenedorMapa.hidden = false;

            if (!mapa) {
                mapa = L.map(contenedorMapa, {
                    scrollWheelZoom: false
                });

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
                    mensaje(
                        "No se pudo cargar parte del mapa. Revisá tu conexión."
                    );
                });

                marcador = L.circleMarker([p.lat, p.lon], {
                    radius: 9,
                    color: "#ffffff",
                    weight: 3,
                    fillColor: "#ad4d32",
                    fillOpacity: 1
                }).addTo(mapa);

                new ResizeObserver(() => {
                    if (!contenedorMapa.hidden) {
                        mapa.invalidateSize();
                    }
                }).observe(contenedorMapa);
            }

            marcador.setLatLng([p.lat, p.lon]);

            requestAnimationFrame(() => {
                mapa.invalidateSize();
                mapa.setView([p.lat, p.lon], 17);
            });
        }
    }
})();
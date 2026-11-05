(() => {
    let cargaGoogle;

    const estilos = [
        {
            elementType: "geometry",
            stylers: [{ color: "#212121" }]
        },
        {
            elementType: "labels.text.stroke",
            stylers: [{ color: "#212121" }]
        },
        {
            elementType: "labels.text.fill",
            stylers: [{ color: "#757575" }]
        },
        {
            featureType: "road",
            elementType: "geometry",
            stylers: [{ color: "#333333" }]
        },
        {
            featureType: "water",
            elementType: "geometry",
            stylers: [{ color: "#0f0f0f" }]
        }
    ];

    function cargarGoogle() {
        if (window.google?.maps?.Map) {
            return Promise.resolve();
        }

        if (cargaGoogle) {
            return cargaGoogle;
        }

        const key = document.querySelector(
            'meta[name="google-maps-key"]'
        )?.content.trim();

        if (!key) {
            return Promise.reject(
                new Error("Falta configurar GOOGLE_MAPS_API_KEY.")
            );
        }

        cargaGoogle = new Promise((resolve, reject) => {
            const script = document.createElement("script");

            const timer = setTimeout(() => {
                reject(new Error("Google Maps tardó demasiado en responder."));
            }, 15000);

            window.dtGoogleMapsListo = () => {
                clearTimeout(timer);
                resolve();
            };

            window.gm_authFailure = () => {
                clearTimeout(timer);
                reject(new Error(
                    "Google Maps rechazó la clave o su configuración."
                ));
            };

            script.onerror = () => {
                clearTimeout(timer);
                reject(new Error("No se pudo cargar Google Maps."));
            };

            const parametros = new URLSearchParams({
                key,
                callback: "dtGoogleMapsListo",
                loading: "async",
                language: "es"
            });

            script.src =
                "https://maps.googleapis.com/maps/api/js?" + parametros;

            script.async = true;
            document.head.appendChild(script);
        });

        return cargaGoogle;
    }

    function coordenadas(punto) {
        if (!punto
            || typeof punto.lat !== "number"
            || typeof punto.lng !== "number"
            || !Number.isFinite(punto.lat)
            || !Number.isFinite(punto.lng)
            || Math.abs(punto.lat) > 90
            || Math.abs(punto.lng) > 180) {
            return null;
        }

        return { lat: punto.lat, lng: punto.lng };
    }

    document.querySelectorAll("[data-seguimiento-id]").forEach((panel) => {
        const detalle = panel.closest("details");
        const mensaje = panel.querySelector(".dt-seguimiento-mensaje");
        const fecha = panel.querySelector(".dt-seguimiento-fecha");
        const contenedor = panel.querySelector(".dt-google-mapa");
        const leyenda = panel.querySelector(".dt-mapa-leyenda");
        const boton = panel.querySelector(".dt-actualizar-mapa");

        let mapa;
        let circulos = [];
        let consultando = false;
        let consultado = false;

        async function actualizar() {
            if (consultando) return;

            consultando = true;
            boton.disabled = true;
            mensaje.textContent = "Consultando seguimiento…";
            fecha.textContent = "";

            // Evitamos conservar posiciones antiguas si la consulta falla.
            circulos.forEach((circulo) => circulo.setMap(null));
            circulos = [];
            contenedor.hidden = true;
            leyenda.hidden = true;

            const abortador = new AbortController();
            const timeout = setTimeout(() => abortador.abort(), 10000);

            try {
                const id = encodeURIComponent(panel.dataset.seguimientoId);

                const respuesta = await fetch(
                    `/mis-donaciones/${id}/seguimiento`,
                    {
                        headers: { Accept: "application/json" },
                        cache: "no-store",
                        signal: abortador.signal
                    }
                );

                const datos = await respuesta.json();

                if (!respuesta.ok) {
                    throw new Error(
                        datos.mensaje || "No se pudo consultar el seguimiento."
                    );
                }

                const puntos = [
                    {
                        posicion: coordenadas(datos.deposito),
                        nombre: "Depósito",
                        color: "#e49b58"
                    },
                    {
                        posicion: coordenadas(datos.destino),
                        nombre: "Destino: " + (datos.entidad || ""),
                        color: "#77c596"
                    },
                    {
                        posicion: coordenadas(datos.camion),
                        nombre: "Camión " + (datos.patente || ""),
                        color: "#6db7ff"
                    }
                ].filter((punto) => punto.posicion);

                const avisos = [
                    `Camión asignado: ${datos.patente}.`
                ];

                if (datos.fechaPlanificada) {
                    avisos.push(`Fecha planificada: ${datos.fechaPlanificada}.`);
                }

                if (!coordenadas(datos.destino)) {
                    avisos.push("El destino todavía no tiene coordenadas.");
                }

                if (!coordenadas(datos.camion)) {
                    avisos.push(
                        "No hay una ubicación del camión disponible "
                        + "para esta entrega."
                    );
                }

                if (puntos.length === 0) {
                    mensaje.textContent =
                        avisos.join(" ") + " No hay puntos para mostrar.";
                    return;
                }

                await cargarGoogle();

                // Puede haberse cerrado mientras respondían los servicios.
                if (!detalle.open) return;

                contenedor.hidden = false;

                if (!mapa) {
                    mapa = new google.maps.Map(contenedor, {
                        center: puntos[0].posicion,
                        zoom: 14,
                        styles: estilos,
                        streetViewControl: false,
                        mapTypeControl: false,
                        fullscreenControl: true,
                        gestureHandling: "cooperative"
                    });
                }

                const limites = new google.maps.LatLngBounds();
                const info = new google.maps.InfoWindow();

                puntos.forEach((punto) => {
                    const circulo = new google.maps.Circle({
                        map: mapa,
                        center: punto.posicion,
                        radius: 65,
                        strokeColor: "#ffffff",
                        strokeWeight: 2,
                        fillColor: punto.color,
                        fillOpacity: 1
                    });

                    circulo.addListener("click", () => {
                        const texto = document.createElement("span");
                        texto.textContent = punto.nombre;

                        info.setContent(texto);
                        info.setPosition(punto.posicion);
                        info.open({ map: mapa });
                    });

                    circulos.push(circulo);
                    limites.extend(punto.posicion);
                });

                if (puntos.length === 1) {
                    mapa.setCenter(puntos[0].posicion);
                    mapa.setZoom(14);
                } else {
                    mapa.fitBounds(limites, 48);
                }

                leyenda.hidden = false;

                mensaje.textContent =
                    avisos.join(" ")
                    + " Se muestran ubicaciones, no el recorrido por calles.";

                if (datos.camion?.fecha) {
                    fecha.textContent =
                        "Último reporte registrado: "
                        + datos.camion.fecha.replace("T", " ");
                }

            } catch (error) {
                mensaje.textContent = error.name === "AbortError"
                    ? "La consulta tardó demasiado. Volvé a intentar."
                    : error.message;
            } finally {
                clearTimeout(timeout);
                consultando = false;
                consultado = true;
                boton.disabled = false;
            }
        }

        detalle.addEventListener("toggle", () => {
            if (detalle.open) {
                actualizar();
            }
        });

        boton.addEventListener("click", actualizar);

        if (detalle.open && !consultado) {
            actualizar();
        }
    });
})();
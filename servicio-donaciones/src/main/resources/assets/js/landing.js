(function() {
    'use strict';
    let map;
    let markers = [];
    let currentAssignments = [];
    let currentEntities = [];
    let currentDonations = [];
    window.initMap = async function initMap() {
        if (document.getElementById('mapa-google').offsetWidth === 0) { setTimeout(initMap, 100); return; }
        const latitude = parseFloat(document.getElementById('mapa-center-lat')?.value || '-34.59837584787982');
        const longitude = parseFloat(document.getElementById('mapa-center-lng')?.value || '-58.420018200000264');
        map = new google.maps.Map(document.getElementById('mapa-google'), {
            center: { lat: latitude, lng: longitude }, zoom: 10,
            styles: [
                { elementType: 'geometry', stylers: [{ color: '#212121' }] },
                { elementType: 'labels.text.stroke', stylers: [{ color: '#212121' }] },
                { elementType: 'labels.text.fill', stylers: [{ color: '#757575' }] },
                { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#333333' }] },
                { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0f0f0f' }] }
            ]
        });
        await cargarDatos();
    };
    async function cargarDatos() {
        try {
            const respuesta = await fetch("/api/landing");
            if (!respuesta.ok) {
                throw new Error("No se pudieron cargar los datos del mapa.");
            }
            const data = await respuesta.json();

            currentAssignments = data.asignaciones || [];
            currentEntities = data.entidades || [];
            currentDonations = data.donaciones || [];

            window.currentData = {
                asignaciones: currentAssignments,
                entidades: currentEntities,
                donaciones: currentDonations
            };

            addMarkers();
            updateCounters();
        } catch (error) {
            console.warn("Error al cargar los datos del mapa:", error);
            currentAssignments = [];
            currentEntities = [];
            currentDonations = [];
            addMarkers();
            updateCounters();
        }
    }




        window.currentData = { asignaciones: currentAssignments, entidades: currentEntities, donaciones: currentDonations };



    function getEntidadDetails(entidadId) { return currentEntities.find(e => e.id == entidadId) || null; }
    function getDonacionDetails(donacionId) { return currentDonations.find(d => d.id == donacionId) || null; }
    function getAsignacionDetails(asignacionId) { return currentAssignments.find(a => a.id == asignacionId) || null; }
    function getEstadoClass(estado) {
        if (!estado) return '';
        const u = estado.toUpperCase();
        if (u.includes('ENTREGAD')) return 'estado-entregada';
        if (u.includes('TRASLAD')) return 'estado-traslado';
        return 'estado-activa';
    }
    function getFotoUrl(foto) {
        const valor = typeof foto === "string"
            ? foto
            : foto?.url ?? foto?.foto;

        if (typeof valor !== "string" || !valor.trim()) {
            return null;
        }

        const ruta = valor.trim();

        try {
            const url = new URL(
                /^[a-z][a-z0-9+.-]*:/i.test(ruta) || ruta.startsWith("/")
                    ? ruta
                    : "/assets/fotos/" + ruta,
                window.location.origin
            );

            return ["http:", "https:"].includes(url.protocol)
                ? url.href
                : null;
        } catch {
            return null;
        }
    }

    function getFotoInfo(donacion) {
        if (!donacion) return [];
        if (Array.isArray(donacion.fotos)) return donacion.fotos;
        if (donacion.bienes && Array.isArray(donacion.bienes)) {
            return donacion.bienes.filter(b => b.foto).map(b => ({ url: b.foto }));
        }
        return [];
    }
    function addMarkers(entidadesSeleccionadas = []) {
        markers.forEach(marker => marker.setMap(null));
        markers = [];

        if (!map) return;

        const seleccionadas = new Set(
            entidadesSeleccionadas.map(String)
        );

        const bounds = new google.maps.LatLngBounds();

        currentAssignments.forEach(asignacion => {
            const entidad = getEntidadDetails(asignacion.entidadId);

            if (!entidad) return;

            if (
                seleccionadas.size > 0 &&
                !seleccionadas.has(String(entidad.id))
            ) {
                return;
            }

            if (entidad.latitud == null || entidad.longitud == null) {
                return;
            }

            const lat = Number(entidad.latitud);
            const lng = Number(entidad.longitud);

            if (
                !Number.isFinite(lat) ||
                !Number.isFinite(lng) ||
                lat < -90 || lat > 90 ||
                lng < -180 || lng > 180
            ) {
                return;
            }

            const donacion = getDonacionDetails(asignacion.donacionId);

            const estado = String(
                donacion?.estadoActualTexto ?? ""
            ).toUpperCase();

            const entregada = estado === "ENTREGADA";
            const enTraslado = estado === "EN_TRASLADO";

            const marker = new google.maps.Marker({
                position: { lat, lng },
                map,
                title: entidad.razonSocial,
                zIndex: 10,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    strokeColor: entregada
                        ? "#e65100"
                        : enTraslado ? "#1e88e5" : "#e53935",
                    fillColor: entregada
                        ? "#ff8f00"
                        : enTraslado ? "#42a5f5" : "#ef5350",
                    strokeWeight: 2,
                    fillOpacity: 0.8,
                    scale: 8
                }
            });

            marker.asignacionEntidadId = String(entidad.id);

            marker.addListener("click", () => {
                showDetail(asignacion, entidad, donacion);
            });

            markers.push(marker);
            bounds.extend({ lat, lng });
        });

        if (markers.length > 0) {
            map.fitBounds(bounds);

            if (markers.length === 1) {
                google.maps.event.addListenerOnce(map, "idle", () => {
                    if (map.getZoom() > 15) {
                        map.setZoom(15);
                    }
                });
            }
        }
    }
function showDetail(asignacion, entidad, donacion) {
    const card = document.getElementById('map-detail-card');
    const content = document.getElementById('detalle-contenido');
    if (!card || !content) return;
    let html = '<div class="detail-header"><span class="badge badge-info">' + escapeHtml(entidad.razonSocial) + '</span></div>';
    html += '<h3 class="font-manrope">' + escapeHtml(entidad.razonSocial) + '</h3>';
    html += '<p class="detail-subtitle">' + escapeHtml(entidad.direccion || '-') + '</p>';
    html += '<p class="detail-subtitle">Teléfono: '
        + escapeHtml(entidad.telefono || '-')
        + '</p>';
    if (donacion) {
        html += '<div class="detail-stats">';
        html += '<div class="stat"><strong>' + (donacion.cantidad || 0) + '</strong><span>' + escapeHtml(donacion.unidadMedida || 'unidades') + '</span></div>';
        html += '<div class="stat"><strong>' + escapeHtml(donacion.descripcionGeneral || '-') + '</strong></div>';
        html += '</div>';
        html += '<p class="detail-status">Estado: <strong>' + escapeHtml(donacion.estadoActualTexto) + '</strong></p>';
        const fotos = getFotoInfo(donacion);
        if (fotos.length > 0) {
            html += '<div class="fotos-grid">';
            fotos.forEach((foto, i) => {
                const fotoUrl = getFotoUrl(foto);
                if (!fotoUrl) return;
                html += '<a href="' + escapeHtml(fotoUrl) + '" target="_blank" class="foto-link">';
                html += '<img src="' + escapeHtml(fotoUrl) + '" alt="Foto ' + (i + 1) + '" loading="lazy">';
                html += '</a>';
            });
            html += '</div>';
        } else {
            html += '<p class="no-fotos">Sin fotos registradas para esta entrega.</p>';
        }
    }
    content.innerHTML = html;
    card.style.display = 'block';
}
function escapeHtml(text) {
    if (!text) return '';
    return String(text).replace(/[&<>"']/g, function(c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
}
function openPhoto(url) { window.open(url, '_blank'); }
    function updateCounters() {
        let activas = 0;
        let enTraslado = 0;
        let entregadas = 0;

        currentAssignments.forEach(asignacion => {
            const donacion = getDonacionDetails(asignacion.donacionId);

            if (!donacion) return;

            const estado = String(
                donacion.estadoActualTexto ?? ""
            ).toUpperCase();

            if (estado === "ENTREGADA") {
                entregadas++;
            } else if (estado === "EN_TRASLADO") {
                enTraslado++;
            } else if ([
                "EN_DEPOSITO",
                "ASIGNACION_REALIZADA",
                "LISTA_PARA_ENTREGAR"
            ].includes(estado)) {
                activas++;
            }
        });

        const actualizar = (id, cantidad) => {
            const elemento = document.getElementById(id);

            if (elemento) {
                elemento.textContent = cantidad;
            }
        };

        actualizar("contador-activas", activas);
        actualizar("contador-traslado", enTraslado);
        actualizar("contador-entregadas", entregadas);
    }
    document.addEventListener("click", evento => {
        const card = document.getElementById("map-detail-card");
        const mapa = document.getElementById("mapa-google");

        if (!card || card.style.display !== "block") return;

        const dentroDelDetalle = card.contains(evento.target);
        const dentroDelMapa = mapa?.contains(evento.target);

        if (!dentroDelDetalle && !dentroDelMapa) {
            card.style.display = "none";
        }
    });

    document.getElementById("btn-cerrar-detalle")
        ?.addEventListener("click", () => {
            const card = document.getElementById("map-detail-card");

            if (card) {
                card.style.display = "none";
            }
        });
    const opcionesEntidades = document.getElementById("filtro-entidades");

    const selectorEntidades = opcionesEntidades
        ?.closest(".filter-group")
        ?.querySelector(".filter-select");

    selectorEntidades?.addEventListener("click", () => {
        const abierto = opcionesEntidades.style.display !== "none";

        opcionesEntidades.style.display = abierto ? "none" : "block";
    });

    document.addEventListener("click", evento => {
        const grupo = opcionesEntidades?.closest(".filter-group");

        if (grupo && !grupo.contains(evento.target)) {
            opcionesEntidades.style.display = "none";
        }
    });
    document.getElementById("btn-filtros-mapa")
        ?.addEventListener("click", () => {
            const seleccionadas = Array.from(
                document.querySelectorAll(
                    ".filtro-entidad-checkbox:checked"
                )
            ).map(checkbox => checkbox.value);

            addMarkers(seleccionadas);

            const detalle = document.getElementById("map-detail-card");

            if (detalle) {
                detalle.style.display = "none";
            }
        });
window.addEventListener('load', function() {
    if (window.google && window.google.maps) { initMap(); }
    else { setTimeout(function() { window.addEventListener('load', initMap); }, 200); }
});
if (document.readyState === 'complete' || document.readyState === 'interactive') {
    window.dispatchEvent(new Event('load'));
}
})();

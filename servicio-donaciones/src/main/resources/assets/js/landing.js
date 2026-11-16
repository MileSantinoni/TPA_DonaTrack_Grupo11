(function() {
    'use strict';

    let map;
    let markers = [];
    let currentAssignments = [];
    let currentEntities = [];
    let currentDonations = [];

    window.initMap = async function initMap() {
        const mapElement = document.getElementById('mapa-google');
        if (!mapElement) return;

        if (mapElement.offsetWidth === 0) {
            setTimeout(initMap, 100);
            return;
        }

        map = new google.maps.Map(mapElement, {
            center: { lat: -34.6037, lng: -58.3816 },
            zoom: 13,
            mapTypeControl: false,
            fullscreenControl: false,
            styles: [
                { elementType: 'geometry', stylers: [{ color: '#212121' }] },
                { elementType: 'labels.text.stroke', stylers: [{ color: '#212121' }] },
                { elementType: 'labels.text.fill', stylers: [{ color: '#757575' }] },
                { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#333333' }] },
                { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0f0f0f' }] }
            ]
        });

        // Ocultar overlay de carga si existe
        const loadingOverlay = document.getElementById('map-loading-overlay');
        if (loadingOverlay) loadingOverlay.style.display = 'none';

        // Cerrar tarjeta de detalle al hacer clic en un área vacía del mapa
        map.addListener('click', () => {
            const card = document.getElementById('map-detail-card');
            if (card) card.style.display = 'none';
        });

        // Configurar botón cerrar detalle
        const botonCerrar = document.getElementById('btn-cerrar-detalle');
        if (botonCerrar) {
            botonCerrar.addEventListener('click', (e) => {
                e.stopPropagation();
                const card = document.getElementById('map-detail-card');
                if (card) card.style.display = 'none';
            });
        }

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

    function getEntidadDetails(entidadId) {
        return currentEntities.find(e => String(e.id) === String(entidadId)) || null;
    }

    function getDonacionDetails(donacionId) {
        return currentDonations.find(d => String(d.id) === String(donacionId)) || null;
    }

    function getFotoUrl(foto) {
        if (!foto) return null;
        const url = typeof foto === 'string' ? foto : (foto.url || foto.path);
        if (!url) return null;
        return url.startsWith('http') ? url : '/assets/fotos/' + url;
    }

    function getFotoInfo(donacion) {
        if (!donacion) return [];
        if (Array.isArray(donacion.fotos)) return donacion.fotos;
        if (donacion.bienes && Array.isArray(donacion.bienes)) {
            return donacion.bienes.filter(b => b.foto).map(b => ({ url: b.foto }));
        }
        return [];
    }

    function addMarkers() {
        renderMarkers([], '');
    }

    function renderMarkers(filtroEntidades = [], filtroEstado = '') {
        markers.forEach(m => m.setMap(null));
        markers = [];
        if (!map) return;

        const bounds = new google.maps.LatLngBounds();
        let hasMarkers = false;

        // Agrupar asignaciones por entidad para calcular offsets y evitar solapamientos
        const asignacionesPorEntidad = {};
        currentAssignments.forEach(asig => {
            const key = String(asig.entidadId);
            if (!asignacionesPorEntidad[key]) {
                asignacionesPorEntidad[key] = [];
            }
            asignacionesPorEntidad[key].push(asig);
        });

        // 1. Marcadores de TODAS las Entidades Beneficiarias registradas
        currentEntities.forEach(entidad => {
            const lat = parseFloat(entidad.latitud);
            const lng = parseFloat(entidad.longitud);
            if (isNaN(lat) || isNaN(lng)) return;

            // Filtro por entidad si fue seleccionada
            if (filtroEntidades.length > 0 && !filtroEntidades.includes(String(entidad.id))) {
                return;
            }

            const asignacionesEntidad = asignacionesPorEntidad[String(entidad.id)] || [];

            // Si hay un filtro de estado activo, verificar si la entidad tiene alguna entrega con ese estado
            if (filtroEstado) {
                const coincide = asignacionesEntidad.some(a => {
                    const donacion = getDonacionDetails(a.donacionId);
                    const estado = (donacion && donacion.estadoActualTexto) ? donacion.estadoActualTexto : (a.estado || '');
                    return estado.toUpperCase() === filtroEstado.toUpperCase();
                });
                if (!coincide) {
                    return;
                }
            }

            // Marcador de entidad beneficiaria (círculo azul sólido con borde)
            const iconEntidad = {
                path: google.maps.SymbolPath.CIRCLE,
                strokeColor: '#0d47a1',
                strokeWeight: 2.5,
                fillColor: '#1e88e5',
                fillOpacity: 0.95,
                scale: 9
            };

            const entityMarker = new google.maps.Marker({
                position: { lat: lat, lng: lng },
                map: map,
                icon: iconEntidad,
                title: 'Entidad: ' + (entidad.razonSocial || 'Entidad Beneficiaria'),
                zIndex: 10
            });

            entityMarker.entidadId = entidad.id;
            entityMarker.tipo = 'entidad';
            entityMarker.addListener('click', () => {
                showEntidadDetail(entidad);
            });

            markers.push(entityMarker);
            bounds.extend({ lat: lat, lng: lng });
            hasMarkers = true;
        });

        // 2. Marcadores de TODAS las Entregas (Asignaciones)
        currentAssignments.forEach(asignacion => {
            // Filtro por entidad si fue seleccionada
            if (filtroEntidades.length > 0 && !filtroEntidades.includes(String(asignacion.entidadId))) {
                return;
            }

            const entidad = getEntidadDetails(asignacion.entidadId);
            if (!entidad) return;

            const baseLat = parseFloat(entidad.latitud);
            const baseLng = parseFloat(entidad.longitud);
            if (isNaN(baseLat) || isNaN(baseLng)) return;

            const donacion = getDonacionDetails(asignacion.donacionId);
            const estado = donacion && donacion.estadoActualTexto ? donacion.estadoActualTexto : (asignacion.estado || 'EN_DEPOSITO');
            const estadoUpper = estado.toUpperCase();

            // Filtro por estado
            if (filtroEstado && estadoUpper !== filtroEstado.toUpperCase()) {
                return;
            }

            // Calcular posición con offset radial para no tapar a la entidad ni a otras entregas
            const listaHermana = asignacionesPorEntidad[String(asignacion.entidadId)] || [asignacion];
            const indice = listaHermana.findIndex(a => String(a.id) === String(asignacion.id));
            const total = listaHermana.length;

            const radio = 0.00045; // ~50 metros
            const angulo = (2 * Math.PI * (indice >= 0 ? indice : 0)) / (total > 0 ? total : 1) + (Math.PI / 4);
            const entregaLat = baseLat + radio * Math.cos(angulo);
            const entregaLng = baseLng + radio * Math.sin(angulo);

            let strokeColor = '#c62828';
            let fillColor = '#ef5350';

            if (estadoUpper.includes('ENTREGAD')) {
                strokeColor = '#e65100'; // Naranja (Entrega realizada)
                fillColor = '#ff8f00';
            } else if (estadoUpper.includes('TRASLAD')) {
                strokeColor = '#00838f'; // Cyan (En traslado)
                fillColor = '#00acc1';
            } else {
                strokeColor = '#c62828'; // Rojo (Entrega activa / pendiente / en depósito)
                fillColor = '#ef5350';
            }

            const iconEntrega = {
                path: google.maps.SymbolPath.CIRCLE,
                strokeColor: strokeColor,
                strokeWeight: 2,
                fillColor: fillColor,
                fillOpacity: 0.9,
                scale: 7
            };

            const entregaMarker = new google.maps.Marker({
                position: { lat: entregaLat, lng: entregaLng },
                map: map,
                icon: iconEntrega,
                title: 'Entrega: ' + (donacion?.descripcionGeneral || 'Donación') + ' (' + entidad.razonSocial + ')',
                zIndex: 25
            });

            entregaMarker.asignacionId = asignacion.id;
            entregaMarker.tipo = 'entrega';
            entregaMarker.addListener('click', () => {
                showDetail(asignacion, entidad, donacion);
            });

            markers.push(entregaMarker);
            bounds.extend({ lat: entregaLat, lng: entregaLng });
            hasMarkers = true;
        });

        if (hasMarkers) {
            map.fitBounds(bounds);
        }
    }

    function showEntidadDetail(entidad) {
        const card = document.getElementById('map-detail-card');
        const content = document.getElementById('detalle-contenido');
        if (!card || !content) return;

        let html = '<div class="detail-header"></div>';
        html += '<h3 class="font-manrope">' + escapeHtml(entidad.razonSocial) + '</h3><br>';
        html += '<p class="detail-subtitle"><strong>Dirección: </strong>' + escapeHtml(entidad.direccion || '-') + '</p>';
        html += '<p class="detail-subtitle"><strong>Teléfono: </strong>' + escapeHtml(entidad.telefono || '-') + '</p><br>';
        html += '<p class="detail-status"><strong>Tipo: </strong>Entidad beneficiaria registrada</p><br>';

        const asignacionesEntidad = currentAssignments.filter(a => String(a.entidadId) === String(entidad.id));

        if (asignacionesEntidad.length > 0) {
            html += '<h5 class="font-manrope" style="margin-top: 8px; margin-bottom: 8px;">Entregas asignadas a esta entidad (' + asignacionesEntidad.length + ')</h5>';
            asignacionesEntidad.forEach((asig, idx) => {
                const donacion = getDonacionDetails(asig.donacionId);
                const estado = donacion && donacion.estadoActualTexto ? donacion.estadoActualTexto : (asig.estado || 'EN_DEPOSITO');
                html += '<div style="padding: 10px; margin-bottom: 10px; background: rgba(0,0,0,0.04); border-radius: 8px;">';
                html += '<p style="margin: 0;"><strong>Entrega #' + (idx + 1) + '</strong> - Estado: <strong>' + escapeHtml(estado) + '</strong></p>';
                if (donacion) {
                    html += '<p style="margin: 4px 0; font-size: 13px;"><strong>Cantidad: </strong>' + (donacion.cantidad || 0) + ' ' + escapeHtml(donacion.unidadMedida || 'unidades') + '</p>';
                    html += '<p style="margin: 4px 0; font-size: 13px;"><strong>Descripción: </strong>' + escapeHtml(donacion.descripcionGeneral || '-') + '</p>';
                }
                html += '</div>';
            });
        } else {
            html += '<p><em>No hay entregas asociadas registradas actualmente para esta entidad.</em></p>';
        }

        content.innerHTML = html;
        card.style.display = 'block';
    }

    function showDetail(asignacion, entidad, donacion) {
        const card = document.getElementById('map-detail-card');
        const content = document.getElementById('detalle-contenido');
        if (!card || !content) return;

        let html = '<div class="detail-header"></div>';
        html += '<h3 class="font-manrope">' + escapeHtml(entidad?.razonSocial || 'Entidad Beneficiaria') + '</h3><br>';
        html += '<p class="detail-subtitle"><strong>Destino: </strong>' + escapeHtml(entidad?.direccion || '-') + '</p>';
        html += '<p class="detail-subtitle"><strong>Teléfono: </strong>' + escapeHtml(entidad?.telefono || '-') + '</p><br>';

        if (donacion) {
            html += '<div class="detail-stats">';
            html += '<div class="stat"><strong>Cantidad: </strong>' + (donacion.cantidad || 0) + ' <span>' + escapeHtml(donacion.unidadMedida || 'unidades') + '</span></div><br>';
            html += '<div class="stat"><strong>Descripción: </strong>' + escapeHtml(donacion.descripcionGeneral || '-') + '</div>';
            html += '</div><br>';
            html += '<p class="detail-status"><strong>Estado de entrega: </strong>' + escapeHtml(donacion.estadoActualTexto || asignacion?.estado || 'EN_DEPOSITO') + '</p><br>';

            if (asignacion?.fechaRecepcion) {
                html += '<p class="detail-subtitle"><strong>Fecha de recepción: </strong>' + escapeHtml(asignacion.fechaRecepcion) + '</p><br>';
            }

            const fotos = getFotoInfo(donacion);
            if (fotos.length > 0) {
                html += '<h5 class="font-manrope">Fotos de la entrega</h5>';
                html += '<div class="fotos-grid">';
                fotos.forEach((foto, i) => {
                    const fotoUrl = getFotoUrl(foto);
                    if (fotoUrl) {
                        html += '<a href="' + escapeHtml(fotoUrl) + '" target="_blank" class="foto-link">';
                        html += '<img src="' + escapeHtml(fotoUrl) + '" alt="Foto ' + (i + 1) + '" loading="lazy">';
                        html += '</a>';
                    }
                });
                html += '</div>';
            } else {
                html += '<h5>Sin fotos registradas para esta entrega</h5>';
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

    function updateCounters() {
        const active = document.getElementById('contador-activas');
        const inTransit = document.getElementById('contador-traslado');
        const delivered = document.getElementById('contador-entregadas');
        let a = 0, t = 0, d = 0;

        currentAssignments.forEach(asignacion => {
            const donacion = getDonacionDetails(asignacion.donacionId);
            const estado = (donacion && donacion.estadoActualTexto) ? donacion.estadoActualTexto : (asignacion.estado || '');
            const estadoUpper = estado.toUpperCase();
            if (estadoUpper.includes('EN_DEPOSITO') || estadoUpper.includes('LISTA') || estadoUpper.includes('ASIGNACION')) a++;
            else if (estadoUpper.includes('TRASLAD')) t++;
            else if (estadoUpper.includes('ENTREGAD')) d++;
        });

        if (active) active.textContent = a;
        if (inTransit) inTransit.textContent = t;
        if (delivered) delivered.textContent = d;
    }

    // Cerrar el detalle únicamente cuando se hace clic fuera del mapa y fuera del detalle
    document.addEventListener('click', function(e) {
        const card = document.getElementById('map-detail-card');
        const mapContainer = document.getElementById('mapa-google');
        if (card && card.style.display === 'block') {
            if (!card.contains(e.target) && !mapContainer?.contains(e.target)) {
                card.style.display = 'none';
            }
        }
    });

    // Toggle para opciones del filtro de estados
    const filtroEstadoDiv = document.getElementById('filtro-estado');
    if (filtroEstadoDiv) {
        filtroEstadoDiv.addEventListener('click', function(e) {
            if (e.target.tagName !== 'INPUT' && e.target.tagName !== 'LABEL') {
                const opciones = this.querySelector('.filter-options-estado');
                if (opciones) {
                    opciones.style.display = opciones.style.display === 'none' ? 'block' : 'none';
                }
            }
        });
    }

    // Toggle para opciones del filtro de entidades
    const filtroEntidadesGroup = document.querySelector('.filter-group:nth-child(2) .filter-select');
    if (filtroEntidadesGroup) {
        filtroEntidadesGroup.addEventListener('click', function(e) {
            const opciones = document.getElementById('filtro-entidades');
            if (opciones) {
                opciones.style.display = opciones.style.display === 'none' ? 'block' : 'none';
            }
        });
    }

    // Botón de aplicar filtros
    document.getElementById('btn-filtros-mapa')?.addEventListener('click', function() {
        const checkboxes = document.querySelectorAll('.filtro-entidad-checkbox:checked');
        const selectedEntidades = Array.from(checkboxes).map(cb => cb.value);

        const selectedEstadoRadio = document.querySelector('input.filtro-estado-radio:checked');
        const selectedEstado = selectedEstadoRadio ? selectedEstadoRadio.value : '';

        renderMarkers(selectedEntidades, selectedEstado);

        const card = document.getElementById('map-detail-card');
        if (card) card.style.display = 'none';
    });

    window.addEventListener('load', function() {
        if (window.google && window.google.maps) {
            initMap();
        } else {
            setTimeout(function() {
                if (window.google && window.google.maps) {
                    initMap();
                }
            }, 300);
        }
    });

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        window.dispatchEvent(new Event('load'));
    }
})();

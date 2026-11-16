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
   // function getFotoUrl(bien) {
     //   if (!bien || !bien.foto) return null;
       // return bien.foto.startsWith('http') ? bien.foto : '/assets/fotos/' + bien.foto;
    //}

    function getFotoInfo(donacion) {
        if (!donacion) return [];
        if (Array.isArray(donacion.fotos)) return donacion.fotos;
        if (donacion.bienes && Array.isArray(donacion.bienes)) {
            return donacion.bienes.filter(b => b.foto).map(b => ({ url: b.foto }));
        }
        return [];
    }
function addMarkers() {
    markers.forEach(m => m.setMap(null));
    markers = [];
    if (!map) return;
    const bounds = new google.maps.LatLngBounds();
    currentAssignments.forEach(asignacion => {
        const entidad = getEntidadDetails(asignacion.entidadId);
        if (!entidad || entidad.latitud == null || entidad.longitud == null) return;
        const donacion = getDonacionDetails(asignacion.donacionId);
        const estado = donacion ? donacion.estadoActualTexto : 'EN_DEPOSITO';
        const isEntregada = estado.toUpperCase().includes('ENTREGAD');
        const isTransito = estado.toUpperCase().includes('TRASLAD');
        const icon = {
            path: google.maps.SymbolPath.CIRCLE,
            strokeColor: isEntregada ? '#e65100' : isTransito ? '#1e88e5' : '#e53935',
            strokeWeight: 2,
            fillColor: isEntregada ? '#ff8f00' : isTransito ? '#42a5f5' : '#ef5350',
            fillOpacity: 0.8,
            scale: 8
        };
        const marker = new google.maps.Marker({
            position: { lat: entidad.latitud, lng: entidad.longitud },
            map: map,
            icon: icon,
            title: entidad.razonSocial,
            zIndex: 10
        });
        marker.addListener('click', () => showDetail(asignacion, entidad, donacion));
        markers.push(marker);
        bounds.extend({ lat: entidad.latitud, lng: entidad.longitud });
    });
    map.fitBounds(bounds);
}
function showDetail(asignacion, entidad, donacion) {
    const card = document.getElementById('map-detail-card');
    const content = document.getElementById('detalle-contenido');
    if (!card || !content) return;
    let html = '<div class="detail-header"><span class="badge badge-info">' + escapeHtml(entidad.razonSocial) + '</span></div>';
    html += '<h3 class="font-manrope">' + escapeHtml(entidad.razonSocial) + '</h3>';
    html += '<p class="detail-subtitle">' + escapeHtml(entidad.direccion || '-') + '</p>';
    html += '<p class="detail-subtitle">TelÃ©fono: ' + (entidad.telefono || '-') + '</p>';
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
                html += '<a href="' + escapeHtml(fotoUrl) + '" target="_blank" class="foto-link">';
                html += '<img src="' + escapeHtml(fotoUrl) + '" alt="Foto ' + (i + 1) + '" loading="lazy">';
                html += '</a>';
            });
            html += '</div>';
        } else {
            html += '<p class="no-fotos">Sin fotos registradas para esta entrega.</p>';
        }
        const btnCerrar = document.getElementById('btn-cerrar-detalle');
        const detailCard = document.getElementById('map-detail-card');


        if (btnCerrar && detailCard) {
            btnCerrar.addEventListener('click', () => {
                detailCard.style.display = 'none';
            });
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
    const active = document.getElementById('contador-activas');
    const inTransit = document.getElementById('contador-traslado');
    const delivered = document.getElementById('contador-entregadas');
    let a = 0, t = 0, d = 0;
    currentAssignments.forEach(asignacion => {
        const donacion = getDonacionDetails(asignacion.donacionId);
        if (!donacion) return;
        const estado = donacion.estadoActualTexto.toUpperCase();
        if (estado.includes('EN_DEPOSITO') || estado.includes('LISTA')) a++;
        else if (estado.includes('TRASLAD')) t++;
        else if (estado.includes('ENTREGAD')) d++;
    });
    if (active) active.textContent = a;
    if (inTransit) inTransit.textContent = t;
    if (delivered) delivered.textContent = d;
}
document.addEventListener('click', function(e) {
    const card = document.getElementById('map-detail-card');
    if (card && card.style.display === 'block') {
        const rect = card.getBoundingClientRect();
        if (!rect.contains(e.clientX, e.clientY)) {
            card.style.display = 'none';
        }
    }
});
document.addEventListener('click', function(e) {
    const filtroEntidades = document.getElementById('filtro-entidades');
    if (filtroEntidades && e.target === filtroEntidades) {
        filtroEntidades.style.display = filtroEntidades.style.display === 'none' ? 'block' : 'none';
    }
});
document.getElementById('btn-filtros-mapa')?.addEventListener('click', function() {
    const checkboxes = document.querySelectorAll('.filtro-entidad-checkbox:checked');
    const selectedEntidades = Array.from(checkboxes).map(cb => cb.value);
    if (selectedEntidades.length === 0) { addMarkers(); return; }
    const filtered = currentAssignments.filter(asignacion => {
        return selectedEntidades.includes(asignacion.entidadId);
    });
    if (filtered.length === 0) {
        markers.forEach(m => m.setMap(null));
        markers = [];
        return;
    }
    const filteredMarkers = markers.filter(m => {
        return selectedEntidades.includes(m.asignacionEntidadId);
    });
    filteredMarkers.forEach(m => m.setMap(m.map));
    markers = filteredMarkers;
    document.getElementById('map-detail-card').style.display = 'none';
});
window.addEventListener('load', function() {
    if (window.google && window.google.maps) { initMap(); }
    else { setTimeout(function() { window.addEventListener('load', initMap); }, 200); }
});
if (document.readyState === 'complete' || document.readyState === 'interactive') {
    window.dispatchEvent(new Event('load'));
}
})();

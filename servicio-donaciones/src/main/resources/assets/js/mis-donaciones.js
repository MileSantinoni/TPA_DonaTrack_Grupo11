/* Interacciones de maqueta: no realiza llamadas HTTP. */
(() => {
  const form = document.querySelector('#dt-filtros');
  const cards = [...document.querySelectorAll('.dt-donacion')];
  function filtrar() {
    const filtros = new FormData(form);
    let cantidad = 0;
    cards.forEach(card => {
      const visible = ['estado', 'categoria', 'subcategoria'].every(key => !filtros.get(key) || filtros.get(key) === card.dataset[key]);
      card.hidden = !visible;
      if (visible) cantidad++;
    });
    document.querySelector('#dt-contador').textContent = `${cantidad} ${cantidad === 1 ? 'donación' : 'donaciones'}`;
    document.querySelector('#dt-vacio').hidden = cantidad !== 0;
  }
  form.addEventListener('submit', event => { event.preventDefault(); filtrar(); });
  form.addEventListener('reset', () => setTimeout(filtrar, 0));
  const aviso = document.querySelector('#dt-aviso');
  function avisar(texto) { aviso.hidden = false; aviso.textContent = texto; }
  document.querySelector('.dt-notificaciones').addEventListener('click', () => avisar('Las notificaciones mostradas son de ejemplo. Todavía no están conectadas.'));
  // Foco por teclado visible, sin highlight al usar el mouse.
  document.addEventListener('keydown', event => {
    if (event.key === 'Tab') document.body.classList.add('dt-teclado');
  });
  document.addEventListener('pointerdown', () => document.body.classList.remove('dt-teclado'));

  const contenedor = document.querySelector('#dt-mapa-entrega');
  const avisoMapa = document.querySelector('#dt-mapa-aviso');
  if (!window.L) {
    avisoMapa.hidden = false;
    avisoMapa.textContent = 'No se pudo cargar el mapa. Recargá la página.';
    return;
  }
  const mapa = L.map(contenedor, {scrollWheelZoom: false});
  const tiles = L.tileLayer(contenedor.dataset.tiles, {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(mapa);
  tiles.on('tileerror', () => {
    avisoMapa.hidden = false;
    avisoMapa.textContent = 'No se pudieron cargar algunas partes del mapa. Revisá tu conexión.';
  });
  tiles.on('tileload', () => { avisoMapa.hidden = true; });
  // Coordenadas y trazado ilustrativos. No son GPS ni una ruta calculada.
  const recorrido = [
    [-34.635, -58.475], [-34.625, -58.451], [-34.630, -58.431],
    [-34.615, -58.409], [-34.603, -58.397], [-34.596118, -58.394256]
  ];
  const linea = L.polyline(recorrido, {color:'#a64e36', weight:4}).addTo(mapa);
  [
    {p:recorrido[0], nombre:'Depósito', detalle:'Punto de salida de ejemplo.'},
    {p:recorrido[3], nombre:'Camión', detalle:'Posición de ejemplo; todavía sin seguimiento GPS.'},
    {p:recorrido[5], nombre:'Comedor Los Aromos', detalle:'Destino ilustrativo de esta donación.'}
  ].forEach(({p,nombre,detalle}) => {
    L.circleMarker(p, {radius:7,color:'#ffffff',weight:2,fillColor:'#a64e36',fillOpacity:1})
      .addTo(mapa).bindTooltip(nombre).bindPopup(`<strong>${nombre}</strong><br>${detalle}`);
  });
  const leyenda = L.control({position:'bottomleft'});
  leyenda.onAdd = () => {
    const div = L.DomUtil.create('div','dt-map-caption');
    div.textContent = 'Recorrido y ubicación de ejemplo';
    L.DomEvent.disableClickPropagation(div);
    return div;
  };
  leyenda.addTo(mapa);
  mapa.fitBounds(linea.getBounds(), {padding:[32,32]});
  const cardMapa = contenedor.closest('details');
  cardMapa.addEventListener('toggle', () => {
    if (cardMapa.open) requestAnimationFrame(() => mapa.invalidateSize());
  });
  new ResizeObserver(() => {
    if (contenedor.clientWidth) mapa.invalidateSize();
  }).observe(contenedor);
})();

(() => {
  const root = document.querySelector('.dt-avisos');
  if (!root) return;
  // Datos de la maqueta. Los IDs son estables entre las vistas del mismo perfil.
  const avisos = {
    donante: [
      ['traslado','Tu donación está en camino','El arroz salió del depósito hacia Comedor Los Aromos.'],
      ['asignacion','Entidad asignada','Comedor Los Aromos recibirá tu donación de alimentos.'],
      ['recepcion','Entrega confirmada','Hogar Esperanza recibió los abrigos de invierno.']
    ],
    beneficiaria: [
      ['salida','Una entrega está en camino','Ya podés consultar el seguimiento del envío de arroz.'],
      ['asignada','Nueva donación asignada','Se asignaron frazadas a tu entidad.'],
      ['confirmada','Recepción registrada','Se confirmó la recepción de los artículos de higiene.']
    ],
    admin: [
      ['vencimiento','Vencimiento superado','Revisá la donación de leche que sigue en depósito.'],
      ['ingreso','Donación pendiente de asignación','Hay abrigos disponibles para asignar a una entidad.'],
      ['camion','Camión disponible','El vehículo AB123CD está disponible para un traslado.']
    ]
  };
  const perfil = root.dataset.perfil, items = avisos[perfil] || [];
  const key = 'donatrack:demo:avisos:v1:' + perfil;
  const campana = root.querySelector('.dt-notificaciones'), panel = root.querySelector('section');
  const todas = root.querySelector('[data-todas]'), contador = root.querySelector('[data-cuenta]');
  let leidos = new Set();
  const tabs = [...panel.querySelectorAll('[role=tab]')], zona = panel.querySelector('[role=tabpanel]');
  let actual = 0, borrados = new Set();
  function cargar() {
    try {const v = JSON.parse(localStorage.getItem(key) || '[]'); leidos = new Set(Array.isArray(v) ? v : []);} catch {}
  }
  function cargarBorrados() {
    try { const v = JSON.parse(localStorage.getItem(key+':borrados') || '[]'); borrados = new Set(Array.isArray(v)?v:[]); } catch {}
  }
  function cambiar(i) {actual=i; pintar(); zona.scrollTop=0;}
  tabs.forEach((t,i)=>{
    t.onclick=()=>cambiar(i);
    t.onkeydown=e=>{const j=({ArrowRight:1-i,ArrowLeft:1-i,Home:0,End:1})[e.key]; if(j!==undefined){e.preventDefault();cambiar(j);tabs[j].focus();}};
  });
  function pintar() {
    const disponibles = items.filter(([id])=>!borrados.has(id));
    const n = disponibles.filter(([id]) => !leidos.has(id)).length;
    contador.textContent = n || ''; contador.hidden = !n;
    campana.setAttribute('aria-label', 'Notificaciones: ' + n + ' sin leer');
    todas.hidden = !n || actual===1;
    tabs.forEach((t,i)=>{t.setAttribute('aria-selected',String(i===actual)); t.tabIndex=i===actual?0:-1;t.textContent=(i?'Leídas':'Sin leer')+' ('+(i?disponibles.length-n:n)+')';});
    zona.setAttribute('aria-labelledby',tabs[actual].id);
    root.querySelector('[data-resumen]').textContent = n ? n + ' sin leer' : 'Estás al día.';
    const lista = panel.querySelector('ul'); lista.replaceChildren();
    const visibles = disponibles.filter(([id])=>leidos.has(id)===Boolean(actual));
    const vacio=panel.querySelector('[data-vacio]'); vacio.hidden=visibles.length>0; vacio.textContent=actual?'No hay notificaciones leídas.':'No tenés notificaciones sin leer.';
    for (const [id,titulo,texto] of visibles) {
      const li = document.createElement('li'), b = document.createElement('b'), p = document.createElement('span');
      b.textContent = titulo; p.textContent = texto; li.append(b,p);
      const nueva = !leidos.has(id), accion = document.createElement('button');
      if (nueva) {li.className = 'av-nueva'; accion.type = 'button'; accion.dataset.leer = id; accion.setAttribute('aria-label','Marcar como leída: '+titulo);}
      if(!nueva){accion.type='button';accion.dataset.borrar=id;accion.className='av-borrar';accion.setAttribute('aria-label','Eliminar notificación: '+titulo);accion.title='Eliminar notificación';}
      accion.textContent = nueva ? 'Marcar como leída' : '×'; li.append(accion); lista.append(li);
    }
  }
  function abrir(si) {if(si)cambiar(0);panel.hidden = !si; campana.setAttribute('aria-expanded', String(si));}
  campana.addEventListener('click', () => abrir(panel.hidden));
  panel.addEventListener('click', e => {
    const accion = e.target.closest('[data-leer],[data-todas],[data-borrar]'); if (!accion) return;
    if(accion.dataset.borrar){borrados.add(accion.dataset.borrar);try{localStorage.setItem(key+':borrados',JSON.stringify([...borrados]));}catch{}} else if (accion === todas) items.forEach(([id]) => leidos.add(id)); else leidos.add(accion.dataset.leer);
    try {localStorage.setItem(key, JSON.stringify([...leidos]));} catch {}
    pintar(); (panel.querySelector('[data-leer],[data-borrar]') || tabs[actual]).focus();
  });
  document.addEventListener('pointerdown', e => {if (!root.contains(e.target)) abrir(false);});
  document.addEventListener('focusin', e => {if (!root.contains(e.target)) abrir(false);});
  document.addEventListener('keydown', e => {if (e.key === 'Escape' && !panel.hidden) {abrir(false); campana.focus();}});
  window.addEventListener('storage', e => {if (e.key === key+':borrados' || e.key === key || e.key === null) {cargar(); cargarBorrados(); pintar();}});
  cargar(); cargarBorrados(); pintar();
})();

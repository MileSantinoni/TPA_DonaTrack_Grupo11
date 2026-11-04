/* Demostración local, sin llamadas al backend. */
(() => {
'use strict';
// Igual que Mis donaciones: foco discreto solamente al usar teclado.
document.addEventListener('keydown', event => {
  if (event.key === 'Tab') document.body.classList.add('dt-teclado');
});
document.addEventListener('pointerdown', () => document.body.classList.remove('dt-teclado'));
const $=s=>document.querySelector(s), normal=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
const avisar=t=>{$('#be-aviso').hidden=false;$('#be-aviso').textContent=t;};
let foco;
function abrir(d){foco=document.activeElement;d.showModal();}
document.querySelectorAll('.be-modal').forEach(d=>{d.querySelectorAll('[data-cerrar]').forEach(b=>b.onclick=()=>d.close());d.addEventListener('close',()=>{(foco?.isConnected?foco:$('#be-registrar')).focus();});});
const tabs=[...document.querySelectorAll('[role=tab]')];
function tab(id){tabs.forEach(t=>{const activo=t.id===id;t.setAttribute('aria-selected',activo);t.tabIndex=activo?0:-1;t.classList.toggle('dt-tab-activa',activo);document.getElementById(t.getAttribute('aria-controls')).hidden=!activo;});}
tabs.forEach((t,i)=>{t.onclick=()=>{tab(t.id);history.replaceState(null,'',i?'#necesidades':'#donaciones');};t.onkeydown=e=>{if(['ArrowLeft','ArrowRight','Home','End'].includes(e.key)){e.preventDefault();const n=e.key==='Home'?0:e.key==='End'?1:1-i;tabs[n].click();tabs[n].focus();}};});
if(location.hash==='#necesidades')tab('tab-necesidades');
$('.dt-notificaciones').onclick=()=>avisar('Las notificaciones todavía no están conectadas.');
function filtrarDonaciones(){const f=new FormData($('#be-filtro-donaciones'));let n=0;document.querySelectorAll('.dt-donacion').forEach(c=>{c.hidden=!!((f.get('estado')&&f.get('estado')!==c.dataset.estado)||!normal(c.querySelector('summary').textContent).includes(normal(f.get('texto').trim())));if(!c.hidden)n++;});$('#be-total-donaciones').textContent=n+(n===1?' donación':' donaciones');$('#be-vacio-donaciones').hidden=n>0;}
$('#be-filtro-donaciones').onsubmit=e=>{e.preventDefault();filtrarDonaciones();};
let necesidades=[
{id:1,descripcion:'Arroz para el comedor',categoria:'Alimentos',subcategoria:'Arroz',objetivo:50,cubierta:20,tipo:'Recurrente',periodicidad:'Mensual',inicio:'2026-11-01',motivo:'',vinculada:true},
{id:2,descripcion:'Frazadas para familias',categoria:'Abrigo',subcategoria:'Ropa de cama',objetivo:15,cubierta:0,tipo:'Extraordinaria',periodicidad:'Mensual',inicio:'2026-11-01',motivo:'asistencia por inundación',vinculada:false},
{id:3,descripcion:'Productos de higiene',categoria:'Higiene',subcategoria:'Jabón',objetivo:40,cubierta:40,tipo:'Recurrente',periodicidad:'Mensual',inicio:'2026-11-01',motivo:'',vinculada:true}];
let siguiente=4,editando=null,eliminando=null,filtro={tipo:'',cobertura:'',texto:''};
const unidad=n=>n.categoria==='Alimentos'?'kg':'unidades';
function periodo(n){const i=new Date(n.inicio+'T12:00:00'),f=new Date(i);if(n.periodicidad==='Mensual'){const dia=i.getDate();f.setDate(1);f.setMonth(f.getMonth()+1);f.setDate(Math.min(dia,new Date(f.getFullYear(),f.getMonth()+1,0).getDate()));f.setDate(f.getDate()-1);}else if(n.periodicidad==='Semanal')f.setDate(f.getDate()+6);return 'Período: '+i.toLocaleDateString('es-AR')+' al '+f.toLocaleDateString('es-AR');}
function elemento(tag,cl,text){const x=document.createElement(tag);if(cl)x.className=cl;if(text!==undefined)x.textContent=text;return x;}
function render(){const lista=$('#be-necesidades');lista.replaceChildren();const visibles=necesidades.filter(n=>(!filtro.tipo||n.tipo===filtro.tipo)&&(!filtro.cobertura||(filtro.cobertura==='cubierta'?n.cubierta>=n.objetivo:n.cubierta<n.objetivo))&&normal(n.descripcion).includes(normal(filtro.texto)));
visibles.forEach(n=>{const card=elemento('article','be-necesidad'),datos=elemento('div','be-datos');datos.append(elemento('h3','',n.descripcion),elemento('p','',n.categoria+' / '+n.subcategoria),elemento('p','',n.tipo==='Recurrente'?periodo(n):'Motivo: '+n.motivo));const tipo=elemento('span','be-tipo-texto',n.tipo+(n.tipo==='Recurrente'?' · '+n.periodicidad:'')),v=elemento('span','be-vinculada',n.vinculada?'Con asignaciones vinculadas':''),cob=elemento('div','be-cobertura');['Objetivo: '+n.objetivo,'Cubierta: '+n.cubierta,'Pendiente: '+Math.max(0,n.objetivo-n.cubierta)].forEach(t=>cob.append(elemento('span','',t+' '+unidad(n))));const acciones=elemento('div','be-card-acciones'),editar=elemento('button','btn btn-secondary','Editar'),borrar=elemento('button','btn be-peligro','Eliminar');editar.type=borrar.type='button';editar.setAttribute('aria-label','Editar '+n.descripcion);editar.onclick=()=>abrirNecesidad(n);borrar.disabled=n.vinculada;borrar.title=n.vinculada?'Tiene asignaciones vinculadas.':'';borrar.setAttribute('aria-label','Eliminar '+n.descripcion);borrar.onclick=()=>{eliminando=n.id;$('#be-eliminar-nombre').textContent=n.descripcion;abrir($('#be-modal-eliminar'));};acciones.append(editar,borrar);card.append(datos,tipo,v,cob,acciones);lista.append(card);});$('#be-total-necesidades').textContent=visibles.length+(visibles.length===1?' necesidad registrada':' necesidades registradas');$('#be-vacio-necesidades').hidden=visibles.length>0;}
$('#be-filtro-necesidades').onsubmit=e=>{e.preventDefault();filtro=Object.fromEntries(new FormData(e.target));filtro.texto=filtro.texto.trim();render();};
const form=$('#be-form-necesidad'),campo=n=>form.elements.namedItem(n),catalogo={Alimentos:['Arroz','Fideos'],Abrigo:['Ropa de cama','Abrigos'],Higiene:['Jabón','Limpieza']};
function categoria(sub){campo('subcategoria').replaceChildren(...catalogo[campo('categoria').value].map(s=>new Option(s,s)));if(sub)campo('subcategoria').value=sub;$('#be-unidad').textContent='Unidad: '+unidad({categoria:campo('categoria').value});}
function tipo(){const r=campo('tipo').value==='Recurrente';document.querySelectorAll('[data-recurrente]').forEach(l=>{l.hidden=!r;const i=l.querySelector('input,select');i.disabled=!r;i.required=r;});document.querySelector('[data-extraordinaria]').hidden=r;campo('motivo').disabled=r;campo('motivo').required=!r;}
campo('categoria').onchange=()=>categoria();form.querySelectorAll('[name=tipo]').forEach(i=>i.onchange=tipo);
function abrirNecesidad(n){editando=n?.id??null;form.reset();campo('descripcion').setCustomValidity('');campo('motivo').setCustomValidity('');campo('categoria').value=n?.categoria??'Alimentos';categoria(n?.subcategoria);for(const k of ['descripcion','objetivo','periodicidad','inicio','motivo'])if(n&&n[k]!==undefined)campo(k).value=n[k];campo('tipo').value=n?.tipo??'Recurrente';campo('objetivo').min=n?.cubierta>0?String(n.cubierta):'0.01';tipo();$('#be-modal-titulo').textContent=n?'Editar necesidad':'Registrar necesidad';$('#be-guardar').textContent=n?'Guardar cambios':'Registrar necesidad';abrir($('#be-modal-necesidad'));}
$('#be-registrar').onclick=()=>abrirNecesidad();
form.onsubmit=e=>{e.preventDefault();const desc=campo('descripcion');desc.setCustomValidity(desc.value.trim()?'':'Escribí una descripción.');const motivo=campo('motivo');motivo.setCustomValidity(motivo.disabled||motivo.value.trim()?'':'Escribí un motivo.');if(!form.reportValidity())return;const anterior=necesidades.find(n=>n.id===editando),n={...anterior,...Object.fromEntries(new FormData(form)),id:editando??siguiente++,objetivo:Number(campo('objetivo').value),cubierta:anterior?.cubierta??0,vinculada:anterior?.vinculada??false};n.descripcion=n.descripcion.trim();if(anterior)necesidades=necesidades.map(x=>x.id===editando?n:x);else necesidades.push(n);render();$('#be-modal-necesidad').close();avisar('Necesidad '+(anterior?'actualizada':'registrada')+' en la demostración. No se guardó en la base.');};
form.addEventListener('input',e=>{if(e.target.setCustomValidity)e.target.setCustomValidity('');});
$('#be-eliminar-confirmar').onclick=()=>{necesidades=necesidades.filter(n=>n.id!==eliminando||n.vinculada);render();$('#be-modal-eliminar').close();avisar('Necesidad eliminada de la demostración.');};render();
let fotos=[];
function renderFotos(){const v=$('#be-preview');v.replaceChildren();fotos.forEach((f,i)=>{const fig=elemento('figure'),img=elemento('img'),b=elemento('button','','×');img.src=f.url;img.alt='Vista previa de '+f.file.name;b.type='button';b.setAttribute('aria-label','Quitar '+f.file.name);b.onclick=()=>{URL.revokeObjectURL(f.url);fotos.splice(i,1);renderFotos();};fig.append(img,b,elemento('figcaption','',f.file.name));v.append(fig);});}
function agregarFotos(files){const errores=[];for(const file of files){if(fotos.length>=5){errores.push('Máximo 5 fotos.');break;}if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5*1024*1024){errores.push('Usá JPG, PNG o WebP de hasta 5 MB.');continue;}fotos.push({file,url:URL.createObjectURL(file)});}$('#be-error-fotos').textContent=[...new Set(errores)].join(' ');$('#be-error-fotos').hidden=!errores.length;renderFotos();}
$('#be-fotos').onchange=e=>{agregarFotos(e.target.files);e.target.value='';};
['dragenter','dragover','drop'].forEach(t=>$('#be-dropzone').addEventListener(t,e=>{e.preventDefault();if(t==='drop')agregarFotos(e.dataTransfer.files);}));
$('#be-recibir').onclick=()=>abrir($('#be-modal-recepcion'));
$('#be-form-recepcion').onsubmit=e=>{e.preventDefault();if(!fotos.length){$('#be-error-fotos').textContent='Adjuntá al menos una foto.';$('#be-error-fotos').hidden=false;return;}$('#be-donacion-principal').dataset.estado='entregada';const badge=$('#be-estado-principal');badge.textContent='Entregada';badge.className='badge dt-badge-entregada';const h=[...document.querySelectorAll('#be-donacion-principal .dt-timeline li')];h[2].removeAttribute('aria-current');h[2].querySelector('p').textContent='Traslado finalizado.';h[2].querySelector('.dt-hito').className='dt-hito dt-completo';h[3].classList.remove('dt-pendiente');h[3].setAttribute('aria-current','step');h[3].querySelector('.dt-hito').classList.add('dt-completo');h[3].querySelector('.dt-fecha').textContent='Recepción confirmada (ejemplo)';h[3].querySelector('p').textContent='Bienes recibidos.';$('#be-confirmacion').hidden=true;$('#be-modal-recepcion').close();filtrarDonaciones();avisar('Recepción confirmada en la demostración. Las fotos no se enviaron al servidor.');fotos.forEach(f=>URL.revokeObjectURL(f.url));fotos=[];renderFotos();};
window.addEventListener('pagehide',()=>fotos.forEach(f=>URL.revokeObjectURL(f.url)));
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
  if (window.ResizeObserver) new ResizeObserver(() => {
    if (contenedor.clientWidth) mapa.invalidateSize();
  }).observe(contenedor);
})();


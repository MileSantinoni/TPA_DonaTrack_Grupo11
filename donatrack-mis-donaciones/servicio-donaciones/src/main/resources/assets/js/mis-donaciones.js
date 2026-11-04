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
  document.querySelector('#dt-entidades').addEventListener('click', () => avisar('La pantalla de entidades beneficiarias se incorporará en el próximo paso.'));
  document.querySelector('.dt-notificaciones').addEventListener('click', () => avisar('Las notificaciones mostradas son de ejemplo. Todavía no están conectadas.'));
  // Conserva las proporciones del mapa y sus capas originales de Figma.
  document.querySelectorAll('.dt-mapa-ventana').forEach(ventana => {
    const ajustar = () => {
      if (!ventana.clientWidth) return;
      const escala = ventana.clientWidth / 730;
      ventana.querySelector('.dt-mapa').style.transform = `scale(${escala})`;
      ventana.style.height = `${314 * escala}px`;
    };
    new ResizeObserver(ajustar).observe(ventana);
    ajustar();
  });
})();

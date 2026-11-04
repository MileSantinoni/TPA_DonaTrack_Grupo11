/* Maqueta con HTML renderizado por Handlebars; sin llamadas a la API. */
(() => {
  const form = document.querySelector('#dt-buscar-entidades');
  const cards = [...document.querySelectorAll('.dt-entidad-card')];
  const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLocaleLowerCase('es');
  form.addEventListener('submit', event => {
    event.preventDefault();
    const busqueda = normalizar(new FormData(form).get('nombre') || '');
    let cantidad = 0;
    cards.forEach(card => {
      card.hidden = !normalizar(card.dataset.nombre).includes(busqueda);
      if (!card.hidden) cantidad++;
    });
    document.querySelector('#dt-entidades-vacio').hidden = cantidad > 0;
    document.querySelector('#dt-entidades-contador').textContent = `${cantidad} ${cantidad === 1 ? 'entidad encontrada' : 'entidades encontradas'}`;
  });
  document.querySelectorAll('[data-modal]').forEach(button => {
    const modal = document.getElementById(button.dataset.modal);
    button.addEventListener('click', () => {
      modal.showModal();
      document.documentElement.classList.add('dt-modal-abierto');
    });
    modal.addEventListener('close', () => {
      document.documentElement.classList.remove('dt-modal-abierto');
      button.focus({preventScroll:true});
    });
    // Escape y el botón Cerrar usan el comportamiento nativo de dialog.
    let inicioEnFondo = false;
    const fuera = event => {
      const rect = modal.getBoundingClientRect();
      return event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom;
    };
    modal.addEventListener('pointerdown', event => { inicioEnFondo = event.target === modal && fuera(event); });
    modal.addEventListener('click', event => {
      if (inicioEnFondo && event.target === modal && fuera(event)) modal.close();
      inicioEnFondo = false;
    });
  });
  document.querySelector('.dt-notificaciones').addEventListener('click', () => {
    const aviso = document.querySelector('#dt-aviso');
    aviso.hidden = false;
    aviso.textContent = 'Las notificaciones mostradas son de ejemplo. Todavía no están conectadas.';
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Tab') document.body.classList.add('dt-teclado');
  });
  document.addEventListener('pointerdown', () => document.body.classList.remove('dt-teclado'));
})();

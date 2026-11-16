(() => {
    'use strict';

    const modal = document.getElementById('eliminar-camion');
    const form = document.getElementById('form-eliminar-camion');
    const error = document.getElementById('error-eliminar-camion');

    let patente = '';
    let ocupado = false;
    let anterior;

    document.addEventListener('click', e => {
        const boton = e.target.closest('[data-eliminar-camion]');

        if (!boton || boton.disabled || ocupado) return;

        const card = boton.closest('[data-patente]');

        if (!card) return;

        patente = card.dataset.patente;
        anterior = boton;

        document.getElementById('patente-eliminar-camion')
            .textContent = patente;

        error.textContent = '';
        modal.showModal();
    });

    modal.querySelectorAll('[data-cerrar-eliminacion]')
        .forEach(b => {
            b.onclick = () => {
                if (!ocupado) {
                    modal.close();
                }
            };
        });

    modal.addEventListener('cancel', e => {
        if (ocupado) {
            e.preventDefault();
        }
    });

    modal.addEventListener('close', () => {
        anterior?.focus();
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();

        if (ocupado) return;

        ocupado = true;
        error.textContent = '';

        const botones = [...form.querySelectorAll('button')];

        botones.forEach(b => {
            b.disabled = true;
        });

        try {
            const respuesta = await fetch(
                `/administracion/camiones/${encodeURIComponent(patente)}/eliminar`,
                {
                    method: 'POST',
                    credentials: 'same-origin'
                }
            );

            const texto = await respuesta.text();

            let datos;

            try {
                datos = JSON.parse(texto);
            } catch {
                datos = null;
            }

            if (!respuesta.ok) {
                throw Error(
                    datos?.mensaje
                    || datos?.title
                    || 'No se pudo eliminar el camión.'
                );
            }

            try {
                sessionStorage.setItem(
                    'camiones-aviso',
                    datos?.mensaje || 'Camión eliminado.'
                );
            } catch {}

            location.reload();
        } catch (e) {
            error.textContent = e.message
                || 'No se pudo confirmar la eliminación. Recargá antes de reintentar.';
        } finally {
            ocupado = false;

            botones.forEach(b => {
                b.disabled = false;
            });
        }
    });
})();
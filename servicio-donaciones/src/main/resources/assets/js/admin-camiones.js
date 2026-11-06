(() => {
    'use strict';

    const $ = id => document.getElementById(id);
    const base = '/administracion/camiones';

    const modal = $('modal');
    const form = $('formulario');

    let patente = null;
    let busy = false;
    let focusBefore;

    const field = name => form.elements.namedItem(name);

    async function post(url, data) {
        const response = await fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            body: new URLSearchParams(data)
        });

        const text = await response.text();

        let result;

        try {
            result = JSON.parse(text);
        } catch {
            result = null;
        }

        if (!response.ok) {
            throw Error(
                result?.mensaje
                || result?.title
                || 'No se pudo confirmar el cambio. Recargá la lista antes de reintentar.'
            );
        }

        return result;
    }

    function done(result) {
        try {
            sessionStorage.setItem(
                'camiones-aviso',
                result.mensaje
            );
        } catch {}

        location.reload();
    }

    try {
        $('aviso').textContent =
            sessionStorage.getItem('camiones-aviso') || '';

        sessionStorage.removeItem('camiones-aviso');
    } catch {}

    function open(card) {
        if (busy) return;

        focusBefore = document.activeElement;

        form.reset();

        patente = card?.dataset.patente ?? null;
        field('patente').readOnly = patente !== null;

        if (card) {
            for (const name of [
                'patente', 'kg', 'm3', 'h', 'disponible'
            ]) {
                field(name).value = card.dataset[name];
            }
        }

        $('mt').textContent = patente === null
            ? 'Registrar camión'
            : 'Editar camión';

        $('guardar').textContent = patente === null
            ? 'Registrar camión'
            : 'Guardar cambios';

        $('error').textContent = '';

        modal.showModal();
    }

    $('nuevo-camion').onclick = () => open(null);

    $('cerrar').onclick = $('cancelar').onclick = () => {
        if (!busy) {
            modal.close();
        }
    };

    modal.addEventListener('cancel', e => {
        if (busy) {
            e.preventDefault();
        }
    });

    modal.addEventListener('close', () => {
        focusBefore?.focus();
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();

        if (busy) return;

        const data = new FormData(form);

        if (patente === null) {
            data.set(
                'patente',
                data.get('patente').trim().toUpperCase()
            );
        }

        const controls = [
            ...form.querySelectorAll('input,select,button')
        ];

        busy = true;

        controls.forEach(c => {
            c.disabled = true;
        });

        $('error').textContent = '';

        try {
            const url = patente === null
                ? base
                : `${base}/${encodeURIComponent(patente)}/editar`;

            done(await post(url, data));
        } catch (err) {
            $('error').textContent = err.message;
        } finally {
            busy = false;

            controls.forEach(c => {
                c.disabled = false;
            });
        }
    });

    document.addEventListener('click', async e => {
        const b = e.target.closest(
            '[data-editar],[data-disponibilidad]'
        );

        if (!b || b.disabled || busy) return;

        const card = b.closest('[data-patente]');

        if (b.hasAttribute('data-editar')) {
            open(card);
            return;
        }

        busy = true;
        b.disabled = true;
        $('aviso').textContent = '';

        try {
            done(await post(
                `${base}/${encodeURIComponent(card.dataset.patente)}/disponibilidad`,
                {
                    disponible: String(
                        card.dataset.disponible !== 'true'
                    )
                }
            ));
        } catch (err) {
            $('aviso').textContent = err.message;
        } finally {
            busy = false;
            b.disabled = false;
        }
    });

    document.addEventListener('keydown', e => {
        if (e.key === 'Tab') {
            document.body.classList.add('teclado');
        }
    });

    document.addEventListener('pointerdown', () => {
        document.body.classList.remove('teclado');
    });
})();
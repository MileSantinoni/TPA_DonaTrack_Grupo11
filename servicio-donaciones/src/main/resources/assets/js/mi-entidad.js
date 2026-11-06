(() => {
    'use strict';

    const $ = id => document.getElementById(id);

    let ocupado = false;
    let foco;
    let editando = null;
    let vinculada = false;
    let eliminando;
    let donacion;
    let fotos = [];

    const form = $('be-form-necesidad');
    const campo = n => form.elements.namedItem(n);

    const catalogo = [...campo('subcategoria').options].map(o => ({
        id: o.value,
        nombre: o.textContent.trim(),
        categoria: o.dataset.categoria
    }));

    if (location.hash === '#necesidades' && !location.search) {
        location.replace('/mi-entidad?vista=necesidades');
    }

    document.addEventListener('keydown', e => {
        if (e.key === 'Tab') {
            document.body.classList.add('dt-teclado');
        }
    });

    document.addEventListener('pointerdown', () => {
        document.body.classList.remove('dt-teclado');
    });

    function avisar(texto) {
        $('be-aviso').hidden = false;
        $('be-aviso').textContent = texto;
    }

    try {
        const aviso = sessionStorage.getItem('mi-entidad-aviso');

        if (aviso) avisar(aviso);

        sessionStorage.removeItem('mi-entidad-aviso');
    } catch {}

    function abrir(id) {
        foco = document.activeElement;
        $(id).showModal();
    }

    document.querySelectorAll('.be-modal').forEach(d => {
        d.querySelectorAll('[data-cerrar]').forEach(b => {
            b.onclick = () => {
                if (!ocupado) d.close();
            };
        });

        d.addEventListener('cancel', e => {
            if (ocupado) e.preventDefault();
        });

        d.addEventListener('close', () => foco?.focus());
    });

    async function request(url, options = {}) {
        const r = await fetch(url, {
            credentials: 'same-origin',
            cache: 'no-store',
            ...options
        });

        const text = await r.text();
        let data;

        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }

        if (!r.ok) {
            throw Error(
                data?.mensaje
                || data?.title
                || 'No se pudo completar la operación.'
            );
        }

        return data;
    }

    function done(data) {
        try {
            sessionStorage.setItem('mi-entidad-aviso', data.mensaje);
        } catch {}

        location.reload();
    }

    async function operacion(dialog, error, ejecutar) {
        if (ocupado) return;

        ocupado = true;
        $(error).textContent = '';

        const controles = [
            ...$(dialog).querySelectorAll('input,select,button')
        ];

        const deshabilitados = controles.map(c => c.disabled);

        controles.forEach(c => {
            c.disabled = true;
        });

        try {
            await ejecutar();
        } catch (e) {
            $(error).textContent = e.message;
        } finally {
            ocupado = false;

            controles.forEach((c, i) => {
                c.disabled = deshabilitados[i];
            });
        }
    }

    function subcategorias(id) {
        campo('subcategoria').replaceChildren(
            ...catalogo
                .filter(s => s.categoria === campo('categoria').value)
                .map(s => new Option(s.nombre, s.id))
        );

        if (id) {
            campo('subcategoria').value = id;
        }
    }

    function tipo() {
        const recurrente = campo('tipo').value === 'Recurrente';

        form.querySelectorAll('[data-recurrente]').forEach(l => {
            l.hidden = !recurrente;

            const c = l.querySelector('input,select');

            c.disabled = !recurrente || vinculada;
            c.required = recurrente;
        });

        form.querySelector('[data-extraordinaria]').hidden = recurrente;

        campo('motivo').disabled = recurrente;
        campo('motivo').required = !recurrente;
    }

    function necesidad(n = {}) {
        form.reset();

        editando = n.id || null;
        vinculada = !!n.vinculada;

        campo('categoria').replaceChildren(
            ...[...new Set(catalogo.map(s => s.categoria))]
                .map(c => new Option(c, c))
        );

        if (n.categoria) {
            campo('categoria').value = n.categoria;
        }

        subcategorias(n.subcategoria);

        for (const k of [
            'descripcion', 'objetivo', 'periodicidad', 'inicio', 'motivo'
        ]) {
            if (n[k] != null) {
                campo(k).value = n[k];
            }
        }

        campo('tipo').value = n.tipo || 'Recurrente';

        form.querySelectorAll('[name=tipo]').forEach(c => {
            c.disabled = !!editando;
        });

        campo('categoria').disabled = vinculada;
        campo('subcategoria').disabled = vinculada;

        campo('objetivo').min = String(
            Math.max(1, n.cubierta || 0)
        );

        tipo();

        $('be-modal-titulo').textContent =
            editando ? 'Editar necesidad' : 'Registrar necesidad';

        $('be-guardar').textContent =
            editando ? 'Guardar cambios' : 'Registrar necesidad';

        $('be-error-necesidad').textContent = catalogo.length
            ? ''
            : 'No hay subcategorías registradas para seleccionar.';

        abrir('be-modal-necesidad');
    }

    campo('categoria').onchange = () => subcategorias();

    form.querySelectorAll('[name=tipo]').forEach(c => {
        c.onchange = tipo;
    });

    $('be-registrar')?.addEventListener('click', () => necesidad());

    form.onsubmit = e => {
        e.preventDefault();

        const data = new FormData(form);

        for (const k of [
            'tipo', 'subcategoria', 'periodicidad', 'inicio'
        ]) {
            data.set(k, campo(k).value);
        }

        operacion(
            'be-modal-necesidad',
            'be-error-necesidad',
            async () => {
                done(await request(
                    editando
                        ? `/mi-entidad/necesidades/${editando}/editar`
                        : '/mi-entidad/necesidades',
                    {
                        method: 'POST',
                        body: new URLSearchParams(data)
                    }
                ));
            }
        );
    };

    $('be-eliminar-confirmar').onclick = () => {
        operacion(
            'be-modal-eliminar',
            'be-error-eliminar',
            async () => {
                done(await request(
                    `/mi-entidad/necesidades/${eliminando}/eliminar`,
                    {method: 'POST'}
                ));
            }
        );
    };

    function limpiarFotos() {
        fotos.forEach(f => URL.revokeObjectURL(f.url));
        fotos = [];
        renderFotos();
    }

    function renderFotos() {
        $('be-preview').replaceChildren();

        fotos.forEach((f, i) => {
            const figure = document.createElement('figure');
            const img = document.createElement('img');
            const b = document.createElement('button');

            img.src = f.url;
            img.alt = f.file.name;

            b.type = 'button';
            b.textContent = '×';
            b.setAttribute('aria-label', 'Quitar foto');

            b.onclick = () => {
                if (ocupado) return;

                URL.revokeObjectURL(f.url);
                fotos.splice(i, 1);
                renderFotos();
            };

            figure.append(img, b);
            $('be-preview').append(figure);
        });
    }

    function agregarFotos(files) {
        if (ocupado) return;

        $('be-error-fotos').textContent = '';

        for (const file of files) {
            if (fotos.length >= 5) {
                $('be-error-fotos').textContent = 'Máximo cinco fotos.';
                break;
            }

            if (
                !['image/jpeg', 'image/png'].includes(file.type)
                || file.size > 5 * 1024 * 1024
            ) {
                $('be-error-fotos').textContent =
                    'Usá JPG o PNG de hasta 5 MB.';

                continue;
            }

            fotos.push({
                file,
                url: URL.createObjectURL(file)
            });
        }

        renderFotos();
    }

    $('be-fotos').onchange = e => {
        agregarFotos(e.target.files);
        e.target.value = '';
    };

    $('be-dropzone').ondragover = e => {
        e.preventDefault();
    };

    $('be-dropzone').ondrop = e => {
        e.preventDefault();
        agregarFotos(e.dataTransfer.files);
    };

    $('be-form-recepcion').onsubmit = e => {
        e.preventDefault();

        const data = new FormData(e.target);

        fotos.forEach(f => {
            data.append('fotos', f.file);
        });

        operacion(
            'be-modal-recepcion',
            'be-error-fotos',
            async () => {
                if (!fotos.length) {
                    throw Error('Adjuntá al menos una foto.');
                }

                done(await request(
                    `/mi-entidad/donaciones/${donacion}/recepcion`,
                    {
                        method: 'POST',
                        body: data
                    }
                ));
            }
        );
    };

    document.addEventListener('click', async e => {
        const b = e.target.closest(
            '[data-editar-necesidad],'
            + '[data-eliminar-necesidad],'
            + '[data-recibir],'
            + '[data-comprobante]'
        );

        if (!b || b.disabled || ocupado) return;

        if (b.hasAttribute('data-eliminar-necesidad')) {
            eliminando = b.dataset.eliminarNecesidad;

            $('be-eliminar-nombre').textContent = b.dataset.descripcion;
            $('be-error-eliminar').textContent = '';

            abrir('be-modal-eliminar');
            return;
        }

        if (b.hasAttribute('data-recibir')) {
            donacion = b.dataset.recibir;

            limpiarFotos();
            $('be-form-recepcion').reset();

            $('be-recepcion-descripcion').textContent =
                b.dataset.descripcion;

            $('be-recepcion-id').textContent = donacion;
            $('be-error-fotos').textContent = '';

            abrir('be-modal-recepcion');
            return;
        }

        b.disabled = true;

        try {
            if (b.hasAttribute('data-editar-necesidad')) {
                necesidad(await request(
                    `/mi-entidad/necesidades/${b.dataset.editarNecesidad}`
                ));
            } else {
                const data = await request(
                    `/mi-entidad/donaciones/${b.dataset.comprobante}/seguimiento`
                );

                const container = b.parentElement.querySelector(
                    '[data-fotos-recibidas]'
                );

                container.replaceChildren();

                const prefijo =
                    `/mi-entidad/fotos/${b.dataset.comprobante}/`;

                for (const url of data.fotos || []) {
                    if (
                        typeof url !== 'string'
                        || !url.startsWith(prefijo)
                    ) {
                        continue;
                    }

                    const a = document.createElement('a');
                    const img = document.createElement('img');

                    a.href = url;
                    a.target = '_blank';
                    a.rel = 'noopener';

                    img.src = url;
                    img.alt = 'Foto de recepción';

                    a.append(img);
                    container.append(a);
                }

                if (!container.childElementCount) {
                    container.textContent =
                        'No hay fotos disponibles para esta recepción.';
                }
            }
        } catch (err) {
            avisar(err.message);
        } finally {
            b.disabled = false;
        }
    });

    window.addEventListener('pagehide', limpiarFotos);
})();
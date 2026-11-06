(() => {
    'use strict';

    const $ = id => document.getElementById(id);
    const modal = $('plan-modal');
    const form = $('plan-form');
    const inicio = $('inicio-ruta-modal');

    if (!modal || !form || !inicio) return;

    let ocupado = false;
    let refrescar = false;
    let foco;
    let ruta;
    let donaciones = [];

    const campo = nombre => form.elements.namedItem(nombre);

    async function pedir(url, datos) {
        const respuesta = await fetch(url, {
            method: datos === undefined ? 'GET' : 'POST',
            credentials: 'same-origin',
            cache: 'no-store',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            },
            body: datos === undefined
                ? undefined
                : JSON.stringify(datos)
        });

        const texto = await respuesta.text();
        let contenido;

        try {
            contenido = texto ? JSON.parse(texto) : {};
        } catch {
            contenido = {};
        }

        if (!respuesta.ok) {
            throw new Error(
                contenido.mensaje
                || contenido.title
                || 'No se pudo completar la operación.'
            );
        }

        return contenido;
    }

    function texto(padre, mensaje) {
        const p = document.createElement('p');
        p.textContent = mensaje;
        padre.append(p);
    }

    function fila(d, seleccionada) {
        const contenedor = document.createElement('article');
        contenedor.className = 'plan-fila';
        contenedor.dataset.id = d.id;

        const label = document.createElement('label');
        label.className = 'plan-eleccion';

        const check = document.createElement('input');
        check.type = 'checkbox';
        check.checked = seleccionada && d.ubicada;
        check.disabled = !d.ubicada;

        const nombre = document.createElement('span');
        nombre.textContent =
            `${d.descripcion} · ${d.cantidad} ${d.unidad}`
            + ` → ${d.entidad}`;

        label.append(check, nombre);
        contenedor.append(label);

        texto(contenedor, d.direccion || 'Sin dirección');

        if (!d.ubicada) {
            texto(
                contenedor,
                'Faltan las coordenadas de esta entidad.'
            );
        }

        const medidas = document.createElement('div');
        medidas.className = 'ac-grid';

        for (const [clave, titulo] of [
            ['pesoKg', 'Peso total (kg)'],
            ['volumenM3', 'Volumen total (m³)']
        ]) {
            const l = document.createElement('label');
            l.textContent = titulo;

            const input = document.createElement('input');
            input.type = 'number';
            input.step = 'any';
            input.min = '0.001';
            input.dataset.medida = clave;

            if (clave === 'pesoKg'
                && /^(kg|kilogramos?)$/i.test(d.unidad)) {
                input.value = d.cantidad;
            }

            l.append(input);
            medidas.append(l);
        }

        const actualizar = () => {
            medidas.querySelectorAll('input').forEach(i => {
                i.disabled = !check.checked;
                i.required = check.checked;
            });
        };

        check.addEventListener('change', actualizar);
        actualizar();

        contenedor.append(medidas);

        return contenedor;
    }

    async function abrir(id) {
        if (ocupado || modal.open || inicio.open) return;

        foco = document.activeElement;
        refrescar = false;

        form.hidden = false;
        $('plan-resultado').hidden = true;
        $('plan-error').textContent = '';

        $('plan-lista').textContent =
            'Consultando donaciones asignadas…';

        $('plan-submit').disabled = true;

        modal.showModal();
        ocupado = true;

        try {
            const datos = await pedir(
                '/administracion/planificaciones/pendientes'
            );

            donaciones = datos.donaciones;

            campo('latitud').value = datos.latitud;
            campo('longitud').value = datos.longitud;

            $('plan-lista').replaceChildren(
                ...donaciones.map(d => fila(d, d.id === id))
            );

            if (!donaciones.length) {
                texto(
                    $('plan-lista'),
                    'No hay donaciones pendientes de planificación.'
                );
            }

            $('plan-submit').disabled =
                !donaciones.some(d => d.ubicada);

        } catch (e) {
            $('plan-lista').replaceChildren();
            $('plan-error').textContent = e.message;
        } finally {
            ocupado = false;
        }
    }

    $('abrir-planificacion')?.addEventListener(
        'click',
        () => abrir()
    );

    document.querySelectorAll('[data-planificar-donacion]')
        .forEach(b => {
            b.addEventListener(
                'click',
                () => abrir(b.dataset.planificarDonacion)
            );
        });

    modal.querySelectorAll('[data-plan-cerrar]')
        .forEach(b => {
            b.addEventListener('click', () => {
                if (!ocupado) modal.close();
            });
        });

    modal.addEventListener('cancel', e => {
        if (ocupado) e.preventDefault();
    });

    modal.addEventListener('close', () => {
        if (refrescar) {
            location.reload();
        } else {
            foco?.focus();
        }
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (ocupado) return;

        const cargas = {};

        $('plan-lista').querySelectorAll('.plan-fila')
            .forEach(f => {
                if (f.querySelector('[type=checkbox]').checked) {
                    cargas[f.dataset.id] = {
                        pesoKg: Number(
                            f.querySelector(
                                '[data-medida=pesoKg]'
                            ).value
                        ),
                        volumenM3: Number(
                            f.querySelector(
                                '[data-medida=volumenM3]'
                            ).value
                        )
                    };
                }
            });

        if (!Object.keys(cargas).length
            || Object.keys(cargas).length > 50) {
            $('plan-error').textContent =
                'Seleccioná entre 1 y 50 donaciones.';
            return;
        }

        const pedido = {
            latitudDeposito: Number(campo('latitud').value),
            longitudDeposito: Number(campo('longitud').value),
            cargas
        };

        ocupado = true;
        refrescar = true;
        $('plan-error').textContent = '';

        const controles = [
            ...form.querySelectorAll('button,input')
        ].map(c => [c, c.disabled]);

        controles.forEach(([c]) => c.disabled = true);
        $('plan-submit').textContent = 'Planificando…';

        try {
            const resultado = await pedir(
                '/administracion/planificaciones',
                pedido
            );

            const resumen = $('plan-resumen');
            resumen.replaceChildren();

            texto(
                resumen,
                `Planificación para ${resultado.fecha}`
            );

            const rutas = (resultado.lotes || [])
                .flatMap(l => l.rutas || []);

            const nombre = id =>
                donaciones.find(d => d.id === id)?.descripcion
                || id;

            rutas.forEach(r => {
                texto(
                    resumen,
                    `Camión ${r.patente}: `
                    + r.entregas.map(nombre).join(', ')
                );
            });

            const problemas = [
                ['Sin ubicación', resultado.sinUbicacion],
                ['Sin datos de carga', resultado.sinDatosDeCarga],
                [
                    'Sin camión disponible',
                    resultado.sinCamionDisponible
                ],
                [
                    'Sin asignar por el planificador',
                    (resultado.lotes || [])
                        .flatMap(l => l.sinAsignar || [])
                ]
            ];

            problemas.forEach(([motivo, ids]) => {
                const elegidas = [...new Set(ids || [])]
                    .filter(id => Object.hasOwn(cargas, id));

                if (elegidas.length) {
                    texto(
                        resumen,
                        `${motivo}: `
                        + elegidas.map(nombre).join(', ')
                    );
                }
            });

            if (!rutas.length) {
                texto(
                    resumen,
                    'No se generaron rutas nuevas. '
                    + 'Consultá la disponibilidad de camiones '
                    + 'y los datos de las entregas.'
                );
            } else {
                texto(
                    resumen,
                    'Las rutas ya están disponibles. '
                    + 'El traslado se inicia desde Camiones.'
                );
            }

            form.hidden = true;
            $('plan-resultado').hidden = false;

        } catch (err) {
            $('plan-error').textContent = err.message;
        } finally {
            ocupado = false;

            controles.forEach(([c, deshabilitado]) => {
                c.disabled = deshabilitado;
            });

            $('plan-submit').textContent = 'Planificar rutas';
        }
    });

    document.querySelectorAll('[data-iniciar-ruta]')
        .forEach(b => {
            b.addEventListener('click', () => {
                if (ocupado || modal.open || inicio.open) return;

                foco = b;
                ruta = b.dataset.iniciarRuta;

                const patente = b.closest(
                    '[data-patente]'
                ).dataset.patente;

                $('inicio-ruta-texto').textContent =
                    `Camión ${patente}`
                    + ` · Fecha planificada: ${b.dataset.fecha}`;

                $('inicio-ruta-error').textContent = '';
                inicio.showModal();
            });
        });

    inicio.querySelectorAll('[data-inicio-cerrar]')
        .forEach(b => {
            b.addEventListener('click', () => {
                if (!ocupado) inicio.close();
            });
        });

    inicio.addEventListener('cancel', e => {
        if (ocupado) e.preventDefault();
    });

    inicio.addEventListener('close', () => foco?.focus());

    $('inicio-ruta-confirmar').addEventListener(
        'click',
        async () => {
            if (ocupado) return;

            ocupado = true;

            const botones = [
                ...inicio.querySelectorAll('button')
            ];

            botones.forEach(b => b.disabled = true);
            $('inicio-ruta-error').textContent = '';

            try {
                const r = await pedir(
                    `/administracion/rutas/`
                    + `${encodeURIComponent(ruta)}/iniciar`,
                    {}
                );

                try {
                    sessionStorage.setItem(
                        'camiones-aviso',
                        r.mensaje
                    );
                } catch {}

                location.reload();

            } catch (e) {
                $('inicio-ruta-error').textContent = e.message;
            } finally {
                ocupado = false;
                botones.forEach(b => b.disabled = false);
            }
        }
    );

    if ($('abrir-planificacion')) {
        try {
            const id = sessionStorage.getItem(
                'planificar-donacion'
            );

            sessionStorage.removeItem('planificar-donacion');

            if (id) abrir(id);
        } catch {}
    }
})();
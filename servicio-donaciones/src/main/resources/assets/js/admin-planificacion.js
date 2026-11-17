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
    let donacion;

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

        const contenidoTexto = await respuesta.text();
        let contenido;

        try {
            contenido = contenidoTexto
                ? JSON.parse(contenidoTexto)
                : {};
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

    function fila(d) {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'plan-fila';

        const nombre = document.createElement('strong');
        nombre.textContent =
            `${d.descripcion} · ${d.cantidad} ${d.unidad}`;

        tarjeta.append(nombre);

        texto(
            tarjeta,
            `Entidad destinataria: ${d.entidad}`
        );

        texto(
            tarjeta,
            `Dirección de entrega: ${d.direccion || 'Sin dirección'}`
        );

        const medidas = document.createElement('div');
        medidas.className = 'ac-grid';

        for (const [clave, titulo] of [
            ['pesoKg', 'Peso total (kg)'],
            ['volumenM3', 'Volumen total (m³)']
        ]) {
            const label = document.createElement('label');
            label.textContent = titulo;

            const input = document.createElement('input');
            input.name = clave;
            input.type = 'number';
            input.step = 'any';
            input.min = '0.001';
            input.required = true;
            input.disabled = !d.ubicada;

            if (clave === 'pesoKg'
                && /^(kg|kilogramos?)$/i.test(d.unidad)) {
                input.value = d.cantidad;
            }

            label.append(input);
            medidas.append(label);
        }

        tarjeta.append(medidas);

        return tarjeta;
    }

    async function abrir(id) {
        if (!id || ocupado || modal.open || inicio.open) return;

        foco = document.activeElement;
        refrescar = false;
        donacion = null;

        form.hidden = false;
        $('plan-resultado').hidden = true;
        $('plan-error').textContent = '';
        $('plan-deposito').textContent = '';

        $('plan-lista').textContent =
            'Consultando la entidad asignada…';

        $('plan-submit').disabled = true;

        modal.showModal();
        ocupado = true;

        try {
            const datos = await pedir(
                '/administracion/planificaciones/pendientes'
                + '?idDonacion=' + encodeURIComponent(id)
            );

            donacion = datos.donacion;

            $('plan-deposito').textContent =
                `Salida desde: ${datos.deposito}`;

            $('plan-lista').replaceChildren(
                fila(donacion)
            );

            $('plan-submit').disabled = !donacion.ubicada;

            if (!donacion.ubicada) {
                $('plan-error').textContent =
                    'La dirección de esta entidad todavía no tiene '
                    + 'una ubicación confirmada en el mapa. '
                    + 'Confirmala antes de planificar.';
            }

        } catch (e) {
            $('plan-lista').replaceChildren();
            $('plan-error').textContent = e.message;
        } finally {
            ocupado = false;
        }
    }

    document.querySelectorAll('[data-planificar-donacion]')
        .forEach(b => {
            b.addEventListener('click', () => {
                abrir(b.dataset.planificarDonacion);
            });
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

        if (ocupado || !donacion?.ubicada) return;

        const carga = {
            pesoKg: Number(
                form.elements.namedItem('pesoKg').value
            ),
            volumenM3: Number(
                form.elements.namedItem('volumenM3').value
            )
        };

        if (!Object.values(carga).every(
            n => Number.isFinite(n) && n > 0
        )) {
            $('plan-error').textContent =
                'Ingresá peso y volumen positivos.';
            return;
        }

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
                {
                    cargas: {
                        [donacion.id]: carga
                    }
                }
            );

            const resumen = $('plan-resumen');
            resumen.replaceChildren();

            const rutas = (resultado.lotes || [])
                .flatMap(l => l.rutas || [])
                .filter(r =>
                    r.entregas.includes(donacion.id)
                );

            const enlace = $('plan-ver-camion');

            enlace.href = '/administracion/camiones';
            enlace.textContent = 'Consultar camiones';

            if (rutas.length) {
                texto(
                    resumen,
                    `Entrega planificada para ${resultado.fecha}.`
                );

                texto(
                    resumen,
                    `Destino: ${donacion.entidad}`
                    + ` · ${donacion.direccion}`
                );

                rutas.forEach(r => {
                    texto(
                        resumen,
                        `Camión asignado: ${r.patente}`
                    );
                });

                texto(
                    resumen,
                    'La donación está lista para entregar. '
                    + 'Iniciá el traslado cuando el camión '
                    + 'salga del depósito.'
                );

                enlace.href +=
                    '?q=' + encodeURIComponent(rutas[0].patente);

                enlace.textContent =
                    'Ver camión e iniciar traslado';

            } else {
                const motivos = [
                    [
                        resultado.sinUbicacion,
                        'Falta confirmar la ubicación de la entidad.'
                    ],
                    [
                        resultado.sinDatosDeCarga,
                        'Faltan datos de carga.'
                    ],
                    [
                        resultado.sinCamionDisponible,
                        'No hay camiones disponibles '
                        + 'sin rutas pendientes.'
                    ],
                    [
                        (resultado.lotes || [])
                            .flatMap(l => l.sinAsignar || []),
                        'El planificador no pudo asignar la carga '
                        + 'a los camiones disponibles.'
                    ]
                ];

                let explicado = false;

                motivos.forEach(([ids, mensaje]) => {
                    if ((ids || []).includes(donacion.id)) {
                        texto(resumen, mensaje);
                        explicado = true;
                    }
                });

                if (!explicado) {
                    texto(
                        resumen,
                        'No se obtuvo una ruta para esta donación. '
                        + 'Recargá y revisá si ya aparece planificada.'
                    );
                }
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

            $('plan-submit').textContent =
                'Confirmar planificación';
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

    inicio.addEventListener('close', () => {
        foco?.focus();
    });

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
                    '/administracion/rutas/'
                    + encodeURIComponent(ruta)
                    + '/iniciar',
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

    // Se abre automáticamente después de asignar la entidad.
    // Ya no depende del botón general que eliminamos.
    if ($('confirmar-asignacion')) {
        try {
            const id = sessionStorage.getItem(
                'planificar-donacion'
            );

            sessionStorage.removeItem('planificar-donacion');

            if (id) abrir(id);
        } catch {}
    }
})();
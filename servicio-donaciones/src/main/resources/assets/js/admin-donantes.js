(() => {
    'use strict';

    const BASE = '/administracion/donantes';
    const $ = id => document.getElementById(id);

    const esc = value => String(value ?? '').replace(
        /[&<>"']/g,
        c => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[c])
    );

    const field = (
        label,
        name,
        value = '',
        type = 'text',
        required = true
    ) => `
    <label>
      ${label}
      <input
        name="${name}"
        type="${type}"
        value="${esc(value)}"
        ${required ? 'required' : ''}
        ${name === 'age' ? 'min="0" step="1"' : ''}>
    </label>
  `;

    const select = (label, name, values, value) => `
    <label>
      ${label}
      <select name="${name}">
        ${values.map(([v, text]) => `
          <option value="${v}" ${v === value ? 'selected' : ''}>
            ${text}
          </option>
        `).join('')}
      </select>
    </label>
  `;

    const grid = (...fields) =>
        `<div class="ac-grid">${fields.join('')}</div>`;

    const dialog = $('modal');
    const form = $('formulario');

    let submit;
    let busy = false;
    let previousFocus;

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            ...options
        });

        const text = await response.text();

        let data;

        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }

        if (!response.ok) {
            throw Error(
                data?.mensaje
                || data?.title
                || (
                    response.status < 500 && !text.trim().startsWith('<')
                        ? text
                        : 'No se pudo completar la operación. Revisá la consola del servidor.'
                )
            );
        }

        return data;
    }

    function open(title, html, label, action) {
        if (!dialog.open) {
            previousFocus = document.activeElement;
        }

        $('mt').textContent = title;
        $('campos').innerHTML = html;
        $('guardar').textContent = label;
        $('error').textContent = '';

        submit = action;

        if (!dialog.open) {
            dialog.showModal();
        }
    }

    function completed(result) {
        try {
            sessionStorage.setItem(
                'admin-donantes-aviso',
                result.mensaje
            );
        } catch {}

        location.reload();
    }

    try {
        $('aviso').textContent =
            sessionStorage.getItem('admin-donantes-aviso') || '';

        sessionStorage.removeItem('admin-donantes-aviso');
    } catch {}

    $('cerrar').onclick = $('cancelar').onclick = () => {
        if (!busy) {
            dialog.close();
        }
    };

    dialog.addEventListener('cancel', e => {
        if (busy) {
            e.preventDefault();
        }
    });

    dialog.addEventListener('close', () => {
        previousFocus?.focus();
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();

        if (busy) return;

        const data = new FormData(form);

        busy = true;

        const controls = [
            ...form.querySelectorAll('input,select,button')
        ];

        const disabled = controls.map(c => c.disabled);

        controls.forEach(c => {
            c.disabled = true;
        });

        $('error').textContent = '';

        try {
            await submit(data);
        } catch (err) {
            $('error').textContent = err.message;
        } finally {
            controls.forEach((c, i) => {
                c.disabled = disabled[i];
            });

            busy = false;
        }
    });

    function person(x = {}, type = x.type || 'HUMANA') {
        const legal = type === 'JURIDICA';

        const html =
            select(
                'Tipo de persona',
                'type',
                [
                    ['HUMANA', 'Persona humana'],
                    ['JURIDICA', 'Persona jurídica']
                ],
                type
            )
            + grid(...(
                legal
                    ? [
                        field('Razón social', 'name', x.name),
                        select(
                            'Tipo de organización',
                            'org',
                            [
                                ['EMPRESA', 'Empresa'],
                                ['ONG', 'ONG'],
                                ['GUBERNAMENTAL', 'Gubernamental'],
                                ['INSTITUCION', 'Institución']
                            ],
                            x.org || 'EMPRESA'
                        )
                    ]
                    : [
                        field('Nombre', 'first', x.first),
                        field('Apellido', 'last', x.last, 'text', false)
                    ]
            ))
            + grid(
                select(
                    'Tipo de documento',
                    'tipoDoc',
                    legal
                        ? [['CUIT', 'CUIT']]
                        : [['DNI', 'DNI'], ['CUIT', 'CUIT']],
                    x.tipoDoc
                ),
                field('Número de documento', 'doc', x.doc)
            )
            + grid(
                field('Email', 'email', x.email, 'email'),
                field('Teléfono', 'phone', x.phone, 'tel', false)
            )
            + grid(...(
                legal
                    ? [
                        field('Rubro', 'rubro', x.rubro, 'text', false),
                        field(
                            'Representante · Nombre',
                            'rep',
                            x.rep,
                            'text',
                            false
                        ),
                        field(
                            'Apellido del representante',
                            'repLast',
                            x.repLast,
                            'text',
                            false
                        ),
                        field(
                            'Email del representante',
                            'repEmail',
                            x.repEmail,
                            'email',
                            false
                        )
                    ]
                    : [
                        field('Edad', 'age', x.age ?? '', 'number'),
                        select(
                            'Género',
                            'gender',
                            [
                                ['OTRO', 'Otro / Sin especificar'],
                                ['FEMENINO', 'Femenino'],
                                ['MASCULINO', 'Masculino']
                            ],
                            x.gender || 'OTRO'
                        ),
                        field(
                            'Dirección',
                            'address',
                            x.address,
                            'text',
                            false
                        )
                    ]
            ));

        open(
            x.id ? 'Editar donante' : 'Registrar donante',
            html,
            x.id ? 'Guardar cambios' : 'Registrar donante',
            async data => {
                data.set('type', type);

                if (
                    !legal
                    && (
                        !/^\d+$/.test(data.get('age'))
                        || !Number.isSafeInteger(Number(data.get('age')))
                    )
                ) {
                    throw Error(
                        'Ingresá una edad entera mayor o igual a cero.'
                    );
                }

                const url = x.id
                    ? `${BASE}/${encodeURIComponent(x.id)}/editar`
                    : BASE;

                completed(await request(url, {
                    method: 'POST',
                    body: new URLSearchParams(data)
                }));
            }
        );

        const typeField = form.elements.namedItem('type');

        typeField.disabled = !!x.id;

        typeField.onchange = () => {
            person(
                {
                    ...x,
                    ...Object.fromEntries(new FormData(form))
                },
                typeField.value
            );
        };
    }

    function csv() {
        let file;

        open(
            'Importar personas donantes',
            `
        <p>
          Incorporá o actualizá personas donantes
          desde un archivo CSV.
        </p>

        <label class="ac-drop" id="drop">
          <svg
            aria-hidden="true"
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6">
            <path d="M12 16V3 M7 8l5-5 5 5 M4 15v5h16v-5"/>
          </svg>

          <strong>Arrastrá tu archivo CSV acá</strong>
          <small>CSV · UTF-8 · Hasta 10 MB</small>

          <span class="btn btn-secondary">
            Seleccionar archivo
          </span>

          <input
            type="file"
            id="archivo"
            accept=".csv,text/csv">

          <span id="archivoNombre" role="status"></span>
        </label>

        <small>
          Columnas: TipoPersona, TipoDoc, Documento,
          Nombre/Razón Social, Email, Teléfono.
        </small>

        <small>
          TipoPersona: HUMANA o JURIDICA.
          TipoDoc: DNI o CUIT.
          Se identifica al donante por email.
          Para los existentes se actualizan documento y teléfono;
          se conservan sus demás datos.
        </small>
      `,
            'Importar donantes',
            async () => {
                if (!file) {
                    throw Error('Seleccioná un archivo CSV.');
                }

                if (file.size > 10 * 1024 * 1024) {
                    throw Error('El máximo es 10 MB.');
                }

                const data = new FormData();
                data.append('archivo', file);

                completed(await request(BASE + '/importar', {
                    method: 'POST',
                    body: data
                }));
            }
        );

        function setFile(f) {
            if (!busy) {
                file = f;
                $('archivoNombre').textContent = f?.name || '';
            }
        }

        $('archivo').onchange = e => {
            setFile(e.target.files[0]);
        };

        $('drop').ondragover = e => {
            e.preventDefault();

            if (!busy) {
                $('drop').classList.add('drag');
            }
        };

        $('drop').ondragleave = () => {
            $('drop').classList.remove('drag');
        };

        $('drop').ondrop = e => {
            e.preventDefault();
            $('drop').classList.remove('drag');
            setFile(e.dataTransfer.files[0]);
        };
    }

    document.addEventListener('click', async e => {
        const b = e.target.closest(
            '[data-crear],[data-editar],[data-eliminar],[data-csv]'
        );

        if (!b || busy || b.disabled) return;

        if (b.hasAttribute('data-crear')) {
            person();
            return;
        }

        if (b.hasAttribute('data-csv')) {
            csv();
            return;
        }

        if (b.hasAttribute('data-eliminar')) {
            open(
                'Eliminar donante',
                `
          <p>
            ¿Querés eliminar a
            <b>${esc(b.dataset.nombre)}</b>?
          </p>
          <small>
            Solo se puede eliminar si no tiene donaciones
            ni cargas registradas.
          </small>
        `,
                'Eliminar donante',
                async () => {
                    completed(await request(
                        `${BASE}/${encodeURIComponent(b.dataset.eliminar)}/eliminar`,
                        {method: 'POST'}
                    ));
                }
            );

            return;
        }

        b.disabled = true;

        try {
            const data = await request(
                `${BASE}/${encodeURIComponent(b.dataset.editar)}`
            );

            person(data);
        } catch (err) {
            $('aviso').textContent = err.message;
        } finally {
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
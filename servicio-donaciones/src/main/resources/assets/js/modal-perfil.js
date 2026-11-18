(() => {
    const modal = document.getElementById("modal-perfil");

    if (!modal) return;

    // Escape no cierra el registro.
    modal.addEventListener("cancel", evento => {
        evento.preventDefault();
    });

    const selector = modal.querySelector("#cuenta-persona");

    if (selector) {
        const actualizar = () => {
            modal.querySelectorAll("[data-persona]").forEach(grupo => {
                const visible =
                    grupo.dataset.persona === selector.value;

                grupo.hidden = !visible;
                grupo.disabled = !visible;
            });
        };

        selector.addEventListener("change", actualizar);
        actualizar();
    }

    document.documentElement.classList.add("dt-perfil-abierto");

    const abrir = () => {
        if (!modal.open) {
            modal.showModal();
        }
    };

    modal.addEventListener("close", abrir);
    window.addEventListener("pageshow", abrir);

    abrir();
})();
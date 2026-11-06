(() => {
    const modal = document.getElementById("modal-perfil");

    if (!modal) return;

    const cerrar = () => {
        window.location.assign("/");
    };

    modal.querySelectorAll("[data-cerrar-perfil]").forEach(boton => {
        boton.addEventListener("click", cerrar);
    });

    modal.addEventListener("cancel", evento => {
        evento.preventDefault();
        cerrar();
    });

    const selector = modal.querySelector("#cuenta-persona");

    if (selector) {
        const actualizar = () => {
            modal.querySelectorAll("[data-persona]").forEach(grupo => {
                const visible = grupo.dataset.persona === selector.value;

                grupo.hidden = !visible;
                grupo.disabled = !visible;
            });
        };

        selector.addEventListener("change", actualizar);
        actualizar();
    }

    document.documentElement.classList.add("dt-perfil-abierto");

    modal.addEventListener("close", () => {
        document.documentElement.classList.remove("dt-perfil-abierto");
    });

    modal.showModal();
})();
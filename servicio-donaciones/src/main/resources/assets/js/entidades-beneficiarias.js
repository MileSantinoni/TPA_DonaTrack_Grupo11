(() => {
    document.querySelectorAll(".dt-entidad-boton[data-modal]")
        .forEach((button) => {
            const modal = document.getElementById(button.dataset.modal);

            if (!modal) {
                return;
            }

            button.addEventListener("click", () => {
                modal.showModal();
                document.documentElement.classList.add("dt-modal-abierto");
            });

            modal.addEventListener("close", () => {
                document.documentElement.classList.remove("dt-modal-abierto");
                button.focus({ preventScroll: true });
            });

            const fueraDelModal = (event) => {
                const rect = modal.getBoundingClientRect();

                return event.clientX < rect.left
                    || event.clientX > rect.right
                    || event.clientY < rect.top
                    || event.clientY > rect.bottom;
            };

            let inicioEnFondo = false;

            modal.addEventListener("pointerdown", (event) => {
                inicioEnFondo =
                    event.target === modal && fueraDelModal(event);
            });

            modal.addEventListener("click", (event) => {
                if (
                    inicioEnFondo
                    && event.target === modal
                    && fueraDelModal(event)
                ) {
                    modal.close();
                }

                inicioEnFondo = false;
            });
        });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Tab") {
            document.body.classList.add("dt-teclado");
        }
    });

    document.addEventListener("pointerdown", () => {
        document.body.classList.remove("dt-teclado");
    });
})();
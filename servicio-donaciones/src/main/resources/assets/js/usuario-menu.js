(() => {
    document.querySelectorAll(".dt-usuario").forEach((menu) => {
        if (menu.dataset.inicializado === "true") return;
        menu.dataset.inicializado = "true";

        const trigger = menu.querySelector("summary");

        menu.addEventListener("toggle", () => {
            if (!menu.open) return;

            document.querySelectorAll(".dt-usuario[open]")
                .forEach((otro) => {
                    if (otro !== menu) otro.open = false;
                });
        });

        document.addEventListener("click", (event) => {
            if (menu.open && !menu.contains(event.target)) {
                menu.open = false;
            }
        });

        menu.addEventListener("keydown", (event) => {
            if (event.key !== "Escape" || !menu.open) return;

            event.preventDefault();
            event.stopPropagation();

            menu.open = false;
            trigger.focus();
        });

        menu.addEventListener("focusout", () => {
            requestAnimationFrame(() => {
                if (!menu.contains(document.activeElement)) {
                    menu.open = false;
                }
            });
        });
    });
})();
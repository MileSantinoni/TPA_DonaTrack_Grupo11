document.querySelectorAll(".dt-contacto-campos").forEach((campos) => {
    const canal = campos.querySelector("[data-contacto-canal]");
    const telefono = campos.querySelector("[data-contacto-telefono]");

    if (!canal || !telefono) return;

    const actualizar = () => {
        telefono.required =
            telefono.dataset.obligatorio === "true" ||
            canal.value !== "MAIL";
    };

    canal.addEventListener("change", actualizar);
    actualizar();
});
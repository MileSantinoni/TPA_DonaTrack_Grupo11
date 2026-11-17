document.addEventListener("keydown", (event) => {
    if (event.key === "Tab") {
        document.body.classList.add("dt-teclado");
    }
});

document.addEventListener("pointerdown", () => {
    document.body.classList.remove("dt-teclado");
});
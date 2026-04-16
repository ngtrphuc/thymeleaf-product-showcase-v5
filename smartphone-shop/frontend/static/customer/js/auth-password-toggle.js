(() => {
  const toggles = document.querySelectorAll(".auth-pass-toggle[data-target]");

  const togglePasswordInput = (toggleEl, inputEl) => {
    const willShow = inputEl.type === "password";
    inputEl.type = willShow ? "text" : "password";
    toggleEl.classList.toggle("is-visible", willShow);
    toggleEl.setAttribute("aria-pressed", String(willShow));
    toggleEl.setAttribute("aria-label", willShow ? "Hide password" : "Show password");
  };

  toggles.forEach((toggleEl) => {
    const targetId = toggleEl.getAttribute("data-target");
    if (!targetId) {
      return;
    }

    const inputEl = document.getElementById(targetId);
    if (!inputEl) {
      return;
    }

    toggleEl.addEventListener("click", () => {
      togglePasswordInput(toggleEl, inputEl);
    });

    toggleEl.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        togglePasswordInput(toggleEl, inputEl);
      }
    });
  });
})();

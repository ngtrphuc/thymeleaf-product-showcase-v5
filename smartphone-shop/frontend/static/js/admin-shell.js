(() => {
  const storageKey = 'admin-nav-origin';
  const maxAgeMs = 1600;
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const isPlainLeftClick = (event) =>
    event.button === 0
    && !event.metaKey
    && !event.ctrlKey
    && !event.shiftKey
    && !event.altKey;

  const sameOriginPath = (href) => {
    try {
      const url = new URL(href, window.location.origin);
      return url.origin === window.location.origin ? url.pathname : null;
    } catch (_) {
      return null;
    }
  };

  const saveNavOrigin = (link) => {
    if (prefersReducedMotion) return;
    const href = link.getAttribute('href');
    const pathname = href ? sameOriginPath(href) : null;
    if (!pathname) return;

    const rect = link.getBoundingClientRect();
    const payload = {
      pathname,
      x: rect.left + (rect.width / 2),
      y: rect.top + (rect.height / 2),
      at: Date.now()
    };
    sessionStorage.setItem(storageKey, JSON.stringify(payload));
  };

  const bindSidebarNavLinks = () => {
    document.querySelectorAll('.sidebar-admin .admin-nav-link[href]').forEach((link) => {
      if (link.dataset.navZoomBound === '1') return;
      link.dataset.navZoomBound = '1';

      const maybeSave = () => {
        const pathname = sameOriginPath(link.href);
        if (!pathname || pathname === window.location.pathname) return;
        saveNavOrigin(link);
      };

      link.addEventListener('click', (event) => {
        if (!isPlainLeftClick(event)) return;
        if (link.target && link.target.toLowerCase() === '_blank') return;
        maybeSave();
      });

      link.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter' && event.key !== ' ') return;
        maybeSave();
      });
    });
  };

  const playEnterAnimation = () => {
    if (prefersReducedMotion) return;
    const content = document.querySelector('.admin-content');
    if (!content) return;

    const raw = sessionStorage.getItem(storageKey);
    if (!raw) return;
    sessionStorage.removeItem(storageKey);

    try {
      const payload = JSON.parse(raw);
      if (!payload || payload.pathname !== window.location.pathname) return;
      if (typeof payload.at !== 'number' || (Date.now() - payload.at) > maxAgeMs) return;

      const rect = content.getBoundingClientRect();
      const originX = Math.max(0, Math.min(rect.width, payload.x - rect.left));
      const originY = Math.max(0, Math.min(rect.height, payload.y - rect.top));

      content.style.setProperty('--admin-nav-origin-x', `${Math.round(originX)}px`);
      content.style.setProperty('--admin-nav-origin-y', `${Math.round(originY)}px`);
      content.classList.add('admin-content--nav-enter');
      content.addEventListener('animationend', () => {
        content.classList.remove('admin-content--nav-enter');
      }, { once: true });
    } catch (_) {
      sessionStorage.removeItem(storageKey);
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    bindSidebarNavLinks();
    playEnterAnimation();
  });
})();

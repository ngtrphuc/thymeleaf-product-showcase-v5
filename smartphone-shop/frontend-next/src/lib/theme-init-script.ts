export const THEME_INIT_SCRIPT = `
(function(){
  try {
    var path = (window.location && window.location.pathname ? window.location.pathname : '').toLowerCase();
    var scope = (path === '/admin' || path.indexOf('/admin/') === 0) ? 'admin' : 'storefront';
    var prefix = 'smartphone-shop-theme';
    var currentAccountKey = prefix + ':current-account-email';
    var currentAccount = localStorage.getItem(currentAccountKey);
    var theme = null;

    if (typeof currentAccount === 'string' && currentAccount.trim().length > 0) {
      var normalizedEmail = currentAccount.trim().toLowerCase();
      var accountGlobalKey = prefix + ':user:' + normalizedEmail;
      var accountGlobalTheme = localStorage.getItem(accountGlobalKey);
      if (accountGlobalTheme === 'light' || accountGlobalTheme === 'dark') {
        theme = accountGlobalTheme;
      } else {
        var accountScopedKey = prefix + ':' + scope + ':user:' + normalizedEmail;
        var accountScopedTheme = localStorage.getItem(accountScopedKey);
        if (accountScopedTheme === 'light' || accountScopedTheme === 'dark') {
          theme = accountScopedTheme;
        } else {
          var fallbackScope = scope === 'admin' ? 'storefront' : 'admin';
          var fallbackScopedKey = prefix + ':' + fallbackScope + ':user:' + normalizedEmail;
          var fallbackScopedTheme = localStorage.getItem(fallbackScopedKey);
          theme = (fallbackScopedTheme === 'light' || fallbackScopedTheme === 'dark') ? fallbackScopedTheme : 'light';
        }
      }
    } else {
      theme = 'light';
    }

    if (theme !== 'light' && theme !== 'dark') {
      theme = 'light';
    }

    document.documentElement.setAttribute('data-theme', theme);
  } catch (error) {
    document.documentElement.setAttribute('data-theme', 'light');
  }
})();
`;

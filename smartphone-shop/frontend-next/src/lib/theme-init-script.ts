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
      var accountScopedKey = prefix + ':' + scope + ':user:' + normalizedEmail;
      var accountTheme = localStorage.getItem(accountScopedKey);
      if (accountTheme === 'light' || accountTheme === 'dark') {
        theme = accountTheme;
      }
    }

    if (theme === null) {
      var guestScopedKey = prefix + ':' + scope + ':guest';
      var guestTheme = localStorage.getItem(guestScopedKey);
      if (guestTheme === 'light' || guestTheme === 'dark') {
        theme = guestTheme;
      }
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

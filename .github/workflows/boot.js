/* BrowOS update loader. It runs first. If you installed an update zip in Settings, the updated app replaces this page. */
(function () {
  window.__NATIVE_BUILT = 5;
  if (window.__bootDone) return;
  window.__bootDone = 1;
  try {
    var raw = localStorage.getItem('browos.update');
    if (!raw) return;
    var u = JSON.parse(raw);
    if (!u || !u.html) return;
    var sig = u.v + ':' + u.t;
    if (localStorage.getItem('browos.updTry') === sig) {
      localStorage.removeItem('browos.update');
      localStorage.removeItem('browos.updTry');
      localStorage.setItem('browos.updFailed', u.v);
      return;
    }
    localStorage.setItem('browos.updTry', sig);
    window.__BOOT_UPDATE = u.v;
    setTimeout(function () {
      if (localStorage.getItem('browos.updTry') === sig) {
        localStorage.removeItem('browos.update');
        localStorage.removeItem('browos.updTry');
        localStorage.setItem('browos.updFailed', u.v);
        location.reload();
      }
    }, 12000);
    document.open();
    document.write(u.html);
    document.close();
  } catch (e) {}
})();

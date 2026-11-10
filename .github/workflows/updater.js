/* BrowOS updater: reads the BrowOS zip you choose, checks it, and installs it as the running app. */
(function () {
  const OS = typeof window !== 'undefined' ? window.BrowOS : null;
  async function unzip(buf) {
    const dv = new DataView(buf), u8 = new Uint8Array(buf);
    let e = u8.length - 22;
    while (e >= 0 && dv.getUint32(e, true) !== 0x06054b50) e--;
    if (e < 0) throw new Error('That is not a zip file.');
    const n = dv.getUint16(e + 10, true); let p = dv.getUint32(e + 16, true); const out = {};
    for (let i = 0; i < n; i++) {
      if (dv.getUint32(p, true) !== 0x02014b50) throw new Error('The zip is damaged.');
      const method = dv.getUint16(p + 10, true), csize = dv.getUint32(p + 20, true), nlen = dv.getUint16(p + 28, true), xlen = dv.getUint16(p + 30, true), clen = dv.getUint16(p + 32, true), lho = dv.getUint32(p + 42, true);
      const name = new TextDecoder().decode(u8.subarray(p + 46, p + 46 + nlen)); p += 46 + nlen + xlen + clen;
      if (name.endsWith('/')) continue;
      const ln = dv.getUint16(lho + 26, true), lx = dv.getUint16(lho + 28, true), start = lho + 30 + ln + lx, data = u8.subarray(start, start + csize);
      if (method === 0) out[name] = data;
      else if (method === 8) out[name] = new Uint8Array(await new Response(new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'))).arrayBuffer());
    }
    return out;
  }
  function build(files) {
    const names = Object.keys(files);
    const idx = names.find(n => /(^|\/)www\/index\.html$/.test(n)) || names.find(n => /(^|\/)index\.html$/.test(n));
    if (!idx) throw new Error('This zip has no www/index.html. Choose the BrowOS zip.');
    const base = idx.slice(0, idx.length - 'index.html'.length), root = base.replace(/www\/$/, '');
    const text = n => { const f = files[base + n]; if (!f) throw new Error('The zip is missing ' + n); return new TextDecoder().decode(f); };
    let html = text('index.html');
    html = html.replace(/<script\s+src="([^"]+)"\s*><\/script>/g, (m, src) => /(^|\/)boot\.js$/.test(src) ? '' : '<script>' + text(src).replace(/<\/script/gi, '<\\/script').replace(/<!--/g, '<\\!--') + '</script>');
    html = html.replace(/<link\s+rel="stylesheet"\s+href="([^"]+)"\s*>/g, (m, href) => '<style>' + text(href) + '</style>');
    if (!/BrowOS\.start/.test(html) || !/window\.BrowOS/.test(html)) throw new Error('This zip does not look like BrowOS.');
    const v = /VERSION = '([\d.]+)'/.exec(text('js/os.js')), nf = files[root + 'android-patch/NATIVE_VERSION'];
    return { html, version: v ? v[1] : 'unknown', native: nf ? parseInt(new TextDecoder().decode(nf), 10) || 0 : 0 };
  }
  async function install(file) {
    const r = build(await unzip(await file.arrayBuffer()));
    if (r.html.length > 3500000) throw new Error('The update is too large.');
    try { localStorage.setItem('browos.update', JSON.stringify({ v: r.version, t: Date.now(), html: r.html })); } catch (e) { throw new Error('Not enough storage space for the update.'); }
    localStorage.removeItem('browos.updTry'); localStorage.removeItem('browos.updFailed');
    return { version: r.version, native: r.native };
  }
  if (OS) OS.updater = { unzip, build, install };
  if (typeof module !== 'undefined') module.exports = { unzip, build, install };
})();

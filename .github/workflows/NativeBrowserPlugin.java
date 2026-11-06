package com.browos.app;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.ByteArrayInputStream;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A real Chromium (Android WebView) window that BrowOS places inside its own windows.
@CapacitorPlugin(name = "NativeBrowser")
public class NativeBrowserPlugin extends Plugin {

  // A basic list of common advertising and tracking networks.
  private static final String[] BLOCK = {
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
    "google-analytics.com", "analytics.google.com", "adnxs.com", "taboola.com", "outbrain.com",
    "criteo.com", "criteo.net", "scorecardresearch.com", "amazon-adsystem.com", "adsrvr.org",
    "pubmatic.com", "rubiconproject.com", "openx.net", "casalemedia.com", "moatads.com", "2mdn.net",
    "advertising.com", "adform.net", "smartadserver.com", "media.net", "revcontent.com", "mgid.com",
    "zedo.com", "serving-sys.com", "yieldmo.com", "teads.tv", "sharethrough.com", "adcolony.com",
    "applovin.com", "ads.yahoo.com", "quantserve.com", "hotjar.com", "ads-twitter.com",
    "ads.linkedin.com", "ads.pinterest.com", "analytics.tiktok.com", "bidswitch.net", "contextweb.com",
    "lijit.com", "sovrn.com", "indexww.com", "33across.com", "id5-sync.com", "liadm.com", "bluekai.com",
    "krxd.net", "demdex.net", "everesttech.net", "adtechus.com", "tapad.com", "rlcdn.com", "exelator.com",
    "agkn.com", "crwdcntrl.net", "adsymptotic.com", "popads.net", "propellerads.com", "exoclick.com"
  };

  private final Map<String, WebView> views = new HashMap<>();
  private final Map<String, Integer> blocked = new ConcurrentHashMap<>();
  private boolean desktop = true;
  private boolean adblock = true;
  private String lang = "en";
  private final Map<String, Boolean> deskOverride = new HashMap<>();
  private String baseUa = null;

  // Watches password fields so BrowOS can offer to save the login in Vault.
  private static final String CRED_JS =
    "(function(){if(window.__browosCred)return;window.__browosCred=1;"
    + "function st(){try{return window.sessionStorage}catch(e){return null}}"
    + "function isText(i){var t=(i.type||'text').toLowerCase();return t==='text'||t==='email'||t==='tel'||t==='username';}"
    + "document.addEventListener('input',function(e){var t=e.target;if(t&&t.tagName==='INPUT'&&isText(t)&&t.value){var s=st();if(s)s.setItem('__bu',t.value);}},true);"
    + "var last='';"
    + "function send(pw){try{var user='';var all=document.querySelectorAll('input');"
    + "for(var i=0;i<all.length;i++){var c=all[i];if(c!==pw&&isText(c)&&c.value&&(c.compareDocumentPosition(pw)&4)){user=c.value;}}"
    + "if(!user){var s=st();if(s)user=s.getItem('__bu')||'';}"
    + "var key=location.href+'|'+user+'|'+pw.value;if(key===last)return;last=key;BrowOSVault.cred(location.href,user,pw.value);}catch(e){}}"
    + "function check(){var p=document.querySelectorAll('input[type=password]');for(var i=0;i<p.length;i++){if(p[i].value){send(p[i]);return;}}}"
    + "document.addEventListener('submit',function(){check();},true);"
    + "document.addEventListener('click',function(e){var b=e.target&&e.target.closest?e.target.closest('button,input[type=submit],input[type=button],[role=button]'):null;"
    + "if(b&&(b.type==='submit'||(b.tagName==='BUTTON'&&b.type!=='button')||/sign|log|next|continue|submit/i.test(b.innerText||b.value||'')))check();},true);"
    + "document.addEventListener('keydown',function(e){if(e.key==='Enter')check();},true);"
    + "})();";

  private class CredBridge {
    private final String id;

    CredBridge(String id) {
      this.id = id;
    }

    @JavascriptInterface
    public void cred(String url, String user, String pass) {
      JSObject o = new JSObject();
      o.put("id", id);
      o.put("url", url);
      o.put("user", user);
      o.put("pass", pass);
      notifyListeners("credential", o);
    }
  }

  private ViewGroup content() {
    return (ViewGroup) getActivity().findViewById(android.R.id.content);
  }

  private boolean isBlocked(String host) {
    if (host == null) return false;
    for (String d : BLOCK) {
      if (host.equals(d) || host.endsWith("." + d)) return true;
    }
    return false;
  }

  private String mobileUa() {
    return baseUa.replace("; wv", "");
  }

  private String desktopUa() {
    Matcher m = Pattern.compile("Chrome/([\\d.]+)").matcher(baseUa);
    String v = m.find() ? m.group(1) : "126.0.0.0";
    return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + v + " Safari/537.36";
  }

  private void applyMode(WebView w, String id) {
    WebSettings s = w.getSettings();
    boolean d = deskOverride.containsKey(id) ? deskOverride.get(id) : desktop;
    s.setUserAgentString(d ? desktopUa() : mobileUa());
    s.setUseWideViewPort(true);
    s.setLoadWithOverviewMode(true);
  }

  private void emit(String id, WebView w, boolean loading, int progress) {
    JSObject o = new JSObject();
    o.put("id", id);
    o.put("url", w.getUrl() == null ? "" : w.getUrl());
    o.put("title", w.getTitle() == null ? "" : w.getTitle());
    o.put("canGoBack", w.canGoBack());
    o.put("canGoForward", w.canGoForward());
    o.put("loading", loading);
    o.put("progress", progress);
    Integer b = blocked.get(id);
    o.put("blocked", b == null ? 0 : b);
    notifyListeners("state", o);
  }

  @SuppressLint("SetJavaScriptEnabled")
  private WebView create(final String id) {
    final WebView w = new WebView(getActivity());
    if (baseUa == null) baseUa = w.getSettings().getUserAgentString();
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setSupportZoom(true);
    s.setBuiltInZoomControls(true);
    s.setDisplayZoomControls(false);
    applyMode(w, id);
    w.setBackgroundColor(0xFFFFFFFF);
    w.addJavascriptInterface(new CredBridge(id), "BrowOSVault");
    CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
    w.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        String u = r.getUrl().toString();
        return !(u.startsWith("http://") || u.startsWith("https://") || u.startsWith("about:"));
      }

      @Override
      public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        if (adblock && isBlocked(r.getUrl().getHost())) {
          Integer n = blocked.get(id);
          blocked.put(id, n == null ? 1 : n + 1);
          return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
        }
        return super.shouldInterceptRequest(v, r);
      }

      @Override
      public void onPageStarted(WebView v, String url, Bitmap favicon) {
        blocked.put(id, 0);
        emit(id, v, true, 0);
      }

      @Override
      public void onPageFinished(WebView v, String url) {
        v.evaluateJavascript(CRED_JS, null);
        emit(id, v, false, 100);
      }

      @Override
      public void doUpdateVisitedHistory(WebView v, String url, boolean isReload) {
        if (v.getProgress() >= 100) v.evaluateJavascript(CRED_JS, null);
        emit(id, v, v.getProgress() < 100, v.getProgress());
      }
    });
    w.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView v, int p) {
        emit(id, v, p < 100, p);
      }

      @Override
      public void onReceivedTitle(WebView v, String t) {
        emit(id, v, v.getProgress() < 100, v.getProgress());
      }
    });
    return w;
  }

  private void applyBounds(WebView w, PluginCall call) {
    int x = call.getInt("x", 0);
    int y = call.getInt("y", 0);
    int wd = call.getInt("w", 1);
    int ht = call.getInt("h", 1);
    boolean vis = call.getBoolean("visible", true);
    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(Math.max(1, wd), Math.max(1, ht));
    p.leftMargin = x;
    p.topMargin = y;
    w.setLayoutParams(p);
    w.setVisibility(vis ? View.VISIBLE : View.INVISIBLE);
  }

  private void load(WebView w, String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept-Language", lang.equals("en") ? "en-US,en;q=0.9" : lang + ",en;q=0.7");
    w.loadUrl(url, headers);
  }

  @PluginMethod
  public void configure(final PluginCall call) {
    final boolean d = call.getBoolean("desktop", true);
    final boolean a = call.getBoolean("adblock", true);
    final String lg = call.getString("lang", "en");
    getActivity().runOnUiThread(() -> {
      boolean changed = d != desktop;
      desktop = d;
      adblock = a;
      lang = lg;
      if (changed && baseUa != null) {
        for (Map.Entry<String, WebView> e : views.entrySet()) {
          if (deskOverride.containsKey(e.getKey())) continue;
          applyMode(e.getValue(), e.getKey());
          e.getValue().reload();
        }
      }
      call.resolve();
    });
  }

  @PluginMethod
  public void open(final PluginCall call) {
    final String id = call.getString("id");
    final String url = call.getString("url");
    if (id == null || url == null) {
      call.reject("id and url are needed");
      return;
    }
    if (call.getData().has("desktop")) deskOverride.put(id, call.getBoolean("desktop", true));
    getActivity().runOnUiThread(() -> {
      try {
        WebView w = views.get(id);
        if (w == null) {
          w = create(id);
          views.put(id, w);
          content().addView(w, new FrameLayout.LayoutParams(1, 1));
        }
        applyBounds(w, call);
        load(w, url);
        call.resolve();
      } catch (Throwable t) {
        call.reject(String.valueOf(t.getMessage()));
      }
    });
  }

  @PluginMethod
  public void setDesktop(final PluginCall call) {
    final String id = call.getString("id");
    final boolean d = call.getBoolean("desktop", true);
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null) {
        deskOverride.put(id, d);
        applyMode(w, id);
        w.reload();
      }
      call.resolve();
    });
  }

  @PluginMethod
  public void fill(final PluginCall call) {
    final String id = call.getString("id");
    final String u = call.getString("user", "");
    final String p = call.getString("pass", "");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null) {
        String js = "(function(u,p){function setv(el,v){var d=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');d.set.call(el,v);"
          + "el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));}"
          + "var pw=document.querySelector('input[type=password]');"
          + "var inputs=[].slice.call(document.querySelectorAll('input[type=text],input[type=email],input[type=tel],input:not([type])')).filter(function(e){return e.offsetParent!==null;});"
          + "var user=null;if(pw){for(var i=0;i<inputs.length;i++){if(inputs[i].compareDocumentPosition(pw)&4)user=inputs[i];}}else if(inputs.length){user=inputs[0];}"
          + "if(user&&u)setv(user,u);if(pw&&p)setv(pw,p);})(" + JSONObject.quote(u) + "," + JSONObject.quote(p) + ");";
        w.evaluateJavascript(js, null);
      }
      call.resolve();
    });
  }

  @PluginMethod
  public void setBounds(final PluginCall call) {
    final String id = call.getString("id");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null) applyBounds(w, call);
      call.resolve();
    });
  }

  @PluginMethod
  public void load(final PluginCall call) {
    final String id = call.getString("id");
    final String url = call.getString("url");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null && url != null) load(w, url);
      call.resolve();
    });
  }

  @PluginMethod
  public void back(final PluginCall call) {
    final String id = call.getString("id");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null && w.canGoBack()) w.goBack();
      call.resolve();
    });
  }

  @PluginMethod
  public void forward(final PluginCall call) {
    final String id = call.getString("id");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null && w.canGoForward()) w.goForward();
      call.resolve();
    });
  }

  @PluginMethod
  public void reload(final PluginCall call) {
    final String id = call.getString("id");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.get(id);
      if (w != null) w.reload();
      call.resolve();
    });
  }

  @PluginMethod
  public void close(final PluginCall call) {
    final String id = call.getString("id");
    getActivity().runOnUiThread(() -> {
      WebView w = id == null ? null : views.remove(id);
      if (w != null) {
        content().removeView(w);
        w.stopLoading();
        w.destroy();
      }
      if (id != null) {
        blocked.remove(id);
        deskOverride.remove(id);
      }
      call.resolve();
    });
  }

  @PluginMethod
  public void clearData(final PluginCall call) {
    getActivity().runOnUiThread(() -> {
      CookieManager.getInstance().removeAllCookies(null);
      CookieManager.getInstance().flush();
      WebStorage.getInstance().deleteAllData();
      for (WebView w : views.values()) {
        w.clearCache(true);
        w.clearHistory();
      }
      call.resolve();
    });
  }

  @Override
  protected void handleOnDestroy() {
    for (WebView w : views.values()) w.destroy();
    views.clear();
  }
}

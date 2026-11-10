package com.browos.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

// Optional browser extras: fullscreen video and file downloads. If this file does not compile, the build drops it.
public class Browsing {
  private static View customView;
  private static WebChromeClient.CustomViewCallback customCb;
  private static ViewGroup customHost;

  public static WebChromeClient wrap(final WebChromeClient base, final Activity act, final ViewGroup content, final WebView w) {
    return new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView v, int p) {
        base.onProgressChanged(v, p);
      }

      @Override
      public void onReceivedTitle(WebView v, String t) {
        base.onReceivedTitle(v, t);
      }

      @Override
      public void onPermissionRequest(android.webkit.PermissionRequest r) {
        base.onPermissionRequest(r);
      }

      @Override
      public void onShowCustomView(View view, CustomViewCallback cb) {
        if (customView != null) {
          cb.onCustomViewHidden();
          return;
        }
        customView = view;
        customCb = cb;
        customHost = content;
        content.addView(view, new FrameLayout.LayoutParams(-1, -1));
      }

      @Override
      public void onHideCustomView() {
        exitCustom();
      }
    };
  }

  public static boolean exitCustom() {
    if (customView == null) return false;
    try {
      customHost.removeView(customView);
    } catch (Throwable ignored) {
    }
    try {
      if (customCb != null) customCb.onCustomViewHidden();
    } catch (Throwable ignored) {
    }
    customView = null;
    customCb = null;
    customHost = null;
    return true;
  }

  public static void downloads(final WebView w, final Activity act) {
    w.setDownloadListener((url, ua, cd, mime, len) -> {
      try {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
          Toast.makeText(act, "This kind of download is not supported yet.", Toast.LENGTH_SHORT).show();
          return;
        }
        String name = URLUtil.guessFileName(url, cd, mime);
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
        if (mime != null) r.setMimeType(mime);
        String cookie = CookieManager.getInstance().getCookie(url);
        if (cookie != null) r.addRequestHeader("cookie", cookie);
        if (ua != null) r.addRequestHeader("User-Agent", ua);
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
        ((DownloadManager) act.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
        Toast.makeText(act, "Downloading " + name, Toast.LENGTH_SHORT).show();
      } catch (Throwable t) {
        try {
          Toast.makeText(act, "Download failed.", Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
      }
    });
  }
}

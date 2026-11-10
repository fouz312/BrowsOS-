package com.browos.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

// Keeps BrowOS (and the web apps inside it) running while you use other apps.
public class KeepAliveService extends Service {

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    try {
      String ch = "browos_keepalive";
      if (Build.VERSION.SDK_INT >= 26) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(ch, "BrowOS running", NotificationManager.IMPORTANCE_LOW));
      }
      Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
      Notification n = b.setContentTitle("BrowOS is running")
          .setContentText("Your web apps keep running. Open BrowOS to manage them.")
          .setSmallIcon(getApplicationInfo().icon)
          .setOngoing(true)
          .build();
      if (Build.VERSION.SDK_INT >= 34) {
        startForeground(7701, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
      } else {
        startForeground(7701, n);
      }
    } catch (Throwable t) {
      stopSelf();
    }
    return START_STICKY;
  }
}

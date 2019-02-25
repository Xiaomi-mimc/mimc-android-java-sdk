package com.xiaomi.mimcdemo.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.ui.VoiceCallActivity;

public class CallService extends Service {
    private static final String TAG = "CallService";
    private CallBinder callBinder = new CallBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return callBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("foregroundService", "前台服务", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, "foregroundService");
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        Intent i = new Intent(this, VoiceCallActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        builder.setContentTitle(callBinder.username)
                .setContentText("语音通话中...")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi);
        startForeground(5566, builder.build());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);
    }

    public class CallBinder extends Binder {
        private String username;

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, CallService.class);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, CallService.class);
        context.stopService(intent);
    }
}
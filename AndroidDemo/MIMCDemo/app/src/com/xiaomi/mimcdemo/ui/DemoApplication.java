package com.xiaomi.mimcdemo.ui;


import android.app.Application;
import android.content.Context;


public class DemoApplication extends Application {
    private static Context context;
    private static final String TAG = "com.xiaomi.mimcdemo";

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}

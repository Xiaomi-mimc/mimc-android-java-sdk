package com.xiaomi.mimcdemo.ui;


import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.xiaomi.msg.logger.Logger;
import com.xiaomi.msg.logger.MIMCLog;


public class DemoApplication extends Application {
    private static Context context;
    private static final String TAG = "com.xiaomi.mimcdemo";

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        MIMCLog.setLogger(new Logger() {
            @Override
            public void d(String s, String s1) {
                Log.d(s, s1);
            }

            @Override
            public void d(String s, String s1, Throwable throwable) {
                Log.d(s, s1, throwable);
            }

            @Override
            public void i(String s, String s1) {
                Log.i(s, s1);
            }

            @Override
            public void i(String s, String s1, Throwable throwable) {
                Log.i(s, s1, throwable);
            }

            @Override
            public void w(String s, String s1) {
                Log.w(s, s1);
            }

            @Override
            public void w(String s, String s1, Throwable throwable) {
                Log.w(s, s1, throwable);
            }

            @Override
            public void e(String s, String s1) {
                Log.e(s, s1);
            }

            @Override
            public void e(String s, String s1, Throwable throwable) {
                Log.e(s, s1, throwable);
            }
        });
        // default
//        MIMCLog.setLogPrintLevel(MIMCLog.INFO);
//        MIMCLog.setLogSaveLevel(MIMCLog.INFO);
//        MIMCLog.enableLog2File(true);
    }

    public static Context getContext() {
        return context;
    }
}

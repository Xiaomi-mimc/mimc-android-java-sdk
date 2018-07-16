package com.xiaomi.mimcdemo.common;

import com.xiaomi.mimcdemo.ui.DemoApplication;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * Created by houminjiang on 18-7-13.
 */

public class Log4JConfigure {
    public static final String DEFAULT_LOG_FILE = "logs/mimc.log";

    public static void configure() {
        final LogConfigurator logConfigurator = new LogConfigurator();
        // 日志在APK安装目录下的files里，建议存放位置
        logConfigurator.setFileName(DemoApplication.getContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + DEFAULT_LOG_FILE);
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setLogCatPattern("%m%n");
        logConfigurator.setMaxFileSize(27 * 1024 * 1024);
        logConfigurator.setMaxBackupSize(27);
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();
    }
}

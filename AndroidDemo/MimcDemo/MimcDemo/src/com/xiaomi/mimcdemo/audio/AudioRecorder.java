package com.xiaomi.mimcdemo.audio;

import android.media.AudioRecord;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.xiaomi.mimcdemo.listener.OnAudioCapturedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by houminjiang on 18-5-28.
 */

public class AudioRecorder implements Capture {
    private AudioCapture audioCapture;
    private Thread captureThread;
    private volatile boolean isLoopExit = false;
    private boolean isCaptureStarted = false;
    private OnAudioCapturedListener onAudioCapturedListener;
    private final Logger logger = LoggerFactory.getLogger(AudioRecorder.class);

    public void setOnAudioCapturedListener(OnAudioCapturedListener onAudioCapturedListener) {
        this.onAudioCapturedListener = onAudioCapturedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        if (isCaptureStarted) {
            logger.info("Capture has already been started.");
            return false;
        }

        isLoopExit = false;
        audioCapture = new AudioCapture();
        boolean result = audioCapture.start();
        if (result) {
            captureThread = new Thread(new AudioCaptureRunnable());
            captureThread.start();
            isCaptureStarted = true;
        }

        return result;
    }

    @Override
    public void stop() {
        if (!isCaptureStarted) {
            return;
        }
        isLoopExit = true;
        //captureThread.interrupt();
        try {
            captureThread.join(50);
        } catch (InterruptedException e) {
            logger.warn("Interrupted exception:" + e);
        }
        audioCapture.stop();
        isCaptureStarted = false;
        cnt = 1;
    }

    private static long cnt = 1;
    private class AudioCaptureRunnable implements Runnable {
        @Override
        public void run() {
            while (!isLoopExit) {
                byte[] buffer = new byte[audioCapture.getMinBufferSize()];
                int result = audioCapture.capture(buffer, 0, audioCapture.getMinBufferSize());
                if (result == AudioRecord.ERROR_INVALID_OPERATION) {
                    logger.warn(" The object isn't properly initialized.");
                } else if (result == AudioRecord.ERROR_BAD_VALUE) {
                    logger.warn("The parameters don't resolve to valid data and indexes.");
                } else if (result == AudioRecord.ERROR_DEAD_OBJECT) {
                    logger.warn("The object is not valid anymore and needs to be recreated.");
                } else if (result == AudioRecord.ERROR) {
                    logger.warn("Other error.");
                } else {
                    if (onAudioCapturedListener != null) {
                        onAudioCapturedListener.onAudioCaptured(buffer);
                    }
                    logger.info("Success captured " + result + "bytes. cnt:" + cnt++);
                }
            }
            logger.info("Audio capture thread exit.");
        }
    }
}

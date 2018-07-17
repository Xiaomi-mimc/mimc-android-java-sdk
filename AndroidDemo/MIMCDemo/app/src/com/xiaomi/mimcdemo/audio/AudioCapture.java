package com.xiaomi.mimcdemo.audio;

import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_AUDIO_FORMAT;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_AUDIO_RECORD_SOURCE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_CHANNEL_CONFIG;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_SAMPLE_RATE;

/**
 * Created by houminjiang on 18-5-28.
 */

public class AudioCapture implements Capture {
    private AudioRecord audioRecord;
    private int minBufferSize;
    private static int MAX_CAPTURE_BUFFER_SIZE = 10 * 1024;
    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private AutomaticGainControl automaticGainControl;
    private final Logger logger = LoggerFactory.getLogger(AudioCapture.class);

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startCapture(DEFAULT_AUDIO_RECORD_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }

    @Override
    public void stop() {
        stopCapture();
        stopAEC();
        stopNS();
        stopAGC();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            logger.warn("Invalid parameters.");
            return false;
        }
        if (minBufferSize < MAX_CAPTURE_BUFFER_SIZE) {
            minBufferSize = MAX_CAPTURE_BUFFER_SIZE;
        }
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, minBufferSize);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            logger.warn("AudioRecord initialize fail.");
            return false;
        }
        if (!startAEC(audioRecord.getAudioSessionId())) {
            logger.info("Start aec fail.");
        } else {
            logger.info("Start aec success.");
        }
        if (!startNS(audioRecord.getAudioSessionId())) {
            logger.info("Start ns fail.");
        } else {
            logger.info("Start ns success.");
        }
        if (!startAGC(audioRecord.getAudioSessionId())) {
            logger.info("Start agc fail.");
        } else {
            logger.info("Start agc success.");
        }
        audioRecord.startRecording();

        logger.info("Start audio capture success.");
        return true;
    }

    private void stopCapture() {
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
        audioRecord = null;

        logger.info("Stop audio capture success.");
    }

    public int capture(byte[] data, int offset, int size) {
        return audioRecord.read(data, offset, size);
    }

    public int getMinBufferSize() {
        return minBufferSize;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startAEC(int audioSessionId) {
        if (!AcousticEchoCanceler.isAvailable()) {
            logger.warn("This device does not support AEC.");
            return false;
        }

        echoCanceler = AcousticEchoCanceler.create(audioSessionId);
        if (echoCanceler == null) {
            logger.warn("This device does not implement AEC.");
            return false;
        }
        echoCanceler.setEnabled(true);

        return echoCanceler.getEnabled();
    }

    private void stopAEC() {
        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startNS(int audioSessionId) {
        if (!NoiseSuppressor.isAvailable()) {
            logger.warn("This device does support NS.");
            return false;
        }

        noiseSuppressor = NoiseSuppressor.create(audioSessionId);
        if (noiseSuppressor == null) {
            logger.warn("This device does not implement NS.");
            return false;
        }
        noiseSuppressor.setEnabled(true);

        return noiseSuppressor.getEnabled();
    }

    private void stopNS() {
        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startAGC(int audioSessionId) {
        if (!automaticGainControl.isAvailable()) {
            logger.warn("This device does support AGC.");
            return false;
        }

        automaticGainControl = AutomaticGainControl.create(audioSessionId);
        if (automaticGainControl == null) {
            logger.warn("This device does not implement AGC.");
            return false;
        }
        automaticGainControl.setEnabled(true);

        return automaticGainControl.getEnabled();
    }

    private void stopAGC() {
        if (automaticGainControl != null) {
            automaticGainControl.release();
            automaticGainControl = null;
        }
    }
}

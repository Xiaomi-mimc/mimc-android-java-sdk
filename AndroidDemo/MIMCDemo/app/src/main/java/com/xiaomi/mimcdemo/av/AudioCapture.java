package com.xiaomi.mimcdemo.av;

import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import static com.xiaomi.mimcdemo.common.Constant.*;

/**
 * Created by houminjiang on 18-5-28.
 */

public class AudioCapture implements Capture {
    private AudioRecord audioRecord;
    private int minBufferSize;
    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private AutomaticGainControl automaticGainControl;
    private static final String TAG = "AudioCapture";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startCapture(DEFAULT_AUDIO_RECORD_SOURCE, DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
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
        minBufferSize = 2 * AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.w(TAG, "Invalid parameters.");
            return false;
        }

        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, minBufferSize);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.w(TAG, "AudioRecord initialize fail.");
            return false;
        }
        if (!startAEC(audioRecord.getAudioSessionId())) {
            Log.i(TAG, "Start aec fail.");
        } else {
            Log.i(TAG, "Start aec success.");
        }
        if (!startNS(audioRecord.getAudioSessionId())) {
            Log.i(TAG, "Start ns fail.");
        } else {
            Log.i(TAG, "Start ns success.");
        }
        if (!startAGC(audioRecord.getAudioSessionId())) {
            Log.i(TAG, "Start agc fail.");
        } else {
            Log.i(TAG, "Start agc success.");
        }
        audioRecord.startRecording();

        Log.i(TAG, "Start audio capture success.");
        return true;
    }

    private void stopCapture() {
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
        audioRecord = null;

        Log.i(TAG, "Stop audio capture success.");
    }

    public int capture(byte[] data, int offset, int size) {
        // read blocking
        return audioRecord.read(data, offset, size);
    }

    public int getMinBufferSize() {
        return minBufferSize;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startAEC(int audioSessionId) {
        if (!AcousticEchoCanceler.isAvailable()) {
            Log.w(TAG, "This device does not support AEC.");
            return false;
        }

        echoCanceler = AcousticEchoCanceler.create(audioSessionId);
        if (echoCanceler == null) {
            Log.w(TAG, "This device does not implement AEC.");
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
            Log.w(TAG, "This device does support NS.");
            return false;
        }

        noiseSuppressor = NoiseSuppressor.create(audioSessionId);
        if (noiseSuppressor == null) {
            Log.w(TAG, "This device does not implement NS.");
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
            Log.w(TAG, "This device does not support AGC.");
            return false;
        }

        automaticGainControl = AutomaticGainControl.create(audioSessionId);
        if (automaticGainControl == null) {
            Log.w(TAG, "This device does not implement AGC.");
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

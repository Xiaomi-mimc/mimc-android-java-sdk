package com.xiaomi.mimcdemo.audio;


import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.xiaomi.mimcdemo.bean.AudioData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static android.media.AudioTrack.ERROR;
import static android.media.AudioTrack.ERROR_BAD_VALUE;
import static android.media.AudioTrack.ERROR_DEAD_OBJECT;
import static android.media.AudioTrack.ERROR_INVALID_OPERATION;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_AUDIO_FORMAT;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_PLAY_CHANNEL_CONFIG;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_PLAY_MODE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_PLAY_STREAM_TYPE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_SAMPLE_RATE;

/**
 * Created by houminjiang on 18-6-6.
 */

public class AudioPlayer implements Player {
    private int minBufferSize = 0;
    private AudioTrack audioTrack;
    private boolean isPlayStarted = false;
    private static int MAX_PLAY_BUFFER_SIZE = 10 * 1024;
    private final Logger logger = LoggerFactory.getLogger(AudioPlayer.class);


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean start() {
        return startPlayer(DEFAULT_PLAY_STREAM_TYPE, DEFAULT_SAMPLE_RATE, DEFAULT_PLAY_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }

    @Override
    public void stop() {
        stopPlayer();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat) {
        if (isPlayStarted) {
            logger.warn("Audio player started.");
            return false;
        }

        minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            logger.warn("Invalid parameters.");
            return false;
        }
        if (minBufferSize < MAX_PLAY_BUFFER_SIZE) {
            minBufferSize = MAX_PLAY_BUFFER_SIZE;
        }
        audioTrack = new AudioTrack((new AudioAttributes.Builder())
            .setLegacyStreamType(streamType)
            .build(),
            (new AudioFormat.Builder())
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .setSampleRate(sampleRateInHz)
                .build(),
            minBufferSize,
            DEFAULT_PLAY_MODE, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            logger.warn("AudioTrack initialize fail.");
            return false;
        }

        isPlayStarted = true;
        logger.info("Start audio player success.");

        return true;
    }

    private void stopPlayer() {
        if (!isPlayStarted) {
            return;
        }

        if (audioTrack.getState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop();
        }
        audioTrack.release();
        isPlayStarted = false;
        logger.info("Stop audio player success.");
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (!isPlayStarted) {
            logger.warn("Audio player not started.");
            return false;
        }

        // TODO: 18-6-7 同步异常，关了，还继续播放问题
        int result = audioTrack.write(audioData, offsetInBytes, sizeInBytes);
        if (result == ERROR_INVALID_OPERATION) {
            logger.warn("The track isn't properly initialized.");
        } else if (result == ERROR_BAD_VALUE) {
            logger.warn("The parameters don't resolve to valid data and indexes.");
        } else if (result == ERROR_DEAD_OBJECT) {
            logger.warn("The AudioTrack is not valid anymore and needs to be recreated.");
        } else if (result == ERROR) {
            logger.warn("Other error.");
        }
        audioTrack.play();
        logger.info("Played:" + result + " bytes.");

        return true;
    }
}

package com.xiaomi.mimcdemo.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_BIT_RATE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_CHANNEL_COUNT;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_MAX_BUFFER_SIZE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_SAMPLE_RATE;

/**
 * Created by houminjiang on 18-6-14.
 */

public class AudioEncoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isEncoderStarted = false;
    private OnAudioEncodedListener onAudioEncodedListener;
    private MediaCodec.BufferInfo bufferInfo;
    private long encodedIndex = 0;
    private final Logger logger = LoggerFactory.getLogger(AudioEncoder.class);

    public void setOnAudioEncodedListener(OnAudioEncodedListener onAudioEncodedListener) {
        this.onAudioEncodedListener = onAudioEncodedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startEncoder(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT, DEFAULT_BIT_RATE, DEFAULT_MAX_BUFFER_SIZE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopEncoder();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startEncoder(int sampleRate, int channelCount, int bitRate, int maxInputSize) {
        if (isEncoderStarted) {
            logger.info("Encoder has started.");
            return true;
        }

        try {
            mediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            bufferInfo = new MediaCodec.BufferInfo();
            mediaCodec.start();
            isEncoderStarted = true;
        } catch (Exception e) {
            logger.warn("Create encoder exception:" + e);
            return false;
        }

        logger.info("Start encoder success.");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopEncoder() {
        if (!isEncoderStarted) {
            return;
        }

        try {
            mediaCodec.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mediaCodec.release();
        isEncoderStarted = false;
        encodedIndex = 0;
        logger.info("Stop encoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean encode(byte[] data) {
        if (!isEncoderStarted) {
            logger.warn("Encoder is not started.");
            return false;
        }

        try {
            int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();
                inputBuffer.put(data);
                mediaCodec.queueInputBuffer(inputBufferId, 0, data.length, System.nanoTime() / 1000, 0);
            }

            int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            while (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] encodedData = new byte[bufferInfo.size];
                outputBuffer.get(encodedData, 0, bufferInfo.size);
                if (onAudioEncodedListener != null) {
                    onAudioEncodedListener.onAudioEncoded(encodedData, ++encodedIndex);
                }
                mediaCodec.releaseOutputBuffer(outputBufferId, false);
                outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            }
        } catch (Exception e) {
            logger.warn("Encode input exception:" + e);
            return false;
        }

        return true;
    }
}

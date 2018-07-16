package com.xiaomi.mimcdemo.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_CHANNEL_COUNT;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_MAX_BUFFER_SIZE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_SAMPLE_RATE;

/**
 * Created by houminjiang on 18-6-14.
 */

public class AudioDecoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isDecoderStarted = false;
    private OnAudioDecodedListener onAudioDecodedListener;
    private MediaCodec.BufferInfo bufferInfo;
    private final Logger logger = LoggerFactory.getLogger(AudioDecoder.class);

    public void setOnAudioDecodedListener(OnAudioDecodedListener onAudioDecodedListener) {
        this.onAudioDecodedListener = onAudioDecodedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startDecoder(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT, DEFAULT_MAX_BUFFER_SIZE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopDecoder();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startDecoder(int sampleRate, int channelCount, int maxInputSize) {
        if (isDecoderStarted) {
            logger.info("Decoder has started.");
            return true;
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(MIMETYPE_AUDIO_AAC);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(format, null, null, 0);
            bufferInfo = new MediaCodec.BufferInfo();
            mediaCodec.start();
            isDecoderStarted = true;
        } catch (Exception e) {
            logger.warn("Create decoder exception:" + e);
            return false;
        }

        logger.info("Start decoder success.");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopDecoder() {
        if (!isDecoderStarted) {
            return;
        }

        try {
            mediaCodec.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mediaCodec.release();
        isDecoderStarted = false;
        logger.info("Stop decoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean decode(byte[] data) {
        if (!isDecoderStarted) {
            logger.warn("Decoder is not started.");
            return false;
        }

        try {
            int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();
                inputBuffer.put(data);
                inputBuffer.limit(data.length);
                mediaCodec.queueInputBuffer(inputBufferId, 0, data.length, System.nanoTime(), 0);
            }

            int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] decodedData = new byte[bufferInfo.size];
                outputBuffer.get(decodedData);
                if (onAudioDecodedListener != null) {
                    onAudioDecodedListener.onAudioDecoded(decodedData);
                }
                mediaCodec.releaseOutputBuffer(outputBufferId, false);
                outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Exception e) {
            logger.warn("Decode input exception:" + e);
            return false;
        }

        return true;
    }
}

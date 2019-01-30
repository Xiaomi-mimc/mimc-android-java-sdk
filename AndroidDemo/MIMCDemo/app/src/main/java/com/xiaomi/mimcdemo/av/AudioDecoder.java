package com.xiaomi.mimcdemo.av;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static com.xiaomi.mimcdemo.common.Constant.DEFAULT_AUDIO_CHANNEL_CONFIG;
import static com.xiaomi.mimcdemo.common.Constant.DEFAULT_AUDIO_FORMAT;
import static com.xiaomi.mimcdemo.common.Constant.DEFAULT_CODEC_CHANNEL_COUNT;
import static com.xiaomi.mimcdemo.common.Constant.DEFAULT_AUDIO_SAMPLE_RATE;

/**
 * Created by houminjiang on 18-6-14.
 */

public class AudioDecoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isDecoderStarted = false;
    private OnAudioDecodedListener onAudioDecodedListener;
    private MediaCodec.BufferInfo bufferInfo;
    private static final String TAG = "AudioDecoder";

    public void setOnAudioDecodedListener(OnAudioDecodedListener onAudioDecodedListener) {
        this.onAudioDecodedListener = onAudioDecodedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startDecoder(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_CODEC_CHANNEL_COUNT,
            2 * AudioRecord.getMinBufferSize(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopDecoder();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startDecoder(int sampleRate, int channelCount, int maxInputSize) {
        if (isDecoderStarted) {
            Log.i(TAG, "Decoder has started.");
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
            Log.e(TAG, "Create decoder exception:", e);
            return false;
        }

        Log.i(TAG, "Start decoder success.");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopDecoder() {
        if (!isDecoderStarted) {
            return;
        }
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        isDecoderStarted = false;
        Log.i(TAG, "Stop decoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean codec(byte[] data) {
        if (!isDecoderStarted) {
            Log.w(TAG, "Decoder is not started.");
            return false;
        }

        try {
            int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();
                inputBuffer.put(data);
                inputBuffer.limit(data.length);
                mediaCodec.queueInputBuffer(inputBufferId, 0, data.length, System.nanoTime() / 1000, 0);
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
            Log.e(TAG, "Decode input exception:", e);
            return false;
        }

        return true;
    }
}

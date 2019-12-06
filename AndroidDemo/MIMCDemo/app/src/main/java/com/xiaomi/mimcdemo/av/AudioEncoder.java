package com.xiaomi.mimcdemo.av;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static com.xiaomi.mimcdemo.common.Constant.*;

/**
 * Created by houminjiang on 18-6-14.
 */

public class AudioEncoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isEncoderStarted = false;
    private OnAudioEncodedListener onAudioEncodedListener;
    private MediaCodec.BufferInfo bufferInfo;
    private long encodedSequence = 0;
//    private byte[] specificData;
    private static final String TAG = "AudioEncoder";

    public void setOnAudioEncodedListener(OnAudioEncodedListener onAudioEncodedListener) {
        this.onAudioEncodedListener = onAudioEncodedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startEncoder(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_CODEC_CHANNEL_COUNT, DEFAULT_ENCODER_BIT_RATE,
            2 * AudioRecord.getMinBufferSize(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopEncoder();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startEncoder(int sampleRate, int channelCount, int bitRate, int maxInputSize) {
        if (isEncoderStarted) {
            Log.w(TAG, "Encoder has started.");
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
            Log.e(TAG, "Create encoder exception:", e);
            return false;
        }

        Log.i(TAG, "Start encoder success.");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopEncoder() {
        if (!isEncoderStarted) {
            return;
        }
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        isEncoderStarted = false;
        encodedSequence = 0;
        Log.i(TAG, "Stop encoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean codec(byte[] data) {
        if (!isEncoderStarted) {
            Log.w(TAG, "Encoder is not started.");
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
                byte[] encodedData = new byte[bufferInfo.size];
                outputBuffer.get(encodedData, 0, bufferInfo.size);

                if (onAudioEncodedListener != null) {
                    onAudioEncodedListener.onAudioEncoded(encodedData, ++encodedSequence);
                }
                mediaCodec.releaseOutputBuffer(outputBufferId, false);
                outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Encode input exception:", e);
            return false;
        }

        return true;
    }
}

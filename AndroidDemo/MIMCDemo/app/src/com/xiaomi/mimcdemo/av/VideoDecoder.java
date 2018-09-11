package com.xiaomi.mimcdemo.av;

import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;


import java.nio.ByteBuffer;

import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_VIDEO_FRAME_RATE;

/**
 * Created by houminjiang on 18-6-14.
 */

public class VideoDecoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isDecoderStarted = false;
    private MediaCodec.BufferInfo bufferInfo;
    private static final String TAG = "VideoDecoder";
    private int width;
    private int height;
    private Surface surface;

    public VideoDecoder(int width, int height, Surface surface) {
        this.width = width;
        this.height = height;
        this.surface = surface;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startDecoder(width, height, DEFAULT_VIDEO_FRAME_RATE, surface);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopDecoder();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean startDecoder(int width, int height, int frameRate, Surface surface) {
        if (isDecoderStarted) {
            Log.i(TAG, "Video decoder has started.");
            return true;
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(KEY_FRAME_RATE, frameRate);           // 帧率
            mediaCodec.configure(format, surface, null, 0);
            bufferInfo = new MediaCodec.BufferInfo();
            mediaCodec.start();
            isDecoderStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "Create video decoder exception:" + e);
            return false;
        }

        Log.i(TAG, "Start video decoder success.");
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
        Log.i(TAG, "Stop video decoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean decode(byte[] data) {
        if (!isDecoderStarted) {
            Log.w(TAG, "Video decoder is not started.");
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
                mediaCodec.releaseOutputBuffer(outputBufferId, true);
                outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Video decode input exception:" + e);
            return false;
        }

        return true;
    }
}

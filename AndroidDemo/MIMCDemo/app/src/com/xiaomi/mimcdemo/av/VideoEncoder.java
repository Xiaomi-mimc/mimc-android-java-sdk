package com.xiaomi.mimcdemo.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.xiaomi.mimcdemo.listener.OnVideoEncodedListener;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_COLOR_FORMAT;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_VIDEO_BIT_RATE;
import static com.xiaomi.mimcdemo.constant.Constant.DEFAULT_VIDEO_FRAME_RATE;

/**
 * Created by houminjiang on 18-6-14.
 */

public class VideoEncoder implements Codec {
    private MediaCodec mediaCodec;
    private boolean isEncoderStarted = false;
    private OnVideoEncodedListener onVideoEncodedListener;
    private MediaCodec.BufferInfo bufferInfo;
    private long encodedSequence = 0;
    private static final String TAG = "VideoEncoder";
    private int width;
    private int height;
    private long pts = 0;
    private long generateIndex = 0;
    private byte[] specificData;

    public VideoEncoder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setOnVideoEncodedListener(OnVideoEncodedListener onVideoEncodedListener) {
        this.onVideoEncodedListener = onVideoEncodedListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean start() {
        return startEncoder(width, height, DEFAULT_VIDEO_FRAME_RATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stop() {
        stopEncoder();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean startEncoder(int width, int height, int frameRate) {
        if (isEncoderStarted) {
            Log.i(TAG, "Video encoder has started.");
            return true;
        }

        try {
            mediaCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
            //MediaFormat format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height);
            MediaFormat format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, height, width);
            format.setInteger(KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(KEY_BIT_RATE, DEFAULT_VIDEO_BIT_RATE);    // 码率，越高越清晰
            format.setInteger(KEY_FRAME_RATE, frameRate);           // 帧率
            format.setInteger(KEY_I_FRAME_INTERVAL, 1);             // 关键帧间隔，越大压缩率越高

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            bufferInfo = new MediaCodec.BufferInfo();
            mediaCodec.start();
            isEncoderStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "Create video encoder exception:" + e);
            return false;
        }

        Log.i(TAG, "Start video encoder success.");
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
        encodedSequence = 0;
        Log.i(TAG, "Stop video encoder success.");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean encode(byte[] data, int width, int height) {
        if (!isEncoderStarted) {
            Log.i(TAG, "Video encoder is not started.");
            return false;
        }

        try {
            int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferId >= 0) {
                pts = computePresentationTimeUs(generateIndex);
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();
                inputBuffer.put(data);
                mediaCodec.queueInputBuffer(inputBufferId, 0, data.length, pts, 0);
                generateIndex++;
            }

            int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            while (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] encodedData = new byte[bufferInfo.size];
                outputBuffer.get(encodedData, 0, bufferInfo.size);
//                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                    // 开始第一帧，特定数据
//                    specificData = new byte[bufferInfo.size];
//                    specificData = encodedData;
//                } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//                    byte[] keyframe = new byte[bufferInfo.size + specificData.length];
//                    System.arraycopy(specificData, 0, keyframe, 0, specificData.length);
//                    System.arraycopy(encodedData, 0, keyframe, specificData.length, encodedData.length);
//                    encodedData = new byte[keyframe.length];
//                    System.arraycopy(keyframe, 0, encodedData, 0, keyframe.length);
//                }

                if (onVideoEncodedListener != null) {
                    onVideoEncodedListener.onVideoEncoded(encodedData, width, height, ++encodedSequence);
                }
                mediaCodec.releaseOutputBuffer(outputBufferId, false);
                outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Video encode input exception:" + e);
            return false;
        }

        return true;
    }

    private long computePresentationTimeUs(long frameIndex) {
        return  frameIndex * 1000000 / DEFAULT_VIDEO_FRAME_RATE;
    }
}

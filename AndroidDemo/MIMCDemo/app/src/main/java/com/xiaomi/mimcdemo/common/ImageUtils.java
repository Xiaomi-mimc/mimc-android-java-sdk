package com.xiaomi.mimcdemo.common;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;

import java.nio.ByteBuffer;


public class ImageUtils {
    public static final int COLOR_FormatI420 = 1;       // YUV420Planar         YYYYYYYYYYYYYYYYUUUUVVVV
    public static final int COLOR_FormatNV21 = 2;       // YUV420SemiPlanar     YYYYYYYYYYYYYYYYVUVUVUVU

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("Only support COLOR_FormatI420 and COLOR_FormatNV21.");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("Can't convert Image to byte array, format:" + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        // V起始位置
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int)(width * height * 1.25);       // Y+U
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        // U的起始位置
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return data;
    }

    public static void nv21ToNv12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;

        int frameSize = width * height;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (int i = 0; i < frameSize; i++) {
            nv12[i] = nv21[i];
        }
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize + 1];
        }
        for (int j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j + 1] = nv21[j + frameSize];
        }
    }

    public static void i420ToNv12(byte[] i420, byte[] nv12, int width, int height) {
        for (int i = 0; i < width * height; i++) {
            nv12[i] = i420[i];
        }

        for (int i = 0; i < width * height / 2; i++) {
            nv12[width * height + i] = i420[width * height + i / 2];
            i++;
            nv12[width * height + i] = i420[width * height + (width * height / 4) + (i - 1) / 2];
        }
    }

    public static void nv12Rotate270(byte[] des, byte[] src, int width, int height) {
        int n = 0;
        int uvHeight = height >> 1;
        int wh = width * height;

        for (int j = width - 1; j >= 0; j--) {
            for (int i = 0; i < height; i++) {
                des[n++] = src[width * i + j];
            }
        }

        for (int j = width - 1; j > 0; j -= 2) {
            for (int i = 0; i < uvHeight; i++) {
                des[n++] = src[wh + width * i + j - 1];
                des[n++] = src[wh + width * i + j];
            }
        }
    }

    public static void i420Rotate270(byte[] des, byte[] src, int width, int height) {
        int n = 0;
        int hw = width / 2;
        int hh = height / 2;

        for(int j = width; j > 0; j--)
        {
            for(int i = 0; i < height; i++)
            {
                des[n++] = src[width * i + j];
            }
        }
        for(int j = hw - 1; j >= 0; j--)
        {
            for(int i = 0; i < hh; i++)
            {
                des[n++] = src[width * height + hw * i + j];
            }
        }
        for(int j = hw - 1; j >= 0; j--)
        {
            for(int i = 0; i < hh; i++)
            {
                des[n++] = src[width * height + width * height / 4 +hw * i + j];
            }
        }
    }
}

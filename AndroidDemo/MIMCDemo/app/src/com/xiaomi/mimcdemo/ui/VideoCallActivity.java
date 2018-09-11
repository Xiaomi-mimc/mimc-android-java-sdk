package com.xiaomi.mimcdemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.xiaomi.mimc.proto.RtsData;
import com.xiaomi.mimc.proto.RtsSignal;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.av.AudioDecoder;
import com.xiaomi.mimcdemo.av.AudioEncoder;
import com.xiaomi.mimcdemo.av.AudioPlayer;
import com.xiaomi.mimcdemo.av.AudioRecorder;
import com.xiaomi.mimcdemo.av.VideoDecoder;
import com.xiaomi.mimcdemo.av.VideoEncoder;
import com.xiaomi.mimcdemo.bean.AudioData;
import com.xiaomi.mimcdemo.bean.VideoData;
import com.xiaomi.mimcdemo.common.ImageUtils;
import com.xiaomi.mimcdemo.common.UserManager;
import com.xiaomi.mimcdemo.listener.OnAudioCapturedListener;
import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;
import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;
import com.xiaomi.mimcdemo.listener.OnCallStateListener;
import com.xiaomi.mimcdemo.listener.OnVideoCapturedListener;
import com.xiaomi.mimcdemo.listener.OnVideoEncodedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.xiaomi.mimcdemo.common.ImageUtils.COLOR_FormatNV21;



@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoCallActivity extends Activity implements View.OnClickListener, OnCallStateListener, OnAudioCapturedListener
    , OnAudioEncodedListener, OnAudioDecodedListener, OnVideoCapturedListener, OnVideoEncodedListener {
    private Button btnHangUpCall;
    private Button btnAnswerCall;
    private Button btnComingRejectCall;
    private Button btnSend;
    private TextView tvAppAccount;
    private TextView tvCallState;
    private RelativeLayout rlComingCallContainer;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private AudioEncoder audioEncoder;
    private AudioDecoder audioDecoder;
    private VideoEncoder videoEncoder;
    private VideoDecoder videoDecoder;
    protected Handler handler;
    protected String username;
    protected boolean isComing;
    protected Long chatId;
    public final static int MSG_CALL_MAKE_VIDEO = 0;
    public final static int MSG_CALL_ANSWER = 2;
    public final static int MSG_CALL_REJECT = 3;
    public final static int MSG_CALL_HANG_UP = 4;
    public final static int MSG_FINISH = 5;
    public final static int MAX_SIZE = 75 * 1024;
    public final static int MSG_CALL_MAKE_VIDEO_DELAY_MS = 50;
    public final static int MSG_FINISH_DELAY_MS = 1 * 1000;
    private AudioManager audioManager;
    private BlockingQueue<AudioData> audioEncodeQueue;
    private AudioEncodeThread audioEncodeThread;
    private BlockingQueue<VideoData> videoEncodeQueue;
    private VideoEncodeThread videoEncodeThread;
    private BlockingQueue<AudioData> audioDecodeQueue;
    private AudioDecodeThread audioDecodeThread;
    private BlockingQueue<VideoData> videoDecodeQueue;
    private VideoDecodeThread videoDecodeThread;
    private volatile boolean isExitCodecThread = false;
    private static final String TAG = "VideoCallActivity";
    private CameraManager cameraManager;
    private String frontFacingCameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private final static int REQUEST_CODE_RECORD_AUDIO = 1;
    private final static int REQUEST_CODE_CAMERA = 2;
    private ImageReader imageReader;
    private CaptureRequest.Builder captureRequestBuilder;
    private AutoFitTextureView textureView;
    private Surface surface;
    private Size previewSize;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private int capturedWidth = 640;   // 640 * 360、960 × 540、1280 * 720
    private int capturedHeight = 360;


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, String.format("TextureView is available. width:%d height:%d", width, height));
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "Camera is opened.");

            cameraDevice = camera;
            createCameraCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "Camera is disconnected.");

            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(TAG, String.format("Camera error:%d.", error));

            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }
    };

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initImageReader(int width, int height) {
        // ImageFormat.NV21 不支持
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireNextImage();
                    if (image == null) {
                        Log.w(TAG, "This current image is null.");
                        return;
                    }
                    int width = image.getWidth();
                    int height = image.getHeight();
                    Log.d(TAG, String.format("Captured video data width:%d height:%d", width, height));
                    byte[] nv21Data = ImageUtils.getDataFromImage(image, COLOR_FormatNV21);
                    byte[] nv12Data = new byte[width * height * 3 / 2];     // YUV数据内存大小
                    ImageUtils.nv21ToNv12(nv21Data, nv12Data, width, height);
                    byte[] nv12DataRotate270 = new byte[width * height * 3 / 2];
                    ImageUtils.nv12Rotate270(nv12DataRotate270, nv12Data, width, height);
                    onVideoCaptured(nv12DataRotate270, width, height);
                } catch(Exception e) {
                    Log.e(TAG, "Captured video data exception:", e);
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, backgroundHandler);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        textureView = findViewById(R.id.tv_big);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    // 拨打语音电话
                    case MSG_CALL_MAKE_VIDEO:
                        chatId = UserManager.getInstance().dialCall(username, null, RtsSignal.StreamDataType.V_STREAM, null);
                        if (chatId == null) finish("Dial call fail, chatId is null.");
                        break;
                    // 同意
                    case MSG_CALL_ANSWER:
                        UserManager.getInstance().answerCall();
                        break;
                    // 拒绝
                    case MSG_CALL_REJECT:
                        UserManager.getInstance().rejectCall();
                        msg = Message.obtain();
                        msg.what = MSG_FINISH;
                        handler.sendMessageDelayed(msg, MSG_FINISH_DELAY_MS);
                        break;
                    // 挂断
                    case MSG_CALL_HANG_UP:
                        UserManager.getInstance().closeCall(chatId);
                        msg = Message.obtain();
                        msg.what = MSG_FINISH;
                        Bundle bundle = new Bundle();
                        bundle.putString("msg", getResources().getString(R.string.call_canceled));
                        msg.setData(bundle);
                        handler.sendMessageDelayed(msg, MSG_FINISH_DELAY_MS);
                        break;
                    // 结束
                    case MSG_FINISH:
                        finish(msg.getData().getString("msg"));
                        break;

                }

                return true;
            }
        });
        audioRecorder = new AudioRecorder();
        UserManager.getInstance().setCallStateListener(this);
        audioRecorder.setOnAudioCapturedListener(this);
        audioPlayer = new AudioPlayer(this, AudioManager.MODE_NORMAL);
        audioPlayer.start();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioEncoder = new AudioEncoder();
        audioEncoder.setOnAudioEncodedListener(this);
        audioEncoder.start();
        audioDecoder = new AudioDecoder();
        audioDecoder.setOnAudioDecodedListener(this);
        audioDecoder.start();
        audioEncodeQueue = new LinkedBlockingDeque<>();
        audioEncodeThread = new AudioEncodeThread();
        audioEncodeThread.start();
        videoEncodeQueue = new LinkedBlockingDeque<>();
        videoEncodeThread = new VideoEncodeThread();
        videoEncodeThread.start();
        audioDecodeQueue = new PriorityBlockingQueue<>(24, new Comparator<AudioData>() {
            @Override
            public int compare(AudioData o1, AudioData o2) {
                if (o1.getSequence() > o2.getSequence()) {
                    return 1;
                } else if (o1.getSequence() == o2.getSequence()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        audioDecodeThread = new AudioDecodeThread();
        audioDecodeThread.start();
        videoDecodeQueue = new PriorityBlockingQueue<>(24, new Comparator<VideoData>() {
            @Override
            public int compare(VideoData o1, VideoData o2) {
                if (o1.getSequence() > o2.getSequence()) {
                    return 1;
                } else if (o1.getSequence() == o2.getSequence()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        videoDecodeThread = new VideoDecodeThread();
        videoDecodeThread.start();

        username = getIntent().getStringExtra("username");
        chatId = getIntent().getLongExtra("chatId", 0);
        tvAppAccount = findViewById(R.id.tv_app_account);
        tvAppAccount.setText(username);
        tvCallState = findViewById(R.id.tv_call_state);
        tvCallState.setText(getResources().getString(R.string.is_connecting));
        btnHangUpCall = findViewById(R.id.btn_hang_up_call);
        btnHangUpCall.setOnClickListener(this);
        btnAnswerCall = findViewById(R.id.btn_answer_call);
        btnAnswerCall.setOnClickListener(this);
        btnComingRejectCall = findViewById(R.id.btn_coming_reject_call);
        btnComingRejectCall.setOnClickListener(this);
        rlComingCallContainer = findViewById(R.id.rl_coming_call_container);

        isComing = getIntent().getBooleanExtra("isComing", false);
        if (chatId != 0) {
            btnHangUpCall.setVisibility(View.INVISIBLE);
            rlComingCallContainer.setVisibility(View.VISIBLE);
        } else {
            Message msg = Message.obtain();
            msg.what = MSG_CALL_MAKE_VIDEO;
            handler.sendMessageDelayed(msg, MSG_CALL_MAKE_VIDEO_DELAY_MS);
        }

        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(this);
        startBackgroundThread();
        startBackgroundThread();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {
        Message msg;
        switch (v.getId()) {
            case R.id.btn_hang_up_call:
                msg = Message.obtain();
                msg.what = MSG_CALL_HANG_UP;
                handler.sendMessage(msg);
                break;
            case R.id.btn_coming_reject_call:
                msg = Message.obtain();
                msg.what = MSG_CALL_REJECT;
                handler.sendMessage(msg);
                break;
            case R.id.btn_answer_call:
                msg = Message.obtain();
                msg.what = MSG_CALL_ANSWER;
                handler.sendMessage(msg);
                btnHangUpCall.setVisibility(View.VISIBLE);
                rlComingCallContainer.setVisibility(View.INVISIBLE);

                tvCallState.setText(getResources().getString(R.string.is_connected));
                // 连接建立，启动音视频采集
                startRecording();
                startCameraCapturing();
                break;
            case R.id.btn_send: {
                byte[] bytes = new byte[MAX_SIZE];
                for (int i = 0; i < MAX_SIZE; i++) {
                    bytes[i] = 1;
                }
                UserManager.getInstance().sendRTSData(chatId, bytes, RtsData.PKT_TYPE.USER_DATA_VIDEO);
            }
            break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void startRecording() {
        // 开始采集前获取运行时录音权限
        if (checkRecordAudioPermission()) {
//            audioRecorder.start();
        }
    }

    public void startCameraCapturing() {
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private void openCamera() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
            initCamera();
            cameraManager.openCamera(frontFacingCameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Open camera exception:", e);
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "CAMERA permission is denied by user.", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }
    }

    private boolean checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "RECORD_AUDIO permission is denied by user.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            }
            return false;
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "RECORD_AUDIO GRANTED.", Toast.LENGTH_SHORT).show();
                    audioRecorder.start();
                } else {
                    Toast.makeText(this, "RECORD_AUDIO DENIED.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "CAMERA GRANTED.", Toast.LENGTH_SHORT).show();
                    openCamera();
                } else {
                    Toast.makeText(this, "CAMERA DENIED.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onLaunched(String fromAccount, String fromResource, Long chatId, byte[] data) {

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAnswered(Long chatId, boolean accepted, String errMsg) {
        if (accepted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCallState.setText(getResources().getString(R.string.is_connected));
                }
            });
            // 采集数据
            startRecording();
            startCameraCapturing();
        } else {
            finish(getResources().getString(R.string.rejected));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void handleData(Long chatId, RtsData.PKT_TYPE pktType, byte[] data) {
        if (pktType == RtsData.PKT_TYPE.USER_DATA_AUDIO) {
            AudioData audioData = (AudioData)UserManager.fromByteArray(data);
            try {
                Log.i(TAG, String.format("Audio sequence:%d length:%d", audioData.getSequence(), audioData.getData().length));
                audioDecodeQueue.put(audioData);
            } catch (InterruptedException e) {
                Log.e(TAG, "Input audio data exception:", e);
            }
        } else if (pktType == RtsData.PKT_TYPE.USER_DATA_VIDEO) {
            VideoData videoData = (VideoData)UserManager.fromByteArray(data);
            try {
                Log.i(TAG, String.format("Video sequence:%d length:%d", videoData.getSequence(), videoData.getData().length));
                videoDecodeQueue.put(videoData);
            } catch (InterruptedException e) {
                Log.e(TAG, "Input video data exception:", e);
            }
        }
    }

    @Override
    public void onClosed(Long chatId, final String errMsg) {
        Message msg = Message.obtain();
        msg.what = MSG_FINISH;
        Bundle bundle = new Bundle();
        bundle.putString("msg", errMsg);
        msg.setData(bundle);
        handler.sendMessageDelayed(msg, MSG_FINISH_DELAY_MS);
    }

    private void finish(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(msg))
                    Toast.makeText(VideoCallActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isExitCodecThread = true;
        audioRecorder.stop();
        audioEncoder.stop();
        audioDecoder.stop();
        audioPlayer.stop();
        if (videoEncoder != null) {
            videoEncoder.stop();
        }
        if (videoDecoder != null) {
            videoDecoder.stop();
        }
        closeCamera();
        stopBackgroundThread();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAudioCaptured(byte[] data) {
        AudioData audioData = new AudioData(data);
        try {
            audioEncodeQueue.put(audioData);
        } catch (InterruptedException e) {
            Log.w(TAG, "Input captured audio data into encode queue exception:", e);
        }
    }

    @Override
    public void onAudioEncoded(byte[] data, long sequence) {
        AudioData audioData = new AudioData(sequence, data);
        UserManager.getInstance().sendRTSData(chatId, UserManager.toByteArray(audioData), RtsData.PKT_TYPE.USER_DATA_AUDIO);
    }

    @Override
    public void onAudioDecoded(byte[] data) {
        audioPlayer.play(data, 0, data.length);
    }

    public static void actionStartActivity(Context context, String username) {
        Intent intent = new Intent(context, VideoCallActivity.class);
        intent.putExtra("username", username);
        context.startActivity(intent);
    }

    @Override
    public void onVideoCaptured(byte[] data, int width, int height) {
        VideoData videoData = new VideoData(data, width, height);
        try {
            videoEncodeQueue.put(videoData);
        } catch (InterruptedException e) {
            Log.e(TAG, "Input captured video data into encode queue exception:", e);
        }
    }

    @Override
    public void onVideoEncoded(byte[] data, int width, int height, long sequence) {
        VideoData videoData = new VideoData(sequence, data, width, height);
        UserManager.getInstance().sendRTSData(chatId, UserManager.toByteArray(videoData), RtsData.PKT_TYPE.USER_DATA_VIDEO);
    }

    public static void actionStartActivity(Context context, String username, long chatId) {
        Intent intent = new Intent(context, VideoCallActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("username", username);
        intent.putExtra("chatId", chatId);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera() {
        cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (cameraIdList == null) {
                Log.w(TAG, "Get camera id list is null.");
                return;
            }
            for (String cameraId : cameraIdList) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    // 获取前置摄像头配置属性
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = new Size(capturedWidth, capturedHeight);
                    List list = Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
                    if (!list.contains(previewSize)) {
                        previewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), capturedWidth, capturedHeight);
                    }
                    initImageReader(previewSize.getWidth(), previewSize.getHeight());
                    frontFacingCameraId = cameraId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "GetFrontFacingCameraId exception:", e);
        }
    }

    // 获取首选的大小
    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for(Size option : mapSizes) {
            if(width > height) {
                if(option.getWidth() > width &&
                    option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if(option.getWidth() > height &&
                    option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if(collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return mapSizes[0];
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createCameraCaptureSession() {
        if (cameraDevice == null) {
            Log.w(TAG, "Camera device is null.");
            return;
        }

        try {
            // 关闭先前预览会话
            closeCameraPreviewSession();
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            surface = new Surface(surfaceTexture);
            if (videoEncoder != null) {
                videoEncoder.stop();
            }
            videoEncoder = new VideoEncoder(previewSize.getWidth(), previewSize.getHeight());
            videoEncoder.setOnVideoEncodedListener(this);
            videoEncoder.start();
            if (videoDecoder != null) {
                videoDecoder.stop();
            }
            videoDecoder = new VideoDecoder(previewSize.getWidth(), previewSize.getHeight(), surface);
            videoDecoder.start();
            // CameraDevice.TEMPLATE_RECORD
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            //captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            //cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "onConfigured ...");

                    if (cameraDevice == null) {
                        Log.w(TAG, "This camera device is null.");
                        return;
                    }
                    cameraCaptureSession = session;
                    try {
                        //captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        //captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.i(TAG, "Set repeating request exception:" + e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "onConfigureFailed");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException:", e);
        }
    }

    private void closeCameraPreviewSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void closeCamera() {
        closeCameraPreviewSession();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        if (imageReader != null) {
            imageReader.close();
        }
    }

    class AudioEncodeThread extends Thread {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (!isExitCodecThread) {
                try {
                    AudioData data = audioEncodeQueue.poll();
                    if (data != null) {
                        audioEncoder.encode(data.getData());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class AudioDecodeThread extends Thread {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (!isExitCodecThread) {
                try {
                    if (audioDecodeQueue.size() > 12) {
                        Log.i(TAG, String.format("-------------------------------audioDecodeQueue.size:%d", audioDecodeQueue.size()));
                        audioDecodeQueue.clear();
                    }
                    AudioData data = audioDecodeQueue.poll();
                    if (data != null) {
                        audioDecoder.decode(data.getData());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class VideoEncodeThread extends Thread {
        @Override
        public void run() {
            while (!isExitCodecThread) {
                try {
                    VideoData videoData = videoEncodeQueue.poll();
                    if (videoData != null) {
                        videoEncoder.encode(videoData.getData(), videoData.getWidth(), videoData.getHeight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class VideoDecodeThread extends Thread {
        @Override
        public void run() {
            while (!isExitCodecThread) {
                try {
                    if (videoDecodeQueue.size() > 12) {
                        Log.i(TAG, String.format("-------------------------------videoDecodeQueue.size:%d", videoDecodeQueue.size()));
                        videoDecodeQueue.clear();
                    }
                    VideoData videoData = videoDecodeQueue.poll();
                    if (videoData != null) {
                        if (videoData.getSequence() <= 1) {
                            Log.d(TAG, String.format("Filter video sequence:%d", videoData.getSequence()));
                            //continue;
                        }
                        videoDecoder.decode(videoData.getData());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

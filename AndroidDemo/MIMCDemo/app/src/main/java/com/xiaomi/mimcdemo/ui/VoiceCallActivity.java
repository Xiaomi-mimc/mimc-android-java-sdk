package com.xiaomi.mimcdemo.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.xiaomi.mimc.data.RtsDataType;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.av.AudioPlayer;
import com.xiaomi.mimcdemo.av.AudioRecorder;
import com.xiaomi.mimcdemo.av.FFmpegAudioDecoder;
import com.xiaomi.mimcdemo.av.FFmpegAudioEncoder;
import com.xiaomi.mimcdemo.bean.Audio;
import com.xiaomi.mimcdemo.common.UserManager;
import com.xiaomi.mimcdemo.listener.OnAudioCapturedListener;
import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;
import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;
import com.xiaomi.mimcdemo.listener.OnCallStateListener;
import com.xiaomi.mimcdemo.proto.AV;
import com.xiaomi.mimcdemo.service.CallService;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;


import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class VoiceCallActivity extends Activity implements View.OnClickListener, OnCallStateListener, OnAudioCapturedListener
    , OnAudioEncodedListener, OnAudioDecodedListener {
    private Button btnHangUpCall;
    private Button btnAnswerCall;
    private Button btnComingRejectCall;
    private TextView tvAppAccount;
    private TextView tvCallState;
    private RelativeLayout rlComingCallContainer;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private FFmpegAudioEncoder audioEncoder;
    private FFmpegAudioDecoder audioDecoder;
//    private AudioEncoder audioEncoder;
//    private AudioDecoder audioDecoder;
    protected Handler handler;
    protected String username;
    protected long callId = -1;
    public final static int MSG_CALL_MAKE_VOICE = 0;
    public final static int MSG_CALL_ANSWER = 2;
    public final static int MSG_CALL_REJECT = 3;
    public final static int MSG_CALL_HANGUP = 4;
    public final static int MSG_FINISH = 5;
    public final static int MSG_CALL_MAKE_VOICE_DELAY_MS = 50;
    public final static int MSG_FINISH_DELAY_MS = 1 * 1000;
    AudioManager audioManager;
    private BlockingQueue<Audio> audioEncodeQueue;
    private AudioEncodeThread audioEncodeThread;
    private BlockingQueue<AV.MIMCRtsPacket> audioDecodeQueue;
    private AudioDecodeThread audioDecodeThread;
    private volatile boolean isExit = false;
    private static final String TAG = "VoiceCallActivity";
    private ServiceConnection serviceConnection;
    private CallService.CallBinder callBinder;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                callBinder = (CallService.CallBinder)service;
                callBinder.setUsername(username);
                CallService.startService(VoiceCallActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    // 拨打语音电话
                    case MSG_CALL_MAKE_VOICE:
                        callId = UserManager.getInstance().dialCall(username, null, "AUDIO".getBytes());
                        if (callId == -1) {
                            finish("Dial call fail, chat id is null.");
                        }
                        break;
                    // 同意
                    case MSG_CALL_ANSWER:
                        UserManager.getInstance().answerCall();
                        tvCallState.setText(getResources().getString(R.string.is_connected));
                        startRecording();
                        startService();
                        break;
                    // 拒绝
                    case MSG_CALL_REJECT:
                        UserManager.getInstance().rejectCall();
                        msg = Message.obtain();
                        msg.what = MSG_FINISH;
                        handler.sendMessageDelayed(msg, MSG_FINISH_DELAY_MS);
                        break;
                    // 挂断
                    case MSG_CALL_HANGUP:
                        UserManager.getInstance().closeCall(callId);
                        callId = -1;
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
        audioPlayer = new AudioPlayer(this, AudioManager.MODE_IN_COMMUNICATION);
        audioPlayer.start();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        audioEncoder = new AudioEncoder();
        audioEncoder = new FFmpegAudioEncoder();
        audioEncoder.setOnAudioEncodedListener(this);
        audioEncoder.start();
//        audioDecoder = new AudioDecoder();
        audioDecoder = new FFmpegAudioDecoder();
        audioDecoder.setOnAudioDecodedListener(this);
        audioDecoder.start();
        audioEncodeQueue = new LinkedBlockingQueue<>();
        audioEncodeThread = new AudioEncodeThread();
        audioEncodeThread.start();
        audioDecodeQueue = new PriorityBlockingQueue<>(24, new Comparator<AV.MIMCRtsPacket>() {
            @Override
            public int compare(AV.MIMCRtsPacket o1, AV.MIMCRtsPacket o2) {
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

        username = getIntent().getStringExtra("username");
        callId = getIntent().getLongExtra("callId", -1);
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

        if (callId != -1) {
            btnHangUpCall.setVisibility(View.INVISIBLE);
            rlComingCallContainer.setVisibility(View.VISIBLE);
        } else {
            Message msg = Message.obtain();
            msg.what = MSG_CALL_MAKE_VOICE;
            handler.sendMessageDelayed(msg, MSG_CALL_MAKE_VOICE_DELAY_MS);
        }
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
                msg.what = MSG_CALL_HANGUP;
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
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void startRecording() {
        // 开始采集前获取运行时录音权限
        if (checkRecordAudioPermission()) {
            audioRecorder.start();
        }
    }

    @Override
    public void onLaunched(String fromAccount, String fromResource, long callId, byte[] data) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAnswered(long callId, boolean accepted, String errMsg) {
        if (accepted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCallState.setText(getResources().getString(R.string.is_connected));
                }
            });
            // 采集数据
            startRecording();
            startService();
        } else {
            finish(getResources().getString(R.string.rejected));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void handleData(long callId, RtsDataType dataType, byte[] data) {
        AV.MIMCRtsPacket audio;
        try {
            audio = AV.MIMCRtsPacket.parseFrom(data);
            audioDecodeQueue.offer(audio);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClosed(long callId, final String errMsg) {
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
                    Toast.makeText(VoiceCallActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callId != -1) {
            UserManager.getInstance().closeCall(callId);
        }
        isExit = true;
        audioRecorder.stop();
        audioEncodeThread.interrupt();
        audioEncodeThread = null;
        audioDecodeThread.interrupt();
        audioDecodeThread = null;
        audioEncoder.stop();
        audioDecoder.stop();
        audioPlayer.stop();
        stopService();
    }

    private void startService() {
        Intent intent = new Intent(this, CallService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void stopService() {
        try {
            CallService.stopService(this);
            unbindService(serviceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "RECORD_AUDIO permission is denied by user.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, 1);
            }
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "RECORD_AUDIO GRANTED.", Toast.LENGTH_SHORT).show();
                    audioRecorder.start();
                } else {
                    Toast.makeText(this, "RECORD_AUDIO DENIED.", Toast.LENGTH_SHORT).show();
                    finish(null);
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAudioCaptured(byte[] pcmData) {
        audioEncodeQueue.offer(new Audio(pcmData));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAudioEncoded(byte[] data, long sequence) {
        AV.MIMCRtsPacket audio = AV.MIMCRtsPacket
            .newBuilder()
            .setType(AV.MIMC_RTS_TYPE.AUDIO)
            .setCodecType(AV.MIMC_RTS_CODEC_TYPE.FFMPEG)
            .setPayload(ByteString.copyFrom(data))
            .setSequence(sequence)
            .build();
        if (-1 == UserManager.getInstance().sendRTSData(callId, audio.toByteArray(), RtsDataType.AUDIO)) {
            Log.e(TAG, String.format("Send audio data fail sequence:%d data.length:%d", sequence, data.length));
        }
    }

    @Override
    public void onAudioDecoded(byte[] data) {
        audioPlayer.play(data, 0, data.length);
    }

    public static void actionStartActivity(Context context, String username) {
        Intent intent = new Intent(context, VoiceCallActivity.class);
        intent.putExtra("username", username);
        context.startActivity(intent);
    }

    public static void actionStartActivity(Context context, String username, long callId) {
        Intent intent = new Intent(context, VoiceCallActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("username", username);
        intent.putExtra("callId", callId);
        context.startActivity(intent);
    }

    class AudioEncodeThread extends Thread {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (!isExit) {
                try {
                    Audio audio = audioEncodeQueue.take();
                    audioEncoder.codec(audio.getData());
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
            while (!isExit) {
                try {
                    if (audioDecodeQueue.size() > 12) {
                        Log.w(TAG, String.format("Clear decode queue size:%d", audioDecodeQueue.size()));
                        audioDecodeQueue.clear();
                        continue;
                    }

                    AV.MIMCRtsPacket rtsPacket = audioDecodeQueue.take();
                    audioDecoder.codec(rtsPacket.getPayload().toByteArray());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
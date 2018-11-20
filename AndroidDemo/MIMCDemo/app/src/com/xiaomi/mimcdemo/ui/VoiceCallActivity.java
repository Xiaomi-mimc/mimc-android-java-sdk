package com.xiaomi.mimcdemo.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.av.AudioDecoder;
import com.xiaomi.mimcdemo.av.AudioEncoder;
import com.xiaomi.mimcdemo.av.AudioPlayer;
import com.xiaomi.mimcdemo.av.AudioRecorder;
import com.xiaomi.mimcdemo.bean.AudioData;
import com.xiaomi.mimcdemo.common.UserManager;
import com.xiaomi.mimcdemo.listener.OnAudioCapturedListener;
import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;
import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;
import com.xiaomi.mimcdemo.listener.OnCallStateListener;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class VoiceCallActivity extends Activity implements View.OnClickListener, OnCallStateListener, OnAudioCapturedListener
    , OnAudioEncodedListener, OnAudioDecodedListener {
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
    protected Handler handler;
    protected String username;
    protected long chatId = -1;
    public final static int MSG_CALL_MAKE_VOICE = 0;
    public final static int MSG_CALL_ANSWER = 2;
    public final static int MSG_CALL_REJECT = 3;
    public final static int MSG_CALL_HANGUP = 4;
    public final static int MSG_FINISH = 5;
    public final static int MAX_SIZE = 100 * 1024;
    public final static int MSG_CALL_MAKE_VOICE_DELAY_MS = 50;
    public final static int MSG_FINISH_DELAY_MS = 1 * 1000;
    AudioManager audioManager;
    private BlockingQueue<AudioData> audioEncodeQueue;
    private AudioEncodeThread audioEncodeThread;
    private BlockingQueue<AudioData> audioDecodeQueue;
    private AudioDecodeThread audioDecodeThread;
    private volatile boolean isExitThread = false;
    private static final String TAG = "VoiceCallActivity";


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    // 拨打语音电话
                    case MSG_CALL_MAKE_VOICE:
                        chatId = UserManager.getInstance().dialCall(username, null, "AUDIO".getBytes());
                        if (chatId == -1) {
                            finish("Dial call fail, chat id is null.");
                        }
                        break;
                    // 同意
                    case MSG_CALL_ANSWER:
                        UserManager.getInstance().answerCall();
                        tvCallState.setText(getResources().getString(R.string.is_connected));
                        startRecording();
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
                        UserManager.getInstance().closeCall(chatId);
                        chatId = -1;
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
        audioEncoder = new AudioEncoder();
        audioEncoder.setOnAudioEncodedListener(this);
        audioEncoder.start();
        audioDecoder = new AudioDecoder();
        audioDecoder.setOnAudioDecodedListener(this);
        audioDecoder.start();
        audioEncodeQueue = new LinkedBlockingDeque<>();
        audioEncodeThread = new AudioEncodeThread();
        audioEncodeThread.start();
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

        username = getIntent().getStringExtra("username");
        chatId = getIntent().getLongExtra("chatId", -1);
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

        if (chatId != -1) {
            btnHangUpCall.setVisibility(View.INVISIBLE);
            rlComingCallContainer.setVisibility(View.VISIBLE);
        } else {
            Message msg = Message.obtain();
            msg.what = MSG_CALL_MAKE_VOICE;
            handler.sendMessageDelayed(msg, MSG_CALL_MAKE_VOICE_DELAY_MS);
        }

        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish(null);
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
            case R.id.btn_send:
            {
                // Test
                byte[] bytes = new byte[MAX_SIZE];
                for (int i = 0; i < MAX_SIZE; i++) {
                    bytes[i] = 1;
                }
            }
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
    public void onLaunched(String fromAccount, String fromResource, long chatId, byte[] data) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAnswered(long chatId, boolean accepted, String errMsg) {
        if (accepted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCallState.setText(getResources().getString(R.string.is_connected));
                }
            });
            // 采集数据
            startRecording();
        } else {
            finish(getResources().getString(R.string.rejected));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void handleData(long chatId, RtsDataType dataType, byte[] data) {
        audioDecodeQueue.offer((AudioData)UserManager.fromByteArray(data));
    }

    @Override
    public void onClosed(long chatId, final String errMsg) {
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
        if (chatId != -1) {
            UserManager.getInstance().closeCall(chatId);
        }
        isExitThread = true;
        audioRecorder.stop();
        audioEncodeThread.interrupt();
        audioEncodeThread = null;
        audioDecodeThread.interrupt();
        audioDecodeThread = null;
        audioEncoder.stop();
        audioDecoder.stop();
        audioPlayer.stop();
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
    public void onAudioCaptured(byte[] data) {
        audioEncodeQueue.offer(new AudioData(data));
    }

    @Override
    public void onAudioEncoded(byte[] data, long sequence) {
        AudioData audioData = new AudioData(sequence, data);
        if (-1 == UserManager.getInstance().sendRTSData(chatId, UserManager.toByteArray(audioData), RtsDataType.AUDIO)) {
            Log.d(TAG, String.format("Send audio data fail sequence:%d data.length:%d", audioData.getSequence(), audioData.getData().length));
            finish(null);
        } else {
            Log.d(TAG, String.format("Send audio data success sequence:%d data.length:%d", audioData.getSequence(), audioData.getData().length));
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

    public static void actionStartActivity(Context context, String username, long chatId) {
        Intent intent = new Intent(context, VoiceCallActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("username", username);
        intent.putExtra("chatId", chatId);
        context.startActivity(intent);
    }

    class AudioEncodeThread extends Thread {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (!isExitThread) {
                try {
                    AudioData data = audioEncodeQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (data != null) {
                        if(!audioEncoder.encode(data.getData())) {
                            Log.d(TAG, "Audio encode failed.");
                        }
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
            while (!isExitThread) {
                try {
                    if (audioDecodeQueue.size() > 12) {
                        Log.w(TAG, String.format("Clear decode queue size:%d", audioDecodeQueue.size()));
                        audioDecodeQueue.clear();
                    }
                    AudioData audioData = audioDecodeQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (audioData != null) {
                        if (!audioDecoder.decode(audioData.getData())) {
                            Log.d(TAG, "Audio decode failed.");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
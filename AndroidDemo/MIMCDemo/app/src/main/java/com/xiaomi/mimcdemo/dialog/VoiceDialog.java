package com.xiaomi.mimcdemo.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.common.UserManager;
import com.xiaomi.mimcdemo.constant.Constant;
import com.xiaomi.mimcdemo.ui.MainActivity;
import com.xiaomi.mimcdemo.ui.VoiceCallActivity;

public class VoiceDialog extends Dialog {

    public VoiceDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_dialog);
        setCancelable(true);
        setTitle(R.string.audio_call);
        final EditText toEditText = findViewById(R.id.voice_to);
        final SharedPreferences sp = getContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        toEditText.setText(sp.getString("voiceToAccount", ""));
        findViewById(R.id.chat_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mTo = toEditText.getText().toString();
                sp.edit().putString("voiceToAccount", mTo).commit();

                if (!TextUtils.isEmpty(mTo)) {
                    if (UserManager.getInstance().getStatus() == MIMCConstant.OnlineStatus.ONLINE) {
                        VoiceCallActivity.actionStartActivity(getContext(), mTo);
                    } else {
                        Toast.makeText(getContext(), getContext().getResources().getString(R.string.not_login), Toast.LENGTH_SHORT).show();
                    }

                    dismiss();
                }
            }
        });
    }
}

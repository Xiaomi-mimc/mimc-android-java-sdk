package com.xiaomi.mimcdemo.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.common.UserManager;
import com.xiaomi.mimcdemo.common.Constant;

public class SendUnlimitedGroupMsgDialog extends Dialog {

    public SendUnlimitedGroupMsgDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_unlimited_group_msg_dialog);
        setCancelable(true);
        setTitle(R.string.send_group_msg);
        final EditText etGroupId = (EditText)findViewById(R.id.et_group_id);
        final SharedPreferences sp = getContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        etGroupId.setText(sp.getString("toGroupId", null));

        findViewById(R.id.btn_group_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mTo = etGroupId.getText().toString();

                sp.edit().putString("toGroupId", mTo).commit();

                String mContent = ((EditText)findViewById(R.id.et_group_content))
                        .getText().toString();

                if (!TextUtils.isEmpty(mTo)){
                    UserManager userManager = UserManager.getInstance();
                    MIMCUser user = userManager.getUser();
                    if (user != null)
                        userManager.sendGroupMsg(Long.parseLong(mTo), mContent.getBytes(), Constant.TEXT, true);
                    dismiss();
                }
            }
        });
    }
}

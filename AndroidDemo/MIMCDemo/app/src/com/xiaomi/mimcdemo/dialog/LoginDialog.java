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
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.common.NetWorkUtils;
import com.xiaomi.mimcdemo.common.UserManager;

public class LoginDialog extends Dialog {

    public LoginDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_dialog);
        setCancelable(true);
        setTitle(R.string.login);

        final EditText accountEditText = (EditText) findViewById(R.id.account);
        final SharedPreferences sp = getContext().getSharedPreferences("user", Context.MODE_PRIVATE);

        accountEditText.setText(sp.getString("loginAccount", null));
        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String account = accountEditText.getText().toString();
                sp.edit().putString("loginAccount", account).commit();

                if (!NetWorkUtils.isNetwork(getContext())) {
                    Toast.makeText(getContext(), getContext().getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
                    return;
                } else if (!TextUtils.isEmpty(account)){
                    MIMCUser user = UserManager.getInstance().newUser(account);
                    if (user != null) user.login();
                    dismiss();
                }
            }
        });
    }
}

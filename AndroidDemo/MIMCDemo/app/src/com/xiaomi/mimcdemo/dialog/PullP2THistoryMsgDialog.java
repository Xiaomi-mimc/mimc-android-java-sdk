package com.xiaomi.mimcdemo.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.common.NetWorkUtils;
import com.xiaomi.mimcdemo.common.TimeUtils;
import com.xiaomi.mimcdemo.common.UserManager;

import java.util.Calendar;

public class PullP2THistoryMsgDialog extends Dialog {
    private TextView tvBeginDateTime, tvEndDateTime;
    private Button btnBeginDate, btnBeginTime, btnEndDate, btnEndTime;
    private int beginYear, beginMonth, beginDay, beginHour, beginMinute;
    private int endYear, endMonth, endDay, endHour, endMinute;
    private Calendar cal;

    public PullP2THistoryMsgDialog(Context context) {
        super(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_p2t_history_msg_dialog);
        setCancelable(true);
        setTitle(R.string.p2t_history);
        getCurrentDate();
        getCurrentTime();
        final EditText etAccount = (EditText)findViewById(R.id.et_account);
        final EditText etGroupId = (EditText)findViewById(R.id.et_group_id);
        final SharedPreferences sp = getContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        etAccount.setText(sp.getString("p2tAccount", ""));
        etGroupId.setText(sp.getString("p2tGroupId", ""));
        tvBeginDateTime = (TextView)findViewById(R.id.tv_begin_date_time);
        tvEndDateTime = (TextView)findViewById(R.id.tv_end_date_time);
        btnBeginDate = (Button)findViewById(R.id.btn_set_begin_date);
        btnBeginTime = (Button)findViewById(R.id.btn_set_begin_time);
        btnBeginDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(v);
            }
        });
        btnBeginTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTime(v);
            }
        });
        btnEndDate = (Button)findViewById(R.id.btn_set_end_date);
        btnEndTime = (Button)findViewById(R.id.btn_set_end_time);
        btnEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate(v);
            }
        });
        btnEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTime(v);
            }
        });

        findViewById(R.id.btn_query).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String toAccount = etAccount.getText().toString();
                String fromAccount = etGroupId.getText().toString();
                sp.edit().putString("p2tAccount", toAccount).commit();
                sp.edit().putString("p2tGroupId", fromAccount).commit();
                String beginDateTime = tvBeginDateTime.getText().toString();
                String endDateTime = tvEndDateTime.getText().toString();

                if (!NetWorkUtils.isNetwork(getContext())) {
                    Toast.makeText(getContext(), getContext().getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
                    return;
                } else if (UserManager.getInstance().getStatus() != MIMCConstant.OnlineStatus.ONLINE) {
                    Toast.makeText(getContext(), getContext().getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                    return;
                } else if (toAccount.isEmpty()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.input_account_of_owner), Toast.LENGTH_SHORT).show();
                    return;
                } else if (fromAccount.isEmpty()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.input_id_of_group), Toast.LENGTH_SHORT).show();
                    return;
                } else if (beginDateTime.isEmpty()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.input_begin_date_time), Toast.LENGTH_SHORT).show();
                    return;
                } else if (endDateTime.isEmpty()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.input_end_date_time), Toast.LENGTH_SHORT).show();
                    return;
                }
                beginDateTime = Long.toString(TimeUtils.local2UTC(beginYear, beginMonth, beginDay, beginHour, beginMinute));
                endDateTime = Long.toString(TimeUtils.local2UTC(endYear, endMonth, endDay, endHour, endMinute));
                UserManager.getInstance().pullP2THistory(toAccount, fromAccount, beginDateTime, endDateTime);
                dismiss();
            }
        });
    }

    private void getCurrentDate() {
        cal = Calendar.getInstance();
        endYear = beginYear = cal.get(Calendar.YEAR);
        endMonth = beginMonth = cal.get(Calendar.MONTH);
        endDay = beginDay = cal.get(Calendar.DAY_OF_MONTH);
    }

    private void getCurrentTime() {
        cal = Calendar.getInstance();
        endHour = beginHour = cal.get(Calendar.HOUR_OF_DAY);
        endMinute = beginMinute = cal.get(Calendar.MINUTE);
    }

    private void getDate(View v) {
        if (v.getId() == R.id.btn_set_begin_date) {
            new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    beginYear = year;
                    beginMonth = monthOfYear;
                    beginDay = dayOfMonth;
                    tvBeginDateTime.setText(String.format("%d-%d-%d %d:%d", beginYear, beginMonth + 1, beginDay, beginHour, beginMinute));
                }
            }, beginYear, beginMonth, beginDay).show();
        } else if (v.getId() == R.id.btn_set_end_date) {
            new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    endYear = year;
                    endMonth = monthOfYear;
                    endDay = dayOfMonth;
                    tvEndDateTime.setText(String.format("%d-%d-%d %d:%d", endYear, endMonth + 1, endDay, endHour, endMinute));
                }
            }, endYear, endMonth, endDay).show();
        }
    }

    private void getTime(View v) {
        if (v.getId() == R.id.btn_set_begin_time) {
            new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    beginHour = hourOfDay;
                    beginMinute = minute;
                    tvBeginDateTime.setText(String.format("%d-%d-%d %d:%d", beginYear, beginMonth + 1, beginDay, beginHour, beginMinute));
                }
            }, beginHour, beginMinute, true).show();
        } else if (v.getId() == R.id.btn_set_end_time) {
            new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    endHour = hourOfDay;
                    endMinute = minute;
                    tvEndDateTime.setText(String.format("%d-%d-%d %d:%d", endYear, endMonth + 1, endDay, endHour, endMinute));
                }
            }, endHour, endMinute, true).show();
        }
    }
}

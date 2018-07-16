package com.xiaomi.mimcdemo.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by houminjiang on 18-1-19.
 */

public class TimeUtils {

    public static Long local2UTC(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new GregorianCalendar(year, month, day, hour, minute).getTime());

        return calendar.getTimeInMillis();
    }

    public static Long local2UTC(String strDateTime) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DateFormat.getDateInstance().parse(strDateTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return calendar.getTimeInMillis();
    }

    public static String utc2Local(Long utcDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(utcDateTime));
    }
}

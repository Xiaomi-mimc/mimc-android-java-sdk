package com.xiaomi.mimc.javasdk.example;

import java.util.Random;

public class MsgHelper {
    private static String sIDPrefix = randomString(5) + "-";
    private static long id = 0;

    public static synchronized String nextID() {
        return sIDPrefix + Long.toString(id++);
    }

    public static String randomString(int length) {
        if (length < 1) {
            return null;
        }

        String str = "0123456789abcdefghijklmnopqrstuvwxyz" +
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}

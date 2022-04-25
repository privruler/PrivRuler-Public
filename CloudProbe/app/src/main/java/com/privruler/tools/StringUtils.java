package com.privruler.tools;

import android.util.Log;

import com.privruler.MainActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StringUtils {

    public static boolean isEmpty(String s) {
        if (s == null) {
            return true;
        }

        if (s.length() < 1) {
            return true;
        }

        return false;
    }

    public static boolean isBlank(String s) {
        if (isEmpty(s)) {
            return true;
        }

        if (isEmpty(s.trim())) {
            return true;
        }

        return false;
    }

    public static String greatestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    public static void logException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        Log.v(MainActivity.LOG_TAG, sw.toString());
    }

    public static String getRandomAlphaNumericString(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }

        return sb.toString();
    }
}

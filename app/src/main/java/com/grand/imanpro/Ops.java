package com.grand.imanpro;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

public class Ops {

    private static final SQLiteDatabase.CursorFactory MODE_PRIVATE = null;
    public String getMacAddress(){
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }
    public void closeKeyboard(Activity context)
    {
        View view = context.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public String getMonthString(int month)
    {
        String mon="";
        switch (month) {
            case 0: {
                mon = "Jan";
                break;
            }
            case 1: {
                mon = "Feb";
                break;
            }
            case 2: {
                mon = "Mar";
                break;
            }
            case 3: {
                mon = "Apr";
                break;
            }
            case 4: {
                mon = "May";
                break;
            }
            case 5: {
                mon = "Jun";
                break;
            }
            case 6: {
                mon = "Jul";
                break;
            }
            case 7: {
                mon = "Aug";
                break;
            }
            case 8: {
                mon = "Sep";
                break;
            }
            case 9: {
                mon = "Oct";
                break;
            }
            case 10: {
                mon = "Nov";
                break;
            }
            case 11: {
                mon = "Dec";
                break;
            }
        }
        return mon;
    }
}

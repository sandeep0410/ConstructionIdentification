package com.umn.mto.android.workzonealert;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Sandeep on 10/28/2015.
 */
public class Util {
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LogUtils.log("cm value: " + cm);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        LogUtils.log("netinfo value: " + netInfo);
        LogUtils.log("is connecting " + netInfo.isConnectedOrConnecting());
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}

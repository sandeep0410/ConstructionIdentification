package com.umn.mto.android.workzonealert;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by Sandeep on 10/28/2015.
 */
public class Util {
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d("sandeep", "cm value: " + cm);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        Log.d("sandeep","netinfo value: " +netInfo);
        Log.d("sandeep","is connecting " +netInfo.isConnectedOrConnecting());
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}

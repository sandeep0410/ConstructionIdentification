package com.umn.mto.android.workzonealert;

import android.util.Log;

/**
 * Created by Sandeep on 12/24/2015.
 */
public class LogUtils {
    static boolean DEBUG = true;
    static String TAG = "sandeep";

    public static void log(String message){
        if(DEBUG)
            Log.d(TAG,message);
    }
}

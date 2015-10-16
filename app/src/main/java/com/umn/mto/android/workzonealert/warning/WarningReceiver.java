package com.umn.mto.android.workzonealert.warning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Sandeep on 9/29/2015.
 */
public class WarningReceiver extends BroadcastReceiver {

    public static final String RAISE_WARNING = "com.umn.mto.android.warning.RAISE";
    public static final String STOP_WARNING = "com.umn.mto.android.warning.STOP";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(RAISE_WARNING)){
            Intent i = new Intent();
            i.setClassName("com.umn.mto.android.workzonealert", "com.umn.mto.android.workzonealert.warning.WarningActivity");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }else if(action.equals(STOP_WARNING)){
                if(WarningActivity.getInstance()!=null){
                    WarningActivity.getInstance().finish();
                }
        }
    }
}

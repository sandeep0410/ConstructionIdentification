package com.umn.mto.android.workzonealert.imagewarning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by Sandeep on 1/3/2016.
 */
public class ImageWarningReceiver extends BroadcastReceiver{
    public static final String RAISE_IMAGEWARNING = "com.umn.mto.android.imagewarning.RAISE";
    public static final String STOP_IMAGEWARNING = "com.umn.mto.android.imagewarning.STOP";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(RAISE_IMAGEWARNING)){
            Intent i = new Intent();
            i.setClassName("com.umn.mto.android.workzonealert", "com.umn.mto.android.workzonealert.imagewarning.ImageWarningActivity");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.putExtra("drawable",intent.getIntExtra("drawable",0));
            context.startActivity(i);
        }else if(action.equals(STOP_IMAGEWARNING)){
            if(ImageWarningActivity.getInstance()!=null){
                ImageWarningActivity.getInstance().finish();
            }
        }
    }
}

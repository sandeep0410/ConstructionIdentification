package com.umn.mto.android.constructionidentification.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.umn.mto.android.constructionidentification.SpeedDetectionService;

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent serviceLauncher = new Intent(context, SpeedDetectionService.class);
            serviceLauncher.setAction(SpeedDetectionService.NotificationConstants.START_SPEED_DETECTION_SERVICE);
            context.startService(serviceLauncher);
            Log.v("TEST", "Service loaded at start");
        } else {
            Log.d("sandeep", "Entered receiver");
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            PhoneCallStateListener listener = new PhoneCallStateListener(context);
            telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

        }

    }
}